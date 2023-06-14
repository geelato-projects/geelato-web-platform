package org.geelato.web.platform.m.base.entity;

import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

/**
 * @Date 2020/4/10 18:12 by liuwq
 */
@Entity(name = "platform_dict_item")
@Title(title = "数据字典项")
public class DictItem extends BaseSortableEntity implements EntityEnableAble {

    private String dictId;
    private String itemCode;
    private String itemText;
    private String dataRemark;
    private int enableStatus = ColumnDefault.ENABLE_STATUS_VALUE;

    @Col(name = "dic_id")
    @Title(title = "字典ID")
    public String getDictId() {
        return dictId;
    }

    public void setDictId(String dictId) {
        this.dictId = dictId;
    }

    @Col(name = "item_code")
    @Title(title = "字典项编码")
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    @Col(name = "item_text")
    @Title(title = "字典项文本")
    public String getItemText() {
        return itemText;
    }

    public void setItemText(String itemText) {
        this.itemText = itemText;
    }

    @Col(name = "data_remark")
    @Title(title = "描述")
    public String getDataRemark() {
        return dataRemark;
    }

    public void setDataRemark(String dataRemark) {
        this.dataRemark = dataRemark;
    }

    @Col(name = "enable_status")
    @Title(title = "启用状态")
    @Override
    public int getEnableStatus() {
        return enableStatus;
    }

    @Override
    public void setEnableStatus(int enableStatus) {
        this.enableStatus = enableStatus;
    }
}
