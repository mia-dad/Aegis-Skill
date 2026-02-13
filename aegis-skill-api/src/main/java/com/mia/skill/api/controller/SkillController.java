package com.mia.skill.api.controller;


import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.model.io.FieldSpec;
import com.mia.aegis.skill.dsl.model.io.InputSchema;
import com.mia.aegis.skill.dsl.model.io.OutputContract;

import com.mia.aegis.skill.dsl.parser.SkillParser;

import com.mia.aegis.skill.executor.context.SkillResult;
import com.mia.aegis.skill.executor.engine.SkillExecutor;


import com.mia.aegis.skill.executor.store.AwaitRequest;
import com.mia.aegis.skill.executor.store.ExecutionAlreadyCompletedException;
import com.mia.aegis.skill.exception.ExecutionNotFoundException;
import com.mia.aegis.skill.exception.AwaitResumeException;
import com.mia.aegis.skill.exception.InputValidationException;
import com.mia.aegis.skill.llm.LLMAdapterRegistry;
import com.mia.aegis.skill.i18n.Messages;
import com.mia.skill.api.dto.SkillExecuteRequest;
import com.mia.skill.api.dto.SkillExecuteResponse;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 执行 REST API。
 */
@RestController
@RequestMapping("/api/skill")
public class SkillController {

    private final SkillParser skillParser;
    private final SkillExecutor skillExecutor;
    private final LLMAdapterRegistry llmRegistry;
    private final SkillRepository skillRepository;


    @Autowired
    public SkillController(SkillParser skillParser,
                           SkillRepository skillRepository,
                           SkillExecutor skillExecutor,
                           LLMAdapterRegistry llmRegistry) {
        this.skillParser = skillParser;
        this.skillExecutor = skillExecutor;
        this.llmRegistry = llmRegistry;
        this.skillRepository = skillRepository;
    }

    /**
     * 获取所有可用的 Skill 列表。
     *
     * <h4>响应示例</h4>
     * <pre>
     * [
     *   {
     *     "id": "chat",
     *     "description": "通用对话 Skill",
     *     "intents": ["聊天", "问答", "chat", "question"],
     *     "inputSchema": {
     *       "fields": {
     *          "query": {
     *             "type": "string",
     *             "required": true,
     *            "description": null
     *          },
     *          "company": {
     *             "type": "string",
     *             "required": true,
     *             "description": null
     *          },
     *          "period": {
     *             "type": "string",
     *             "required": true,
     *             "description": null
     *           }
     *       },
     *       "fieldNames": [
     *           "query",
     *           "company",
     *           "period"
     *       ],
     *       "empty": false
     *     }
     *   }
     * ]
     * </pre>
     *
     * @return Skill 列表
     */
    @GetMapping("/skills")
    public ResponseEntity<List<com.mia.skill.api.dto.SkillInfo>> skills() {
        List<Skill> skills = skillRepository.findAll();
        List<com.mia.skill.api.dto.SkillInfo> result = new ArrayList<com.mia.skill.api.dto.SkillInfo>();

        for (Skill skill : skills) {
            result.add(new com.mia.skill.api.dto.SkillInfo(
                    skill.getId(),
                    skill.getVersion(),
                    skill.getDescription(),
                    skill.getIntents(),
                    skill.getInputSchema(),
                    buildOutputSchema(skill.getOutputContract())
            ));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{skillId}")
    public ResponseEntity<?> getSkillById(@PathVariable String skillId,
                                          @RequestParam(required = false) String version) {
        if (skillId == null || skillId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(Messages.get("api.error.skillId.required")));
        }

        Skill skill;
        if (version != null && !version.isEmpty()) {
            skill = skillRepository.findById(skillId, version);
        } else {
            skill = skillRepository.findById(skillId);
        }
        if (skill == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(Messages.get("api.error.skill.notFound", skillId)));
        }

        com.mia.skill.api.dto.SkillInfo skillInfo = new com.mia.skill.api.dto.SkillInfo(
                skill.getId(),
                skill.getVersion(),
                skill.getDescription(),
                skill.getIntents(),
                skill.getInputSchema(),
                buildOutputSchema(skill.getOutputContract())
        );
        return ResponseEntity.ok(skillInfo);
    }


