package org.geelato.web.platform.m.security.entity;

import java.util.ArrayList;

/**
 * @author geemeta
 */
public class LoginResult {
    private String userId;
    private String username;
    private String realName;
    private String token;
    private String avatar;
    private String desc;
    private String homePath;
    private ArrayList<LoginRoleInfo> roles;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ArrayList<LoginRoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<LoginRoleInfo> roles) {
        this.roles = roles;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getHomePath() {
        return homePath;
    }

    public void setHomePath(String homePath) {
        this.homePath = homePath;
    }
}
