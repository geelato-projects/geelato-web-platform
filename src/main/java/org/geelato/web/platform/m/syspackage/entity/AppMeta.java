package org.geelato.web.platform.m.syspackage.entity;

import java.util.List;

public class AppMeta {
    private String metaName;

    private List<Object> metaData;

    public String getMetaName() {
        return metaName;
    }

    public void setMetaName(String metaName) {
        this.metaName = metaName;
    }

    public List<Object> getMetaData() {
        return metaData;
    }

    public void setMetaData(List<Object> metaData) {
        this.metaData = metaData;
    }
}
