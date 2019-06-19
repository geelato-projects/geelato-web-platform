package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.IdEntity;

/**
 * Created by hongxq
 */

@Entity(name = "platform_org_r_user")
@Title(title = "组织用户关系表")
public class OrgUserMap extends IdEntity {
    private Long orgId;

    private Long userId;

    @Title(title = "组织ID")
    @Col(name = "org_id")
    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long roleId) {
        this.orgId = orgId;
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
