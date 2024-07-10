package org.geelato.web.platform.m.excel.entity;

/**
 * @author diabl
 * @description: 表头所在的位置，对应的数据类型
 */
public class BusinessColumnMeta {
    private int index;
    private BusinessTypeData businessTypeData;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public BusinessTypeData getBusinessTypeData() {
        return businessTypeData;
    }

    public void setBusinessTypeData(BusinessTypeData businessTypeData) {
        this.businessTypeData = businessTypeData;
    }
}