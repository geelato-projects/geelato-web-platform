package org.geelato.web.platform.enums;

import org.geelato.utils.StringUtils;

/**
 * @author diabl
 */
public enum OrgTypeEnum {
    ROOT("根组织", "root"),
    COMPANY("公司", "company"),
    DEPT("部门", "department"),
    FACTORY("工厂", "factory"),
    OFFICE("办事处", "office");

    private final String label;// 选项内容
    private final String value;// 选项值

    OrgTypeEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public static String getLabel(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (OrgTypeEnum enums : OrgTypeEnum.values()) {
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