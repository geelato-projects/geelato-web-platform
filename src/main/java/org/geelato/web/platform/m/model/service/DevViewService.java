package org.geelato.web.platform.m.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.ColumnEncryptedEnum;
import org.geelato.core.enums.ColumnSyncedEnum;
import org.geelato.core.enums.ViewTypeEnum;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.meta.model.view.TableView;
import org.geelato.core.util.ClassUtils;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
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

    public List<TableView> getTableView(String connectId, String entityName) {
        Map<String, Object> params = new HashMap<>();
        params.put("connectId", connectId);
        params.put("entityName", entityName);
        params.put("viewType", ViewTypeEnum.DEFAULT.getCode());
        params.put("enableStatus", ColumnDefault.ENABLE_STATUS_VALUE);
        List<TableView> tableViewList = queryModel(TableView.class, params);
        return tableViewList;
    }

    /**
     * 仅创建、更新默认视图
     */
    public void createOrUpdateDefaultTableView(TableMeta tableMeta, Map<String, Object> viewParams) {
        Assert.notNull(tableMeta, ApiErrorMsg.IS_NULL);
        String viewColumns = (String) viewParams.get("viewColumns");
        String viewConstruct = (String) viewParams.get("viewConstruct");
        if (Strings.isBlank(viewColumns) || Strings.isBlank(viewConstruct)) {
            return;
        }
        List<TableView> tableViewList = getTableView(tableMeta.getConnectId(), tableMeta.getEntityName());
        if (tableViewList != null && !tableViewList.isEmpty()) {
            TableView meta = tableViewList.get(0);
            if (tableViewList.size() > 1) {
                for (int i = 1; i < tableViewList.size(); i++) {
                    isDeleteModel(tableViewList.get(i));
                }
            }
            meta.setAppId(tableMeta.getAppId());
            meta.setViewConstruct(viewConstruct);
            meta.setViewColumn(viewColumns);
            meta.setTitle(String.format("%s的默认视图", tableMeta.getTitle()));
            meta.setViewName(String.format("v_%s", tableMeta.getEntityName()));
            meta.afterSet();
            updateModel(meta);
        } else {
            TableView meta = new TableView();
            meta.setAppId(tableMeta.getAppId());
            meta.setTenantCode(tableMeta.getTenantCode());
            meta.setConnectId(tableMeta.getConnectId());
            meta.setEntityName(tableMeta.getEntityName());
            meta.setTitle(String.format("%s的默认视图", tableMeta.getTitle()));
            meta.setViewName(String.format("v_%s", tableMeta.getEntityName()));
            meta.setViewType(ViewTypeEnum.DEFAULT.getCode());
            meta.setViewConstruct(viewConstruct);
            meta.setViewColumn(viewColumns);
            meta.setLinked(tableMeta.getLinked());
            meta.setSeqNo(ColumnDefault.SEQ_NO_FIRST);
            meta.afterSet();
            createModel(meta);
        }
    }

    public void viewColumnMapperDBObject(TableView form) {
        if (Strings.isNotBlank(form.getViewColumn())) {
            List<Object> list = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();
            JSONArray columnData = JSONArray.parse(form.getViewColumn());
            columnData.forEach(x -> {
                ColumnMeta meta = JSON.parseObject(x.toString(), ColumnMeta.class);
                meta.afterSet();
                columnNames.add(meta.getName());
                list.add(ClassUtils.toMapperDBObject(meta));
            });
            // 默认字段
            List<ColumnMeta> metaList = MetaManager.singleInstance().getDefaultColumn();
            if (metaList != null && metaList.size() > 0) {
                for (ColumnMeta meta : metaList) {
                    if (!columnNames.contains(meta.getName())) {
                        meta.setAppId(form.getAppId());
                        meta.setTenantCode(form.getTenantCode());
                        meta.setSynced(ColumnSyncedEnum.FALSE.getValue());
                        meta.setEncrypted(ColumnEncryptedEnum.FALSE.getValue());
                        meta.setTableName(form.getViewName());
                        list.add(ClassUtils.toMapperDBObject(meta));
                    }
                }
            }
            form.setViewColumn(JSON.toJSONString(list));
        }
    }

    public void viewColumnMeta(TableView form) {
        if (Strings.isNotBlank(form.getViewColumn())) {
            List<Object> list = new ArrayList<>();
            JSONArray columnData = JSONArray.parse(form.getViewColumn());
            columnData.forEach(x -> {
                Map<String, Object> m = JSON.parseObject(x.toString(), Map.class);
                list.add(ClassUtils.toMeta(ColumnMeta.class, m));
            });
            form.setViewColumn(JSON.toJSONString(list));
        }
    }

}
