package org.geelato.web.platform.script.entty;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseEntity;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

@Entity(name = "platform_api", table = "platform_api")
@Title(title = "服务接口")
public class Api extends BaseEntity {
    private String release_content;
    @Col(name = "release_content", nullable = true)
    @Title(title = "服务脚本", description = "服务脚本")
    public String getRelease_content() {
        return release_content;
    }

    public void setRelease_content(String release_content) {
        this.release_content = release_content;
    }
}
