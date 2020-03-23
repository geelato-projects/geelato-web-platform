package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.ForeignKey;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * Created by hongxq
 */

@Entity(name = "platform_org_r_user")
@Title(title = "组织用户关系表")
public class OrgUserMap extends BaseEntity {
    private Long orgId;

    private Long userId;

    //1-默认组织 0-兼职
    private int defaultOrg;

    @Title(title = "组织ID")
    @Col(name = "org_id")
    @ForeignKey(fTable = Org.class)
    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long roleId) {
        this.orgId = orgId;
    }

    @Title(title = "用户ID")
    @Col(name = "user_id")
    @ForeignKey(fTable = User.class)
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Title(title = "默认组织")
    @Col(name = "default_org")
    public int getDefaultOrg() {
        return defaultOrg;
    }

    public void setDefaultOrg(int defaultOrg) {
        this.defaultOrg = defaultOrg;
    }
}
