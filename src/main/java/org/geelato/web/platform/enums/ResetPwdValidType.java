package org.geelato.web.platform.enums;

import org.apache.logging.log4j.util.Strings;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/13 15:04
 */
public enum ResetPwdValidType {
    MOBILE("mobilePhone", "1"), MAIL("email", "2");

    private final String label;//选项内容
    private final String value;//选项值

    ResetPwdValidType(String label, String value) {
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
            for (ResetPwdValidType validType : ResetPwdValidType.values()) {
                if (validType.getValue().equals(value)) {
                    return validType.getLabel();
                }
            }
        }
        return null;
    }
}