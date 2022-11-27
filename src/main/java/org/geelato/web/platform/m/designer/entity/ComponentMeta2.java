package org.geelato.web.platform.m.designer.entity;

import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author geemeta
 */
//@Entity(name = "platform_component_meta", table = "platform_component_meta")
//@Title(title = "组件元数据")
public class ComponentMeta2 extends BaseSortableEntity {

    private String title;
    private String ComponentName;
    private String packageName;
    private String alias;
    private String group;
    private String SettingDisplayMode;
    private String SettingPanels;

    private String thumbnail;
    public int publicStatus;

    @Title(title = "组件标题")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Title(title = "组件名", description = "英文名称")
    @Col(name = "component_name", nullable = false)
    public String getComponentName() {
        return ComponentName;
    }

    public void setComponentName(String componentName) {
        ComponentName = componentName;
    }

    @Title(title = "组件包名", description = "组件的命名空间")
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Title(title = "组件别名", description = "简短的别名，用于创建便于识别的组织维一标识")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Title(title = "组件分组")
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Title(title = "设置面板模式", description = "组件属性设置面板的展示模式")
    public String getSettingDisplayMode() {
        return SettingDisplayMode;
    }

    public void setSettingDisplayMode(String settingDisplayMode) {
        SettingDisplayMode = settingDisplayMode;
    }

    @Title(title = "更多设置面板", description = "更多的设置面板名称，多个用逗号分开")
    public String getSettingPanels() {
        return SettingPanels;
    }

    public void setSettingPanels(String settingPanels) {
        SettingPanels = settingPanels;
    }

    @Title(title="发布状态",description = "1:已发布；0：待发布")
    public int getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(int publicStatus) {
        this.publicStatus = publicStatus;
    }

}
