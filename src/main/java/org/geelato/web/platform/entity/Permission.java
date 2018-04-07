package org.geelato.web.platform.entity;

import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * Created by hongxueqian on 14-4-12.
 */
@Entity(name = "sys_permission")
public class Permission extends BaseEntity {

    private String name;

    private String text;

    private String description;

    @Title(title = "名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Title(title = "权限描述符")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
