package org.geelato.web.platform.m.base.entity;

import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

/**
 * @author geelato
 */
@Entity(name = "platform_dict")
@Title(title = "数据字典")
public class Dict extends BaseSortableEntity implements EntityEnableAble {

    private String appId;
    private String dictCode;
    private String dictName;
    private String dictRemark;
    private int enableStatus = ColumnDefault.ENABLE_STATUS_VALUE;

    @Col(name = "dict_code")
    @Title(title = "字典编码")
    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    @Col(name = "dict_name")
    @Title(title = "字典名称")
    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    @Title(title = "应用Id")
    @Col(name = "app_id")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Col(name = "dict_remark")
    @Title(title = "字典备注")
    public String getDictRemark() {
        return dictRemark;
    }

    public void setDictRemark(String dictRemark) {
        this.dictRemark = dictRemark;
    }


    @Col(name = "enable_status")
    @Title(title = "启用状态")
    @Override
    public int getEnableStatus() {
        return this.enableStatus;
    }

    /**
     * @param enableStatus
     */
    @Override
    public void setEnableStatus(int enableStatus) {
        this.enableStatus = enableStatus;
    }
}
