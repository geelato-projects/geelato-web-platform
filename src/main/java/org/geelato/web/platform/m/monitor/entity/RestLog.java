package org.geelato.web.platform.m.monitor.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

import java.util.Date;

@Entity(name = "platform_rest_log")
@Title(title = "接口日志")
public class RestLog extends BaseSortableEntity {
    private Date happenedTime;
    private String client;
    private String params;
    private String action;
    private String userId;
    private long duration;
    private String description;

    @Title(title = "发生时间")
    @Col(name = "happened_time")
    public Date getHappenedTime() {
        return happenedTime;
    }

    public void setHappenedTime(Date happenedTime) {
        this.happenedTime = happenedTime;
    }

    @Title(title = "客户端")
    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    @Title(title = "参数")
    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    @Title(title = "访问方法")
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Title(title = "操作用户")
    @Col(name = "user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Title(title = "耗时（毫秒）")
    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Title(title = "描述")
    @Col(name = "description", charMaxlength = 1024)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
