package org.geelato.web.platform.enums;

import org.apache.logging.log4j.util.Strings;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/27 11:09
 */
public enum PermissionTypeEnum {
    DATA("数据权限", "dp"),
    ELEMENT("页面元素权限", "ep"),
    MODEL("实体模型权限", "mp"),
    COLUMN("实体字段权限", "cp");

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