package org.geelato.web.platform.entity.settings;


import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.web.platform.entity.TreeNodeAble;

@Entity(name = "platform_menu_item")
@Title(title = "菜单项", description = "菜单项与菜单子项")
public class MenuItem extends BaseSortableEntity implements TreeNodeAble {
    private String title;
    private String clazz;
    private String active;
    private String href;
    private Long treeNodeId;

    @Title(title = "标题")
    @Col(name = "title", nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Title(title = "样式类")
    @Col(name = "clazz")
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Title(title = "激活")
    @Col(name = "active")
    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    @Title(title = "链接")
    @Col(name = "href")
    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }


    @Title(title = "树节点id")
    @Col(name = "tree_node_id")
    @Override
    public Long getTreeNodeId() {
        return treeNodeId;
    }

    @Override
    public Long setTreeNodeId(Long treeNodeId) {
        return this.treeNodeId = treeNodeId;
    }
}
