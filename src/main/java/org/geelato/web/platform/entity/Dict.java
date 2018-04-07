package org.geelato.web.platform.entity;

import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * @author geemeta
 */
@Entity(name = "sys_dict")
@Title(title = "数据字典")
public class Dict extends BaseEntity {

    private Long groupId;
    private String groupCode;
    private String name;
    private String value;
    private int seq;
    private int enabled;
    private String description;

    @Col(name = "group_id")
    @Title(title = "分组ID")
    public Long getGroupId() {
        return groupId;
    }


    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    @Col(name = "group_code")
    @Title(title = "分组编码")
    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    @Title(title = "名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Title(title = "值")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Title(title = "次顺", description = "展示的次序")
    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    @Title(title = "启用状态", description = "1表示启用、0表示未启用")
    public int getEnabled() {
        return enabled;
    }

    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
