package org.geelato.web.platform.m.model.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.ViewTypeEnum;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.model.entity.TableView;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: 表单视图服务类
 * @date 2023/6/15 9:48
 */
@Component
public class DevViewService extends BaseSortableService {

    /**
     * 仅创建、更新默认视图
     *
     * @param tableMeta
     * @param defaultViewSql
     */
    public void createOrUpdateDefaultTableView(TableMeta tableMeta, String defaultViewSql) {
        Assert.notNull(tableMeta, ApiErrorMsg.IS_NULL);
        if (Strings.isBlank(defaultViewSql)) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("connectId", tableMeta.getConnectId());
        params.put("entityName", tableMeta.getEntityName());
        params.put("viewType", ViewTypeEnum.DEFAULT.getCode());
        params.put("enableStatus", ColumnDefault.ENABLE_STATUS_VALUE);
        List<TableView> tableViewList = queryModel(TableView.class, params);
        if (tableViewList != null && !tableViewList.isEmpty()) {
            TableView meta = tableViewList.get(0);
            if (tableViewList.size() > 1) {
                for (int i = 1; i < tableViewList.size(); i++) {
                    deleteModel(TableView.class, tableViewList.get(i).getId());
                }
            }
            meta.setViewConstruct(defaultViewSql);
            meta.setTitle(String.format("%s的默认视图", tableMeta.getTitle()));
            meta.setViewName(String.format("v_%s", tableMeta.getEntityName()));
            updateModel(meta);
        } else {
            TableView meta = new TableView();
            meta.setConnectId(tableMeta.getConnectId());
            meta.setEntityName(tableMeta.getEntityName());
            meta.setTitle(String.format("%s的默认视图", tableMeta.getTitle()));
            meta.setViewName(String.format("v_%s", tableMeta.getEntityName()));
            meta.setViewType(ViewTypeEnum.DEFAULT.getCode());
            meta.setViewConstruct(defaultViewSql);
            meta.setLinked(tableMeta.getLinked());
            meta.setSeqNo(ColumnDefault.SEQ_NO_FIRST);
            createModel(meta);
        }
    }
}
