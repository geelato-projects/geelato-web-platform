package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.ForeignKey;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * Created by hongxq on 2015/6/17.
 */

@Entity(name = "platform_role_r_permission")
@Title(title = "角色权限关系表")
public class RolePermissionMap extends BaseEntity {
    private Long roleId;

    private Long permissionId;

    private String roleName;
    private String permissionName;


    @Title(title = "角色ID")
    @Col(name = "role_id", refTables = "platform_role", refColName = "platform_role.id")
    @ForeignKey(fTable = Role.class)
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Title(title = "权限ID")
    @Col(name = "permission_id", refTables = "platform_permission", refColName = "platform_permission.id")
    @ForeignKey(fTable = Permission.class)
    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    @Title(title = "角色名称")
    @Col(name = "role_name", isRefColumn = true, refLocalCol = "roleId", refColName = "platform_role.name")
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Title(title = "权限名称")
    @Col(name = "permission_name", isRefColumn = true, refLocalCol = "permission_id", refColName = "platform_permission.name")
    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
}
