package org.geelato.web.platform.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

@Entity(name = "platform_common_config")
@Title(title = "平台设置")
public class CommonConfig extends BaseSortableEntity {
    private String name;
    private String code;
    private String value;
    private String ownerId;
    private String description;

    public CommonConfig() {
    }

    @Col(name = "name", unique = true)
    @Title(title = "参数名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Col(name = "code", unique = true)
    @Title(title = "参数编码", description = "如menu，表示")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Col(name = "value", nullable = false, dataType = "Text")
    @Title(title = "参数值")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Title(title = "所有者", description = "所有者为平台时，值为'platform'，若归属为具体某个账号，值为账号id。")
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Col(name = "description", charMaxlength = 5120)
    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
