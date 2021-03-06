package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.ForeignKey;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;
import org.geelato.web.platform.m.base.entity.TreeNode;

/**
 * 菜单存在platform_tree_node中，字段flag为"menuItem"
 */

@Entity(name = "platform_role_r_tree_node")
@Title(title = "角色菜单关系表")
public class RoleTreeNodeMap extends BaseEntity {
    private Long roleId;

    private Long treeNodeId;

    private String treeNodeText;
    private String roleName;

    private String title;

    @Title(title = "名称")
    @Col(name = "title", nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Title(title = "角色ID")
    @Col(name = "role_id", refTables = "platform_role", refColName = "platform_role.id")
    @ForeignKey(fTable = Role.class)
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Title(title = "菜单ID")
    @Col(name = "tree_node_id", refTables = "platform_tree_node", refColName = "platform_tree_node.id")
    @ForeignKey(fTable = TreeNode.class)
    public Long getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(Long treeNodeId) {
        this.treeNodeId = treeNodeId;
    }

    @Title(title = "菜单名称")
    @Col(name = "tree_node_text", isRefColumn = true, refLocalCol = "treeNodeId", refColName = "platform_tree_node.text")
    public String getTreeNodeText() {
        return treeNodeText;
    }

    public void setTreeNodeText(String treeNodeText) {
        this.treeNodeText = treeNodeText;
    }

    @Title(title = "角色名称")
    @Col(name = "role_name", isRefColumn = true, refLocalCol = "roleId", refColName = "platform_role.name")
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
