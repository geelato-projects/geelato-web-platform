package org.geelato.web.platform.enums;

import org.geelato.utils.StringUtils;

/**
 * @author diabl
 */
public enum RoleTypeEnum {
    PLATFORM("平台级角色", "platform"), APP("应用级角色", "app");

    private final String label;// 选项内容
    private final String value;// 选项值

    RoleTypeEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public static String getLabel(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (RoleTypeEnum enums : RoleTypeEnum.values()) {
                if (enums.getValue().equals(value)) {
                    return enums.getLabel();
                }
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }
}