    /**
     * 执行 Skill（统一接口，支持 skillId 和 skillMarkdown）。
     *
     * POST /api/skill/execute
     * {
     *   "skillMarkdown": "# skill: my_skill\n...",
     *   "inputs": { "param1": "value1" },
     *   "adapter": "dashscope"  // 可选
     * }
     *
     * 或使用 skillId：
     * {
     *   "skillId": "my_skill",
     *   "inputs": { "param1": "value1" }
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<SkillExecuteResponse> execute(@RequestBody SkillExecuteRequest request) {
        // 验证至少提供 skillId 或 skillMarkdown
        boolean hasSkillId = request.getSkillId() != null && !request.getSkillId().trim().isEmpty();
        boolean hasSkillMarkdown = request.getSkillMarkdown() != null && !request.getSkillMarkdown().trim().isEmpty();
        if (!hasSkillId && !hasSkillMarkdown) {
            return ResponseEntity.badRequest()
                    .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.skillMarkdown.required")));
        }

        // 切换到指定的 Adapter（如果指定）
        String previousDefault = null;
        if (request.getAdapter() != null && !request.getAdapter().isEmpty()) {
            if (!llmRegistry.contains(request.getAdapter())) {
                return ResponseEntity.badRequest()
                        .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.adapter.notFound", request.getAdapter())));
            }
            previousDefault = llmRegistry.getDefault().map(a -> a.getName()).orElse(null);
            llmRegistry.setDefault(request.getAdapter());
        }

        String skillId = null;
        String skillVersion = null;
        try {
            Skill skill;

            // skillId 优先于 skillMarkdown
            if (hasSkillId) {
                String reqVersion = request.getVersion();
                if (reqVersion != null && !reqVersion.isEmpty()) {
                    skill = skillRepository.findById(request.getSkillId(), reqVersion);
                } else {
                    skill = skillRepository.findById(request.getSkillId());
                }
                if (skill == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.skill.notFound", request.getSkillId())));
                }
            } else {
                // 解析 Skill Markdown
                skill = skillParser.parse(request.getSkillMarkdown());
            }
            skillId = skill.getId();
            skillVersion = skill.getVersion();

            // 执行 Skill
            SkillResult result = skillExecutor.execute(skill, request.getInputs());

            if (result.isAwaiting()) {
                // 执行暂停，等待用户输入
                AwaitRequest awaitRequest = (AwaitRequest) result.getOutput();
                return ResponseEntity.ok(
                        SkillExecuteResponse.waitingForInput(
                                skillId,
                                skillVersion,
                                result.getExecutionId(),
                                awaitRequest.getMessage(),
                                convertInputSchemaToMap(awaitRequest.getInputSchema()),
                                result.getTotalDuration()
                        ));
            } else if (result.isSuccess()) {
                return ResponseEntity.ok(
                        SkillExecuteResponse.success(skillId, skillVersion, result.getOutput(), result.getTotalDuration()));
            } else {
                return ResponseEntity.ok(
                        SkillExecuteResponse.error(skillId, skillVersion, result.getError()));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(
                    SkillExecuteResponse.error(skillId, skillVersion, e.getMessage()));

        } finally {
            // 恢复之前的默认 Adapter
            if (previousDefault != null) {
                llmRegistry.setDefault(previousDefault);
            }
        }
    }

    /**
     * 验证 Skill 格式。
     *
     * POST /api/skill/validate
     * {
     *   "skillMarkdown": "# skill: my_skill\n..."
     * }
     */
    @PostMapping("/validate")
    public ResponseEntity<Object> validate(@RequestBody SkillExecuteRequest request) {
        if (request.getSkillMarkdown() == null || request.getSkillMarkdown().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ValidationResult(false, Messages.get("api.error.skillMarkdown.required"), null));
        }

