package org.geelato.web.platform.m.excel.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author diabl
 * @description: 业务数据
 * @date 2023/10/15 10:53
 */
public class BusinessData {
    private int XIndex;
    private int YIndex;
    private Object value;
    private BusinessTypeData businessTypeData;
    private List<String> errorMsg = new ArrayList<>();

    public int getXIndex() {
        return XIndex;
    }

    public void setXIndex(int XIndex) {
        this.XIndex = XIndex;
    }

    public int getYIndex() {
        return YIndex;
    }

    public void setYIndex(int YIndex) {
        this.YIndex = YIndex;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public BusinessTypeData getBusinessTypeData() {
        return businessTypeData;
    }

    public void setBusinessTypeData(BusinessTypeData businessTypeData) {
        this.businessTypeData = businessTypeData;
    }

    public List<String> getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(List<String> errorMsg) {
        this.errorMsg = errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        if (this.errorMsg == null) {
            this.errorMsg = new ArrayList<>();
        }
        this.errorMsg.add(errorMsg);
    }

    public void setErrorMsgs(List<String> errorMsg) {
        if (this.errorMsg == null) {
            this.errorMsg = new ArrayList<>();
        }
        this.errorMsg.addAll(errorMsg);
    }

    public boolean isValidate() {
        return !(this.errorMsg != null && this.errorMsg.size() > 0);
    }
}
