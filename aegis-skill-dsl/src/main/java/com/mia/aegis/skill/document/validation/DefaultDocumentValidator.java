package com.mia.aegis.skill.document.validation;

import com.mia.aegis.skill.document.model.*;
import com.mia.aegis.skill.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认 Document 验证器实现。
 *
 * <p>验证 Document 结构的完整性，包括：</p>
 * <ul>
 *   <li>Document 不可为 null</li>
 *   <li>必需字段存在（type, version, blocks）</li>
 *   <li>ChartBlock 必须包含 chart 对象</li>
 *   <li>未知 Block 类型产生警告</li>
 * </ul>
 *
 * @since 0.3.0
 */
public class DefaultDocumentValidator implements DocumentValidator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDocumentValidator.class);

    @Override
    public List<ValidationResult> validate(Document document) {
        if (logger.isDebugEnabled()) {
            logger.debug(Messages.get("document.validation.started"));
        }

        List<ValidationResult> results = new ArrayList<>();

        // Null document check
        if (document == null) {
            String message = Messages.get("document.null");
            results.add(ValidationResult.error("document", message));
            logger.error(message);
            return results;
        }

        // Validate document type
        if (!Document.TYPE.equals(document.getType())) {
            String message = Messages.get("document.invalid.type", Document.TYPE, document.getType());
            results.add(ValidationResult.error("type", message));
            if (logger.isWarnEnabled()) {
                logger.warn(message);
            }
        }

        // Validate document version
        if (!Document.VERSION.equals(document.getVersion())) {
            String message = Messages.get("document.invalid.version", Document.VERSION, document.getVersion());
            results.add(ValidationResult.error("version", message));
            if (logger.isWarnEnabled()) {
                logger.warn(message);
            }
        }

        // Validate blocks
        List<Block> blocks = document.getBlocks();
        if (blocks == null) {
            String message = Messages.get("document.blocks.null");
            results.add(ValidationResult.error("blocks", message));
            logger.error(message);
            return results;
        }

        // Validate each block
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            String blockPath = "blocks[" + i + "]";

            if (block == null) {
                String message = Messages.get("block.null");
                results.add(ValidationResult.error(blockPath, message));
                logger.error("{}: {}", blockPath, message);
                continue;
            }

            // Validate block based on type
            validateBlock(block, blockPath, results);
        }

        // Log validation result
        long errorCount = results.stream().filter(r -> r.getLevel() == ValidationLevel.ERROR).count();
        long warningCount = results.stream().filter(r -> r.getLevel() == ValidationLevel.WARNING).count();
        if (logger.isInfoEnabled()) {
            logger.info(Messages.get("document.validation.completed", errorCount, warningCount));
        }

        if (errorCount > 0) {
            logger.error(Messages.get("document.validation.failed"));
            results.stream()
                .filter(r -> r.getLevel() == ValidationLevel.ERROR)
                .forEach(r -> logger.error("  - {}", r));
        }

        return results;
    }

    /**
     * 验证单个 Block。
     *
     * @param block   要验证的块
     * @param path    块的路径
     * @param results 验证结果列表
     */
    private void validateBlock(Block block, String path, List<ValidationResult> results) {
        String type = block.getType();

        if (type == null) {
            String message = Messages.get("block.type.null");
            results.add(ValidationResult.error(path + ".type", message));
            logger.error("{}: {}", path, message);
            return;
        }

        switch (type) {
            case ParagraphBlock.TYPE:
                validateParagraphBlock(block, path, results);
                break;
            case ChartBlock.TYPE:
                validateChartBlock(block, path, results);
                break;
            default:
                // Unknown block type - warning level
                String message = Messages.get("block.type.unknown", type);
                results.add(ValidationResult.warning(path + ".type", message));
                if (logger.isWarnEnabled()) {
                    logger.warn("{}: {}", path, message);
                }
                break;
        }
    }

    /**
     * 验证 ParagraphBlock。
     */
    private void validateParagraphBlock(Block block, String path, List<ValidationResult> results) {
        if (!(block instanceof ParagraphBlock)) {
            String message = Messages.get("block.instance.mismatch", ParagraphBlock.TYPE, ParagraphBlock.class.getSimpleName());
            results.add(ValidationResult.error(path, message));
            logger.error("{}: {}", path, message);
            return;
        }

        ParagraphBlock paragraphBlock = (ParagraphBlock) block;
        if (paragraphBlock.getText() == null) {
            String message = Messages.get("block.paragraph.text.null");
            results.add(ValidationResult.error(path + ".text", message));
            logger.error("{}: {}", path, message);
        }
    }

    /**
     * 验证 ChartBlock。
     */
    private void validateChartBlock(Block block, String path, List<ValidationResult> results) {
        if (!(block instanceof ChartBlock)) {
            String message = Messages.get("block.instance.mismatch", ChartBlock.TYPE, ChartBlock.class.getSimpleName());
            results.add(ValidationResult.error(path, message));
            logger.error("{}: {}", path, message);
            return;
        }

        ChartBlock chartBlock = (ChartBlock) block;
        ChartSpec chart = chartBlock.getChart();

        if (chart == null) {
            String message = Messages.get("block.chart.missing");
            results.add(ValidationResult.error(path + ".chart", message));
            logger.error("{}: {}", path, message);
            return;
        }

        // Validate ChartSpec fields
        validateChartSpec(chart, path + ".chart", results);
    }

    /**
     * 验证 ChartSpec。
     */
    private void validateChartSpec(ChartSpec chart, String path, List<ValidationResult> results) {
        // Type validation
        String chartType = chart.getType();
        if (chartType == null) {
            String message = Messages.get("chart.type.null");
            results.add(ValidationResult.error(path + ".type", message));
            logger.error("{}: {}", path, message);
        } else if (!ChartSpec.TYPE_BAR.equals(chartType) && !ChartSpec.TYPE_LINE.equals(chartType)) {
            String message = Messages.get("chart.type.unknown", chartType);
            results.add(ValidationResult.warning(path + ".type", message));
            if (logger.isWarnEnabled()) {
                logger.warn("{}: {}", path, message);
            }
        }

        // Title validation
        if (chart.getTitle() == null) {
            String message = Messages.get("chart.title.null");
            results.add(ValidationResult.error(path + ".title", message));
            logger.error("{}: {}", path, message);
        }

        // X-axis validation
        if (chart.getX() == null) {
            String message = Messages.get("chart.x.null");
            results.add(ValidationResult.error(path + ".x", message));
            logger.error("{}: {}", path, message);
        }

        // Series validation
        List<Series> series = chart.getSeries();
        if (series == null) {
            String message = Messages.get("chart.series.null");
            results.add(ValidationResult.error(path + ".series", message));
            logger.error("{}: {}", path, message);
        } else if (series.isEmpty()) {
            String message = Messages.get("chart.series.empty");
            results.add(ValidationResult.error(path + ".series", message));
            logger.error("{}: {}", path, message);
        } else {
            for (int i = 0; i < series.size(); i++) {
                validateSeries(series.get(i), path + ".series[" + i + "]", results);
            }
        }
    }

    /**
     * 验证 Series。
     */
    private void validateSeries(Series series, String path, List<ValidationResult> results) {
        if (series == null) {
            String message = Messages.get("series.null");
            results.add(ValidationResult.error(path, message));
            logger.error("{}: {}", path, message);
            return;
        }

        if (series.getName() == null || series.getName().isEmpty()) {
            String message = Messages.get("series.name.empty");
            results.add(ValidationResult.error(path + ".name", message));
            logger.error("{}: {}", path, message);
        } else if (series.getName() == null) {
            String message = Messages.get("series.name.null");
            results.add(ValidationResult.error(path + ".name", message));
            logger.error("{}: {}", path, message);
        }

        if (series.getData() == null) {
            String message = Messages.get("series.data.null");
            results.add(ValidationResult.error(path + ".data", message));
            logger.error("{}: {}", path, message);
        }
    }
}
