package org.geelato.web.platform.m.syspackage.entity;

import java.util.List;

public class AppPackage {

    private String sourceAppId;
    private String targetAppId;

    private String appCode;

    private List<AppMeta> appMetaList;

    public List<AppMeta> getAppMetaList() {
        return appMetaList;
    }

    public void setAppMetaList(List<AppMeta> appMetaList) {
        this.appMetaList = appMetaList;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getSourceAppId() {
        return sourceAppId;
    }

    public void setSourceAppId(String sourceAppId) {
        this.sourceAppId = sourceAppId;
    }

    public String getTargetAppId() {
        return targetAppId;
    }

    public void setTargetAppId(String targetAppId) {
        this.targetAppId = targetAppId;
    }
}
