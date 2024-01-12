package org.geelato.web.platform.m.base.entity;

import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.annotation.Transient;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

/**
 * @Description 应用参数配置，
 * @Date 2020/4/12 12:00 by liuwq
 */
@Entity(name = "platform_sys_config")
@Title(title = "配置")
public class SysConfig extends BaseSortableEntity implements EntityEnableAble {

    private String appId;
    private String keyType;
    private String configKey;
    private String valueType;
    private String configValue;
    private String configAssist;
    private int enableStatus = ColumnDefault.ENABLE_STATUS_VALUE;
    private String remark;

    @Col(name = "app_id")
    @Title(title = "应用ID")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Col(name = "config_key")
    @Title(title = "配置键")
    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    @Col(name = "key_type")
    @Title(title = "键类型")
    public String getKeyType() {
        return keyType;
    }

    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }

    @Col(name = "value_type")
    @Title(title = "值类型")
    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    @Col(name = "config_value")
    @Title(title = "配置值")
    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    @Transient
    @Title(title = "辅助字段")
    public String getConfigAssist() {
        return configAssist;
    }

    public void setConfigAssist(String configAssist) {
        this.configAssist = configAssist;
    }

    @Col(name = "remark")
    @Title(title = "备注")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Col(name = "enable_status")
    @Title(title = "启用状态")
    @Override
    public int getEnableStatus() {
        return enableStatus;
    }

    /**
     * @param enableStatus
     */
    @Override
    public void setEnableStatus(int enableStatus) {
        this.enableStatus = enableStatus;
    }
}
