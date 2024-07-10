package org.geelato.web.platform.m.syspackage.entity;

public class AppMeta {

    private String metaName;
    private Object metaData;

    public AppMeta(String metaName, Object metaData) {
        this.metaName = metaName;
        this.metaData = metaData;
    }

    public String getMetaName() {
        return metaName;
    }

    public void setMetaName(String metaName) {
        this.metaName = metaName;
    }

    public Object getMetaData() {
        return metaData;
    }

    public void setMetaData(Object metaData) {
        this.metaData = metaData;
    }
}
