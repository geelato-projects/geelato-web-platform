package org.geelato.web.platform.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * Created by hongxueqian on 14-5-2.
 */
@Entity(name = "sys_common_general_config")
@Title(title = "平台设置")
public class CommonGeneralConfig extends BaseEntity {
    private String name;
    private String code;
    private int seq;
    private String value;
    private String description;

    public CommonGeneralConfig() {
    }

    @Col(name = "name",unique = true)
    @Title(title = "参数名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Col(name = "code",unique = true)
    @Title(title = "参数编码")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Col(name="value",nullable = false)
    @Title(title = "参数值")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Title(title = "次序")
    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
