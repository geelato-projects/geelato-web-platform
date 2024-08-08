package org.geelato.web.platform.script.entity;

import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.annotation.Transient;
import org.geelato.core.meta.model.entity.BaseEntity;

import java.util.List;

/**
 * @author diabl
 */
@Entity(name = "platform_api_param", table = "platform_api_param")
@Title(title = "接口服务参数")
public class ApiParam extends BaseEntity {
    private String appId;
    private String apiId;
    private String pid;
    private String paramType;
    private String bodyType;
    private String name;
    private String dataType;
    private String defaultValue;
    private String demoValue;
    private boolean required = false;
    private String remark;
    private List<ApiParam> children;

    @Col(name = "app_id")
    @Title(title = "所属应用")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Col(name = "api_id")
    @Title(title = "所属接口")
    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    @Col(name = "param_type")
    @Title(title = "参数类型")
    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    @Col(name = "name")
    @Title(title = "参数名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Col(name = "data_type")
    @Title(title = "参数数据类型")
    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Col(name = "default_value")
    @Title(title = "参数默认值")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Col(name = "demo_value")
    @Title(title = "示例数据")
    public String getDemoValue() {
        return demoValue;
    }

    public void setDemoValue(String demoValue) {
        this.demoValue = demoValue;
    }

    @Col(name = "required")
    @Title(title = "是否必填")
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Col(name = "remark")
    @Title(title = "备注")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Col(name = "pid")
    @Title(title = "父级ID")
    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    @Col(name = "body_type")
    @Title(title = "类型")
    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    @Transient
    public List<ApiParam> getChildren() {
        return children;
    }

    public void setChildren(List<ApiParam> children) {
        this.children = children;
    }
}
