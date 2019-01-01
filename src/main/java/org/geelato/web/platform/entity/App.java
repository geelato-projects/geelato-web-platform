package org.geelato.web.platform.entity;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * Created by hongxueqian on 14-5-2.
 */
@Entity(name = "platform_app")
@Title(title = "应用")
public class App extends BaseSortableEntity {
    private String name;
    private String code;
    private String href;
    private String icon;
    private String menu;
    private String dependAppCode;
    private String description;

    public App() {
    }

    public App(Long id) {
        this.setId(id);
    }

    @Col(name = "name",unique = true)
    @Title(title = "名称")
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

    @Col(name = "href")
    @Title(title = "首页链接",description = "加载模块之后打开的首页面")
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Col(name = "icon")
    @Title(title = "图标")
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }


    @Col(name="menu",dataType = "json")
    @Title(title="菜单",description = "JSON格式，例如：[{\n" +
            "                \"text\": \"系统配置\", \"expanded\": true, \"items\": [\n" +
            "                    {\"text\": \"个人信息\", \"linkTo\": \"\\#/m/sys/profile\"},\n" +
            "                    {\"text\": \"应用管理\", \"linkTo\": \"\\#/m/sys/app/index\"}\n" +
            "                ]\n" +
            "            }]")
    public String getMenu() {
        return menu;
    }

    public void setMenu(String menu) {
        this.menu = menu;
    }

    @Col(name = "depend_app_code")
    @Title(title = "依赖的应用",description = "依赖的应用模块编码，可多个，格式如：dev,sys")
    public String getDependAppCode() {
        return dependAppCode;
    }

    public void setDependAppCode(String dependAppCode) {
        this.dependAppCode = dependAppCode;
    }

    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
