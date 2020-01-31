package org.geelato.web.platform.m.base.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author itechgee@126.com
 * @date 2017/9/8.
 */
@Entity(name = "platform_app", table = "platform_app")
@Title(title = "应用")
public class App extends BaseSortableEntity {

    private String name;
    private String code;
    private String icon;
    private String tree;
    private String href;
    private String dependAppCode;
    private String description;

    @Col(name = "name", nullable = false)
    @Title(title = "应用名称")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Col(name = "code",unique = true)
    @Title(title = "编码")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Col(name = "icon")
    @Title(title = "图标")
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }


    @Col(name = "href")
    @Title(title = "首页链接",description = "加载模块之后打开的首页面")
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Col(name = "tree", nullable = false, dataType = "longText")
    @Title(title = "文件树", description = "json字符串，如jsTree")
    public String getTree() {
        return tree;
    }

    public void setTree(String tree) {
        this.tree = tree;
    }


    @Col(name = "depend_app_code")
    @Title(title = "依赖的应用",description = "依赖的应用模块编码，可多个，格式如：dev,sys")
    public String getDependAppCode() {
        return dependAppCode;
    }

    public void setDependAppCode(String dependAppCode) {
        this.dependAppCode = dependAppCode;
    }

    @Col(name = "description")
    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
