package org.geelato.web.platform.m.designer.entity;

import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author geemeta
 *
 */
//@Entity(name = "platform_property_meta", table = "platform_property_meta")
//@Title(title = "属性元数据")
public class PropertyMeta extends BaseSortableEntity {

    private String componentMetaId;
    private String title;
    private String ComponentName;
    private String type;
    private String group;

    private String VModelName;

    private int showSetter;

    private String placeholder;

    private String description;

    @Title(title="所属组件",description = "所属组件元数据Id")
    public String getComponentMetaId() {
        return componentMetaId;
    }

    public void setComponentMetaId(String componentMetaId) {
        this.componentMetaId = componentMetaId;
    }

    @Title(title="属性名称",description = "中文名称")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Title(title="属性配置器",description = "对于每一个属性都有对应的配置器即一个用于配置的组件，这时填写配置组件的名称")
    public String getComponentName() {
        return ComponentName;
    }

    public void setComponentName(String componentName) {
        ComponentName = componentName;
    }

    @Title(title="属性类型",description = "是一个普通的属性，还是一个特殊的插槽属性、子组件属性，这里的普通属性，包数字、字符串、数组等")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Title(title="属性分组",description = "如基础|外观|数据")
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Title(title="v-model名称",description = "该属性配置器的v-model名称，默认为“value”，对于Radio则为“checked”，即配置器自动进行如下值绑定v-model:checked，该值与属性配置器联动，自动填写")
    public String getVModelName() {
        return VModelName;
    }

    public void setVModelName(String VModelName) {
        this.VModelName = VModelName;
    }

    @Title(title = "是否显示设置器")
    public int getShowSetter() {
        return showSetter;
    }

    public void setShowSetter(int showSetter) {
        this.showSetter = showSetter;
    }

    @Title(title = "占位描述",description = "组件占位信息描述，即组件的placeholder字段。")
    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Title(title = "详细提示",description = "详细的提示信息描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
