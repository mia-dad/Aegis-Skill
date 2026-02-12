package com.mia.skill.api.controller;

import com.mia.aegis.skill.tools.ToolProvider;
import com.mia.aegis.skill.tools.ToolRegistry;
import com.mia.skill.api.dto.ToolInfo;
import com.mia.skill.api.dto.ToolInfoConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Tool 元信息 REST API。
 *
 * <p>暴露工具的元数据给前端和 AI 使用，支持工具列表查询、类别过滤和详情查看。</p>
 */
@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;

    @Autowired
    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 列出所有工具。
     *
     * <p>支持 category 参数过滤：GET /api/tools?category=data_access</p>
     *
     * @param category 工具类别（可选）
     * @return 工具列表
     */
    @GetMapping
    public ResponseEntity<List<ToolInfo>> listTools(
            @RequestParam(value = "category", required = false) String category) {

        List<ToolProvider> providers = toolRegistry.listProviders();
        List<ToolInfo> result = new ArrayList<ToolInfo>();

        for (ToolProvider provider : providers) {
            ToolInfo info = ToolInfoConverter.convert(provider);
            if (info == null) {
                continue;
            }
            if (category != null && !category.trim().isEmpty()) {
                if (!category.equals(info.getCategory())) {
                    continue;
                }
            }
            result.add(info);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 列出所有工具类别（去重排序）。
     *
     * @return 类别列表
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> listCategories() {
        List<ToolProvider> providers = toolRegistry.listProviders();
        Set<String> categories = new TreeSet<String>();

        for (ToolProvider provider : providers) {
            String category = provider.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                categories.add(category);
            }
        }

        return ResponseEntity.ok(new ArrayList<String>(categories));
    }

    /**
     * 获取指定工具的详细信息。
     *
     * @param name 工具名称
     * @return 工具详情，404 时返回 error JSON
     */
    @GetMapping("/{name}")
    public ResponseEntity<?> getToolByName(@PathVariable String name) {
        Optional<ToolProvider> providerOpt = toolRegistry.find(name);

        if (!providerOpt.isPresent()) {
            Map<String, String> error = new LinkedHashMap<String, String>();
            error.put("error", "Tool not found: " + name);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        ToolInfo info = ToolInfoConverter.convert(providerOpt.get());
        return ResponseEntity.ok(info);
    }
}
