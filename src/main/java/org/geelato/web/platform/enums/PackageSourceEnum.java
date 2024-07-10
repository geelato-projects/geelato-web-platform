package org.geelato.web.platform.enums;

import org.geelato.utils.StringUtils;

/**
 * @author diabl
 * @description: 应用版本来源
 */
public enum PackageSourceEnum {
    PACKET("当前环境打包", "packet"),
    UPLOAD("版本包上传", "upload"),
    SYNC("版本仓库下载", "sync");

    private final String label;// 选项内容
    private final String value;// 选项值

    PackageSourceEnum(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public static String getLabel(String value) {
        if (StringUtils.isNotBlank(value)) {
            for (PackageSourceEnum enums : PackageSourceEnum.values()) {
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