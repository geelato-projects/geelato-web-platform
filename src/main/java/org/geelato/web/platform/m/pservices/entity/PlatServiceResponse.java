package org.geelato.web.platform.m.pservices.entity;

import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

/**
 * @Description TODO
 * @Date 2020/4/10 19:24 by liuwq
 */
@Entity(name = "platform_svc_service_response")
@Title(title = "服务响应")
public class PlatServiceResponse extends BaseSortableEntity implements EntityEnableAble {
    private int enableStatus;

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
