package org.geelato.web.platform.enums;

import org.geelato.utils.StringUtils;

/**
 * @author diabl
 */
public enum ValidTypeEnum {
    MOBILE("mobilePhone", "1"),
    MAIL("email", "2"),
    PASSWORD("'password'", "3");

    private final String label;// 选项内容
    private final String value;// 选项值

    ValidTypeEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public static String getLabel(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (ValidTypeEnum validType : ValidTypeEnum.values()) {
                if (validType.getValue().equals(value)) {
                    return validType.getLabel();
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