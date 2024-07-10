package org.geelato.web.platform.enums;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.geelato.utils.StringUtils;

/**
 * @author diabl
 */
public enum ExcelAlignmentEnum {
    LEFT(HorizontalAlignment.LEFT, "left"),
    CENTER(HorizontalAlignment.CENTER, "center"),
    RIGHT(HorizontalAlignment.RIGHT, "right");

    private final HorizontalAlignment label;// 选项内容
    private final String value;// 选项值

    ExcelAlignmentEnum(HorizontalAlignment label, String value) {
        this.label = label;
        this.value = value;
    }

    public static HorizontalAlignment getLabel(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (ExcelAlignmentEnum enums : ExcelAlignmentEnum.values()) {
                if (enums.getValue().equalsIgnoreCase(value)) {
                    return enums.getLabel();
                }
            }
        }
        return ExcelAlignmentEnum.CENTER.getLabel();
    }

    public HorizontalAlignment getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}