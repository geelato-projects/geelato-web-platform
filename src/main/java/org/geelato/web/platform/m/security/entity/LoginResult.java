package org.geelato.web.platform.m.security.entity;

import java.util.ArrayList;
import java.util.Date;

/**
 * @author geemeta
 */
public class LoginResult {
    private String id;
    private String name;
    private String loginName;
    private String avatar;
    private String mobilePrefix;
    private String mobilePhone;
    private String email;
    private Date registrationDate;
    private String orgId;
    private String orgName;
    private String description;
    private String tenantCode;

    private String token;
    private String homePath;
    private ArrayList<LoginRoleInfo> roles;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getMobilePrefix() {
        return mobilePrefix;
    }

    public void setMobilePrefix(String mobilePrefix) {
        this.mobilePrefix = mobilePrefix;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHomePath() {
        return homePath;
    }

    public void setHomePath(String homePath) {
        this.homePath = homePath;
    }

    public ArrayList<LoginRoleInfo> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<LoginRoleInfo> roles) {
        this.roles = roles;
    }

    /**
     * 对象转换
     *
     * @param user
     * @return
     */
    public static LoginResult formatLoginResult(User user) {
        LoginResult loginResult = new LoginResult();
        loginResult.setId(user.getId());
        loginResult.setName(user.getName());
        loginResult.setLoginName(user.getLoginName());
        loginResult.setAvatar(user.getAvatar());
        loginResult.setMobilePrefix(user.getMobilePrefix());
        loginResult.setMobilePhone(user.getMobilePhone());
        loginResult.setEmail(user.getEmail());
        loginResult.setRegistrationDate(user.getCreateAt());
        loginResult.setOrgId(user.getOrgId());
        loginResult.setOrgName(user.getOrgName());
        loginResult.setDescription(user.getDescription());
        loginResult.setTenantCode(user.getTenantCode());

        return loginResult;
    }
}
