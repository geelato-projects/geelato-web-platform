package org.geelato.web.platform.m.security.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.annotation.Transient;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author geelato
 */
@Entity(name = "platform_user")
@Title(title = "用户")
public class User extends BaseSortableEntity {
    private String name;
    private String loginName;
    private int sex;
    private long orgId;
    private String password;
    private String salt;
    private String avatar;
    private String plainPassword;
    private String mobilePhone;
    private String telephone;
    private String email;
    private String post;
    private int type;
    private int source;
    private String provinceCode;
    private String cityCode;
    private String description;


    public User() {
    }

    public User(Long id) {
        this.setId(id);
    }

    @Title(title = "名称")
    @Col(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Title(title = "登录名")
    @Col(name = "login_name", nullable = false)
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    @Title(title = "组织")
    @Col(name = "org_id")
    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    @Title(title = "密码")
    @Col(name = "password", nullable = false)
//    @JsonDeserialize(using = JsonIgnoreDeserialize.class)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Title(title = "性别")
    @Col(name = "sex", nullable = false)
    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    @Title(title = "Salt")
    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    @Title(title = "头像")
    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Title(title = "描述")
    @Col(name = "description", charMaxlength = 1024)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Title(title = "明文密码")
    // 不持久化到数据库，也不显示在Restful接口的属性.
    @Transient
    public String getPlainPassword() {
        return plainPassword;
    }

    public void setPlainPassword(String plainPassword) {
        this.plainPassword = plainPassword;
    }

    @Title(title = "手机")
    @Col(name = "mobile_phone", charMaxlength = 16)
    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    @Title(title = "电话")
    @Col(name = "telephone", charMaxlength = 20)
    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @Title(title = "邮箱")
    @Col(name = "email", charMaxlength = 126)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Title(title = "职务")
    @Col(name = "post", charMaxlength = 40)
    public String getPost() {
        return post;
    }


    public void setPost(String post) {
        this.post = post;
    }


    @Title(title = "来源", description = "0:本用用户|1:用户平台")
    @Col(name = "source", nullable = false)
    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    @Title(title = "类型", description = "0:员工账号|1:系统账号|2:企业外人员")
    @Col(name = "type", nullable = false)
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Title(title = "省份")
    @Col(name = "province_code")
    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    @Title(title = "城市")
    @Col(name = "city_code")
    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }
}
