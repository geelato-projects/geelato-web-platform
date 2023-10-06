package org.geelato.web.platform.enums;

import org.apache.logging.log4j.util.Strings;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/27 11:09
 */
public enum PermissionTypeEnum {
    DP("表单权限", "dp"), EP("字段权限", "ep");

    private final String label;//选项内容
    private final String value;//选项值

    PermissionTypeEnum(String label, String value) {
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
            for (PermissionTypeEnum enums : PermissionTypeEnum.values()) {
                if (enums.getValue().equals(value)) {
                    return enums.getLabel();
                }
            }
        }
        return null;
    }
}