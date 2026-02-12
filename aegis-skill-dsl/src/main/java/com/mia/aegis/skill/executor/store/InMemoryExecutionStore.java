package com.mia.aegis.skill.executor.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 内存实现的执行上下文快照存储。
 *
 * <p>使用 ConcurrentHashMap 实现线程安全的快照存储。</p>
 */
public class InMemoryExecutionStore implements ExecutionStore {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryExecutionStore.class);

    private final ConcurrentMap<String, ExecutionContextSnapshot> store;
    private final ConcurrentMap<String, ReentrantLock> locks;

    /**
     * 创建 InMemoryExecutionStore 实例。
     */
    public InMemoryExecutionStore() {
        this.store = new ConcurrentHashMap<String, ExecutionContextSnapshot>();
        this.locks = new ConcurrentHashMap<String, ReentrantLock>();
        
        logger.debug("创建 InMemoryExecutionStore");
    }

    @Override
    public void save(ExecutionContextSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }
        if (snapshot.getExecutionId() == null || snapshot.getExecutionId().trim().isEmpty()) {
            throw new IllegalArgumentException("executionId cannot be null or empty");
        }
        store.put(snapshot.getExecutionId(), snapshot);

        logger.info("保存快照 - executionId: {}, skill: {}, stepIndex: {}, status: {}",
            snapshot.getExecutionId(), snapshot.getSkillName(),
            snapshot.getCurrentStepIndex(), snapshot.getStatus());
    }

    @Override
    public ExecutionContextSnapshot findById(String executionId) {
        if (executionId == null || executionId.trim().isEmpty()) {
            return null;
        }
        ExecutionContextSnapshot snapshot = store.get(executionId);

        if (snapshot != null) {
            logger.debug("找到快照 - executionId: {}, status: {}",
                executionId, snapshot.getStatus());
        } else {
            logger.warn("快照不存在 - executionId: {}", executionId);
        }

        return snapshot;
    }

    @Override
    public ExecutionContextSnapshot removeById(String executionId) {
        if (executionId == null || executionId.trim().isEmpty()) {
            return null;
        }
        locks.remove(executionId);
        ExecutionContextSnapshot removed = store.remove(executionId);
        
        if (removed != null) {
            logger.info("删除快照 - executionId: {}, status: {}",
                executionId, removed.getStatus());
        }
        
        return removed;
    }

    @Override
    public List<ExecutionContextSnapshot> findExpired(long beforeTimestamp) {
        List<ExecutionContextSnapshot> expired = new ArrayList<ExecutionContextSnapshot>();
        for (ExecutionContextSnapshot snapshot : store.values()) {
            if (snapshot.getCreatedAt() < beforeTimestamp &&
                snapshot.getStatus() == SnapshotStatus.ACTIVE) {
                expired.add(snapshot);
            }
        }

        if (!expired.isEmpty()) {
            logger.info("找到过期快照 - count: {}", expired.size());
        }

        return expired;
    }

    @Override
    public boolean updateStatus(String executionId, SnapshotStatus newStatus) {
        if (executionId == null || newStatus == null) {
            return false;
        }

        ReentrantLock lock = getLockFor(executionId);
        lock.lock();
        try {
            ExecutionContextSnapshot snapshot = store.get(executionId);
            if (snapshot != null) {
                SnapshotStatus oldStatus = snapshot.getStatus();
                snapshot.setStatus(newStatus);

                logger.info("更新快照状态 - executionId: {}, status: {} -> {}",
                    executionId, oldStatus, newStatus);

                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exists(String executionId) {
        if (executionId == null || executionId.trim().isEmpty()) {
            return false;
        }
        return store.containsKey(executionId);
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public void clear() {
        int size = store.size();
        store.clear();
        locks.clear();
        
        logger.debug("清空所有快照 - count: {}", size);
    }

    /**
     * 原子性地更新状态（仅当当前状态匹配时）。
     */
    public boolean compareAndUpdateStatus(String executionId,
                                           SnapshotStatus expectedStatus,
                                           SnapshotStatus newStatus) {
        if (executionId == null || expectedStatus == null || newStatus == null) {
            return false;
        }

        ReentrantLock lock = getLockFor(executionId);
        lock.lock();
        try {
            ExecutionContextSnapshot snapshot = store.get(executionId);
            if (snapshot != null && snapshot.getStatus() == expectedStatus) {
                SnapshotStatus oldStatus = snapshot.getStatus();
                snapshot.setStatus(newStatus);

                logger.debug("CAS 更新快照状态 - executionId: {}, status: {} -> {}",
                    executionId, oldStatus, newStatus);

                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock getLockFor(String executionId) {
        locks.putIfAbsent(executionId, new ReentrantLock());
        return locks.get(executionId);
    }
}
