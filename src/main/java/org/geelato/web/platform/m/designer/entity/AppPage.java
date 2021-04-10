package org.geelato.web.platform.m.designer.entity;

import org.geelato.core.meta.annotation.Col;
import org.geelato.core.meta.annotation.Entity;
import org.geelato.core.meta.annotation.Title;
import org.geelato.core.meta.model.entity.BaseSortableEntity;

/**
 * @author itechgee@126.com
 * @date 2017/5/27.
 */
@Entity(name = "platform_app_page", table = "platform_app_page")
@Title(title = "页面配置")
public class AppPage extends BaseSortableEntity {

    private Long extendId;
    private Long appId;
    private String type;
    private String code;
    private String sourceContent;
    private String previewContent;
    private String releaseContent;
    private String description;

    @Col(name = "app_id", nullable = true)
    @Title(title = "应用Id", description = "所属应用ID")
    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    @Col(name = "extend_id", nullable = true)
    @Title(title = "扩展信息", description = "扩展id，如对应的叶子节点id")
    public Long getExtendId() {
        return extendId;
    }

    public void setExtendId(Long extendId) {
        this.extendId = extendId;
    }


    @Col(name = "type", nullable = false)
    @Title(title = "类型", description = "如api|form|table")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Col(name = "code", nullable = true)
    @Title(title = "编码")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    @Col(name = "source_content", nullable = false, dataType = "longText")
    @Title(title = "源文件内容")
    public String getSourceContent() {
        return sourceContent;
    }
    public void setSourceContent(String sourceContent) {
        this.sourceContent = sourceContent;
    }

    @Col(name = "preview_content", nullable = false, dataType = "longText")
    @Title(title = "预览的内容")
    public String getPreviewContent() {
        return previewContent;
    }

    public void setPreviewContent(String previewContent) {
        this.previewContent = previewContent;
    }

    @Col(name = "release_content", nullable = false, dataType = "longText")
    @Title(title = "发布的内容")
    public String getReleaseContent() {
        return releaseContent;
    }

    public void setReleaseContent(String releaseContent) {
        this.releaseContent = releaseContent;
    }

    @Title(title = "描述")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
