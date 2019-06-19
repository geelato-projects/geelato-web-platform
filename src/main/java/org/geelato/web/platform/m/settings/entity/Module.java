package org.geelato.web.platform.m.settings.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.meta.model.entity.EntityEnableAble;

@Entity(name = "platform_module")
@Title(title = "模块")
public class Module extends BaseSortableEntity implements EntityEnableAble {
    private String title;
    private String code;
    private String index;
    private String resize;
    private String html;
    private int enabled;
    private String description;

    @Title(title = "编码")
    @Col(name = "code", nullable = false)
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Title(title = "模块名称")
    @Col(name = "title", nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Title(title = "默认首页")
    @Col(name = "href", nullable = false)
    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    @Title(title = "默认大小", description = "min|max")
    @Col(name = "resize", nullable = false, charMaxlength = 3)
    public String getResize() {
        return resize;
    }

    public void setResize(String resize) {
        this.resize = resize;
    }

    @Title(title = "模块名称html", description = "若有值时，展示该html的内容为模块的名称。")
    @Col(name = "html")
    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }


    @Title(title = "描述")
    @Col(name = "description", charMaxlength = 1024)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Title(title = "启用状态", description = "1表示启用、0表示未启用")
    @Col(name = "enabled", nullable = false, dataType = "tinyint", numericPrecision = 1)
    @Override
    public int getEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(int enabled) {
        this.enabled = enabled;
    }
}
