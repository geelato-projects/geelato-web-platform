package org.geelato.web.platform.enums;

import org.apache.logging.log4j.util.Strings;

/**
 * @author diabl
 * @description: TODO
 * @date 2024/4/15 15:40
 */
public enum OrgTypeEnum {
    DEPT("部门", "department"), COMPANY("公司", "company");

    private final String label;// 选项内容
    private final String value;// 选项值

    OrgTypeEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public static String getLabel(String value) {
        if (Strings.isNotBlank(value)) {
            for (OrgTypeEnum enums : OrgTypeEnum.values()) {
                if (enums.getValue().equals(value)) {
                    return enums.getLabel();
                }
            }
        }
        return null;
    }
}