package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.IdEntity;

/**
 * Created by hongxq on 2015/6/17.
 */

@Entity(name = "platform_role_r_permission")
@Title(title = "角色权限关系表")
public class RolePermissionMap extends IdEntity {
    private Long roleId;

    private Long permissionId;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }
}
