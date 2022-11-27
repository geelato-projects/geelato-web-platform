package org.geelato.web.platform.m.designer.entity;

import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author geemeta
 *
 */
//@Entity(name = "platform_method_meta", table = "platform_method_meta")
//@Title(title = "方法元数据")
public class MethodMeta extends BaseSortableEntity {

    private Long componentMetaId;
    private String title;
    private String name;



    @Title(title="所属组件",description = "所属组件元数据Id")
    public Long getComponentMetaId() {
        return componentMetaId;
    }

    public void setComponentMetaId(Long componentMetaId) {
        this.componentMetaId = componentMetaId;
    }
}
