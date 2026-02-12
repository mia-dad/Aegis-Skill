/**
 * Document 验证器。
 *
 * <p>提供文档结构完整性验证：</p>
 * <ul>
 *   <li>{@link com.mia.aegis.skill.document.validation.DocumentValidator} - 验证器接口</li>
 *   <li>{@link com.mia.aegis.skill.document.validation.DefaultDocumentValidator} - 默认实现</li>
 *   <li>{@link com.mia.aegis.skill.document.validation.ValidationResult} - 验证结果</li>
 *   <li>{@link com.mia.aegis.skill.document.validation.ValidationLevel} - 错误级别枚举</li>
 * </ul>
 *
 * <h3>验证规则</h3>
 * <ul>
 *   <li>Document 不可为 null，type 必须为 "document"，version 必须为 "v1"</li>
 *   <li>Blocks 列表不可为 null</li>
 *   <li>ChartBlock 必须包含有效的 ChartSpec</li>
 *   <li>未知 Block 类型产生 WARNING</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * DocumentValidator validator = new DefaultDocumentValidator();
 * List<ValidationResult> results = validator.validate(document);
 *
 * // 检查是否有 ERROR 级别错误
 * if (validator.isValid(document)) {
 *     // 文档有效
 * } else {
 *     // 处理错误
 *     results.stream()
 *         .filter(r -> r.getLevel() == ValidationLevel.ERROR)
 *         .forEach(r -> System.err.println(r));
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
package com.mia.aegis.skill.document.validation;
