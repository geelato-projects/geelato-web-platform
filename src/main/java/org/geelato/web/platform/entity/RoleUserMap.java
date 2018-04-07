package org.geelato.web.platform.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.IdEntity;

/**
 * Created by hongxq on 2015/6/17.
 */

@Entity(name = "sys_role_r_user")
@Title(title = "角色用户关系表")
public class RoleUserMap extends IdEntity {
    private Long roleId;

    private Long userId;

    @Title(title = "角色ID")
    @Col(name = "role_id")
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Title(title = "用户ID")
    @Col(name = "user_id")
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
