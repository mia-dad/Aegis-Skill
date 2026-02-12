package com.mia.skill.api.controller;

import com.mia.aegis.skill.exception.LLMInvocationException;
import com.mia.aegis.skill.llm.LLMAdapter;
import com.mia.aegis.skill.llm.LLMAdapterRegistry;
import com.mia.aegis.skill.i18n.Messages;
import com.mia.skill.api.dto.AdapterInfo;
import com.mia.skill.api.dto.LLMInvokeRequest;
import com.mia.skill.api.dto.LLMInvokeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * LLM 调用 REST API。
 */
@RestController
@RequestMapping("/api/llm")
public class LLMController {

    private final LLMAdapterRegistry registry;

    @Autowired
    public LLMController(LLMAdapterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 列出所有可用的 LLM Adapter。
     *
     * GET /api/llm/adapters
     */
    @GetMapping("/adapters")
    public ResponseEntity<List<AdapterInfo>> listAdapters() {
        List<AdapterInfo> adapters = new ArrayList<>();
        Optional<LLMAdapter> defaultAdapter = registry.getDefault();
        String defaultName = defaultAdapter.map(LLMAdapter::getName).orElse(null);

        for (String name : registry.listAdapters()) {
            Optional<LLMAdapter> adapterOpt = registry.find(name);
            if (adapterOpt.isPresent()) {
                LLMAdapter adapter = adapterOpt.get();
                adapters.add(new AdapterInfo(
                        adapter.getName(),
                        adapter.getSupportedModels(),
                        adapter.isAvailable(),
                        adapter.getName().equals(defaultName)
                ));
            }
        }

        return ResponseEntity.ok(adapters);
    }

    /**
     * 设置默认 Adapter。
     *
     * POST /api/llm/adapters/default?name=xxx
     */
    @PostMapping("/adapters/default")
    public ResponseEntity<String> setDefaultAdapter(@RequestParam String name) {
        if (!registry.contains(name)) {
            return ResponseEntity.badRequest().body(Messages.get("api.llm.error.adapter.notFound", name));
        }
        registry.setDefault(name);
        return ResponseEntity.ok("Default adapter set to: " + name);
    }

    /**
     * 调用 LLM。
     *
     * POST /api/llm/invoke
     * {
     *   "prompt": "你好",
     *   "adapter": "dashscope",  // 可选
     *   "options": { "temperature": 0.7 }  // 可选
     * }
     */
    @PostMapping("/invoke")
    public ResponseEntity<LLMInvokeResponse> invoke(@RequestBody LLMInvokeRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(LLMInvokeResponse.error(Messages.get("api.llm.error.prompt.required"), null));
        }

        // 获取指定的或默认的 Adapter
        LLMAdapter adapter;
        String adapterName = request.getAdapter();
        if (adapterName != null && !adapterName.isEmpty()) {
            Optional<LLMAdapter> found = registry.find(adapterName);
            if (!found.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(LLMInvokeResponse.error(Messages.get("api.llm.error.adapter.notFound", adapterName), null));
            }
            adapter = found.get();
        } else {
            Optional<LLMAdapter> defaultAdapter = registry.getDefault();
            if (!defaultAdapter.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(LLMInvokeResponse.error(Messages.get("api.llm.error.noDefault"), null));
            }
            adapter = defaultAdapter.get();
        }

        // 检查可用性
        if (!adapter.isAvailable()) {
            return ResponseEntity.ok(
                    LLMInvokeResponse.error(Messages.get("api.llm.error.notAvailable"), adapter.getName()));
        }

        // 调用 LLM
        long startTime = System.currentTimeMillis();
        try {
            String response = adapter.invoke(request.getPrompt(), request.getOptions());
            long duration = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(
                    LLMInvokeResponse.success(response, adapter.getName(), duration));
        } catch (LLMInvocationException e) {
            return ResponseEntity.ok(
                    LLMInvokeResponse.error(e.getMessage(), adapter.getName()));
        }
    }
}
