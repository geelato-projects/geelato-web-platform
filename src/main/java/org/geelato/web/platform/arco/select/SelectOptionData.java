package org.geelato.web.platform.arco.select;

import java.io.Serializable;

/**
 * @author diabl
 * @description: Arco Design select
 */
public class SelectOptionData<E> implements Serializable {
    private Boolean disabled = false;// 是否禁用
    private String value;// 选项值
    private String label;// 选项内容
    private E data;// 存放可能存在的数据

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public E getData() {
        return data;
    }

    public void setData(E data) {
        this.data = data;
    }
}