        try {
            Skill skill = skillParser.parse(request.getSkillMarkdown());
            SkillInfo info = new SkillInfo(
                    skill.getId(),
                    skill.getVersion(),
                    skill.getDescription(),
                    skill.getInputSchema() != null ? skill.getInputSchema().getFields().size() : 0,
                    skill.getSteps().size()
            );
            return ResponseEntity.ok(new ValidationResult(true, null, info));
        } catch (Exception e) {
            return ResponseEntity.ok(new ValidationResult(false, e.getMessage(), null));
        }
    }

    // Inner classes for validation response
    static class ValidationResult {
        public boolean valid;
        public String error;
        public SkillInfo skill;

        ValidationResult(boolean valid, String error, SkillInfo skill) {
            this.valid = valid;
            this.error = error;
            this.skill = skill;
        }
    }

    static class SkillInfo {
        public String id;
        public String version;
        public String description;
        public int inputCount;
        public int stepCount;

        SkillInfo(String id, String version, String description, int inputCount, int stepCount) {
            this.id = id;
            this.version = version;
            this.description = description;
            this.inputCount = inputCount;
            this.stepCount = stepCount;
        }
    }
    /**
     * 错误响应。
     */
    static class ErrorResponse {
        public String error;

        ErrorResponse(String error) {
            this.error = error;
        }
    }



    /**
     * 恢复暂停的 Skill 执行。
     *
     * <p>POST /api/skill/resume</p>
     *
     * <p>支持两种方式指定 Skill：</p>
     * <ol>
     *   <li>skillId（推荐）- 从 SkillRepository 加载</li>
     *   <li>skillMarkdown - 解析 Markdown 定义</li>
     * </ol>
     * <p>若同时提供，skillId 优先。</p>
     *
     * <p>请求示例（使用 skillId）：</p>
     * <pre>{@code
     * {
     *   "executionId": "exec-550e8400-e29b-41d4-a716-446655440000",
     *   "skillId": "financial_analysis",
     *   "inputs": {
     *     "confirmed": true
     *   }
     * }
     * }</pre>
     *
     * <p>请求示例（使用 skillMarkdown）：</p>
     * <pre>{@code
     * {
     *   "executionId": "exec-550e8400-e29b-41d4-a716-446655440000",
     *   "skillMarkdown": "# skill: my_skill\n...",
     *   "inputs": {
     *     "approval": true,
     *     "comment": "Looks good!"
     *   },
     *   "adapter": "dashscope"  // 可选
     * }
     * }</pre>
     *
     * <p>HTTP 状态码：</p>
     * <ul>
     *   <li>200 OK - 恢复成功或再次暂停</li>
     *   <li>400 Bad Request - 请求参数错误或输入验证失败</li>
     *   <li>404 Not Found - 执行不存在或 Skill 不存在</li>
     *   <li>409 Conflict - 执行已完成（已恢复/过期/取消）</li>
     * </ul>
     */
    @PostMapping("/resume")
    public ResponseEntity<SkillExecuteResponse> resume(@RequestBody SkillExecuteRequest request) {
        // 验证必填参数
        if (request.getExecutionId() == null || request.getExecutionId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.executionId.required")));
        }

        // 验证至少提供 skillId 或 skillMarkdown
        boolean hasSkillId = request.getSkillId() != null && !request.getSkillId().trim().isEmpty();
        boolean hasSkillMarkdown = request.getSkillMarkdown() != null && !request.getSkillMarkdown().trim().isEmpty();
        if (!hasSkillId && !hasSkillMarkdown) {
            return ResponseEntity.badRequest()
                    .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.skill.required")));
        }

        // 切换到指定的 Adapter（如果指定）
        String previousDefault = null;
        if (request.getAdapter() != null && !request.getAdapter().isEmpty()) {
            if (!llmRegistry.contains(request.getAdapter())) {
                return ResponseEntity.badRequest()
                        .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.adapter.notFound", request.getAdapter())));
            }
            previousDefault = llmRegistry.getDefault().map(a -> a.getName()).orElse(null);
            llmRegistry.setDefault(request.getAdapter());
        }

        String skillId = null;
        String skillVersion = null;
        try {
            Skill skill;

            // skillId 优先于 skillMarkdown
            if (hasSkillId) {
                String reqVersion = request.getVersion();
                if (reqVersion != null && !reqVersion.isEmpty()) {
                    skill = skillRepository.findById(request.getSkillId(), reqVersion);
                } else {
                    skill = skillRepository.findById(request.getSkillId());
                }
                if (skill == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(SkillExecuteResponse.error(null, null, Messages.get("api.error.skill.notFound", request.getSkillId())));
                }
            } else {
                // 解析 Skill Markdown
                skill = skillParser.parse(request.getSkillMarkdown());
            }
            skillId = skill.getId();
            skillVersion = skill.getVersion();

            // 恢复执行
            SkillResult result = skillExecutor.resume(skill, request.getExecutionId(), request.getInputs());

            if (result.isAwaiting()) {
                // 再次暂停（遇到另一个 await step）
                AwaitRequest awaitRequest = (AwaitRequest) result.getOutput();
                return ResponseEntity.ok(
                        SkillExecuteResponse.waitingForInput(
                                skillId,
                                skillVersion,
                                result.getExecutionId(),
                                awaitRequest.getMessage(),
                                convertInputSchemaToMap(awaitRequest.getInputSchema()),
                                result.getTotalDuration()
                        ));
            } else if (result.isSuccess()) {
                return ResponseEntity.ok(
                        SkillExecuteResponse.success(skillId, skillVersion, result.getOutput(), result.getTotalDuration()));
            } else {
                return ResponseEntity.ok(
                        SkillExecuteResponse.error(skillId, skillVersion, result.getError()));
            }

        } catch (ExecutionNotFoundException e) {
            // 404 Not Found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SkillExecuteResponse.error(skillId, skillVersion, e.getMessage()));

        } catch (ExecutionAlreadyCompletedException e) {
            // 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(SkillExecuteResponse.error(skillId, skillVersion, e.getMessage()));

        } catch (InputValidationException e) {
            // 400 Bad Request
            return ResponseEntity.badRequest()
                    .body(SkillExecuteResponse.error(skillId, skillVersion, e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.ok(
                    SkillExecuteResponse.error(skillId, skillVersion, e.getMessage()));

        } finally {
            // 恢复之前的默认 Adapter
            if (previousDefault != null) {
                llmRegistry.setDefault(previousDefault);
            }
        }
    }

    /**
     * 将 InputSchema 转换为 Map 格式（用于 JSON 序列化）。
     *
     * @param schema InputSchema
     * @return Map 格式的 Schema
     */
    private Map<String, Object> convertInputSchemaToMap(InputSchema schema) {
        if (schema == null || schema.isEmpty()) {
            return new HashMap<String, Object>();
        }

        Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, FieldSpec> entry : schema.getFields().entrySet()) {
            String fieldName = entry.getKey();
            FieldSpec spec = entry.getValue();

            Map<String, Object> fieldInfo = new HashMap<String, Object>();
            fieldInfo.put("type", spec.getType());
            fieldInfo.put("required", spec.isRequired());

            // 描述字段
            if (spec.getDescription() != null) {
                fieldInfo.put("description", spec.getDescription());
            }

            result.put(fieldName, fieldInfo);
        }
        return result;
    }

    /**
     * 将 OutputContract 转换为 Map 格式，用于 SkillInfo 元数据返回。
     *
     * <p>直接返回平铺的字段定义（保留完整嵌套结构）。</p>
     *
     * <p>返回格式示例：</p>
     * <pre>{@code
     * {
     *   "results": {
     *     "type": "array",
     *     "description": "搜索结果列表",
     *     "items": { ... }
     *   }
     * }
     * }</pre>
     *
     * @param contract OutputContract
     * @return Map 格式的 outputSchema，如果 contract 为 null 则返回 null
     */
    private Map<String, Object> buildOutputSchema(OutputContract contract) {
        if (contract == null) {
            return null;
        }

        // 优先使用 rawProperties（保留完整嵌套结构），回退到 FieldSpec 重建
        Map<String, Object> rawProperties = contract.getRawProperties();
        if (rawProperties != null && !rawProperties.isEmpty()) {
            return rawProperties;
        }

        if (!contract.getFields().isEmpty()) {
            Map<String, Object> result = new HashMap<String, Object>();
            for (Map.Entry<String, FieldSpec> entry : contract.getFields().entrySet()) {
                FieldSpec spec = entry.getValue();
                Map<String, Object> fieldInfo = new HashMap<String, Object>();
                fieldInfo.put("type", spec.getType());
                if (spec.getDescription() != null) {
                    fieldInfo.put("description", spec.getDescription());
                }
                result.put(entry.getKey(), fieldInfo);
            }
            return result;
        }

        return null;
    }
}