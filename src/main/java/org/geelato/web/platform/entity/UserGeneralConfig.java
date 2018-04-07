package org.geelato.web.platform.entity;


import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;

/**
 * Created by hongxueqian on 14-5-2.
 */
@Entity(name = "sys_user_general_config")
@Title(title = "用户配置")
public class UserGeneralConfig extends BaseEntity {
    private String ownerId;
    private String theme;

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
