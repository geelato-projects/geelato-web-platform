package org.geelato.web.platform.entity.settings;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

@Entity(name = "platform_module")
@Title(title = "模块")
public class Module extends BaseSortableEntity {
    private String title;
    private String code;
    private String index;
    private String resize;
    private String html;
    private int enable;
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


    @Title(title = "启用")
    @Col(name = "enable", numericPrecision = 1)
    public int getEnable() {
        return enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
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
