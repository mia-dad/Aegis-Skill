package com.mia.skill.api.controller;

import com.mia.aegis.skill.dsl.model.Skill;
import com.mia.aegis.skill.dsl.validator.ComprehensiveSkillValidator;
import com.mia.aegis.skill.dsl.validator.report.SkillValidationReport;
import com.mia.skill.api.dto.ValidateSkillRequest;
import com.mia.aegis.skill.persistence.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合 Skill 校验 REST API。
 */
@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    private final ComprehensiveSkillValidator validator;
    private final SkillRepository skillRepository;

    @Autowired
    public ValidationController(ComprehensiveSkillValidator validator, SkillRepository skillRepository) {
        this.validator = validator;
        this.skillRepository = skillRepository;
    }

    /**
     * 校验 Skill Markdown。
     *
     * <p>POST /api/validate/skill</p>
     */
    @PostMapping("/skill")
    public ResponseEntity<SkillValidationReport> validateSkill(@RequestBody ValidateSkillRequest request) {
        SkillValidationReport report = validator.validateMarkdown(request.getMarkdown());
        return ResponseEntity.ok(report);
    }

    /**
     * 校验所有已加载的 Skill。
     *
     * <p>GET /api/validate/skills</p>
     *
     * @return 每个 Skill 的校验报告列表
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillValidationReport>> validateAllSkills() {
        List<Skill> skills = skillRepository.findAll();
        List<SkillValidationReport> reports = new ArrayList<SkillValidationReport>();
        for (Skill skill : skills) {
            reports.add(validator.validate(skill));
        }
        return ResponseEntity.ok(reports);
    }

    /**
     * 校验指定 Skill。
     *
     * <p>GET /api/validate/skill/{skillId}</p>
     *
     * @param skillId Skill ID
     * @param version 版本号（可选）
     * @return 校验报告
     */
    @GetMapping("/skill/{skillId}")
    public ResponseEntity<SkillValidationReport> validateSkillById(
            @PathVariable String skillId,
            @RequestParam(required = false) String version) {

        Skill skill;
        if (version != null && !version.isEmpty()) {
            skill = skillRepository.findById(skillId, version);
        } else {
            skill = skillRepository.findById(skillId);
        }

        if (skill == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        SkillValidationReport report = validator.validate(skill);
        return ResponseEntity.ok(report);
    }
}
