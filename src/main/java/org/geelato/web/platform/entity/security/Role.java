package org.geelato.web.platform.entity.security;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

/**
 * Created by hongxueqian on 14-4-12.
 */

@Entity(name = "platform_role")
@Title(title = "角色")
public class Role extends BaseSortableEntity implements EntityEnableAble {
    private String name;
    private String code;
    private String type;
    private int enabled = 1;
    //    private long treeNodeId;
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

    @Title(title = "类型", description = "")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Title(title = "描述")
    @Col(name = "description", charMaxlength = 1024)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Title(title = "启用状态", description = "1表示启用、0表示未启用")
    @Col(name = "enabled", nullable = false, dataType = "tinyint", numericPrecision = 1)
    @Override
    public int getEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }

//    @Title(title = "树节点")
//    @Col(name = "tree_node_id")
//    @Override
//    public Long getTreeNodeId() {
//        return this.treeNodeId;
//    }
//
//    @Override
//    public Long setTreeNodeId(Long treeNodeId) {
//        return this.treeNodeId = treeNodeId;
//    }
}
