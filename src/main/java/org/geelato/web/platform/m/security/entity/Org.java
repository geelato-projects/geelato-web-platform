package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

@Entity(name = "platform_org")
@Title(title = "组织")
public class Org extends BaseSortableEntity {
    private String name;
    private String code;
    private long pid;
    private String type;
    private int status;
    private String description;

    @Title(title = "编码")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Title(title = "名称")
    @Col(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Title(title = "上级组织")
    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    @Title(title = "类型", description = "")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Title(title = "状态", description = "0:停用|1:启用")
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    @Title(title = "描述")
    @Col(name = "description", charMaxlength = 1024)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
