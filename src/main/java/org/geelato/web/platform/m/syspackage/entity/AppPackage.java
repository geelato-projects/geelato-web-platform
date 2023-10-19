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
}
