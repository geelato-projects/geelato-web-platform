package org.geelato.web.platform.m.model.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.ColumnSyncedEnum;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.meta.model.field.ColumnSelectType;
import org.geelato.web.platform.enums.PermissionTypeEnum;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.model.service.DevTableColumnService;
import org.geelato.web.platform.m.model.service.DevViewService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/model/table/column")
public class DevTableColumnController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("title", "fieldName", "name", "comment", "description"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(DevTableColumnController.class);
    private final MetaManager metaManager = MetaManager.singleInstance();
    @Autowired
    private DevTableColumnService devTableColumnService;
    @Autowired
    private DevViewService devViewService;
    @Autowired
    private PermissionService permissionService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult<DataItems> pageQuery(HttpServletRequest req) {
        ApiPagedResult<DataItems> result = new ApiPagedResult<>();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(ColumnMeta.class, req);
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<ColumnMeta> pageQueryList = devTableColumnService.pageQueryModel(ColumnMeta.class, pageNum, pageSize, filterGroup);
            List<ColumnMeta> queryList = devTableColumnService.queryModel(ColumnMeta.class, filterGroup);

            result.setTotal(queryList != null ? queryList.size() : 0);
            result.setData(new DataItems(pageQueryList, result.getTotal()));
            result.setPage(pageNum);
            result.setSize(pageSize);
            result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<ColumnMeta>> query(HttpServletRequest req) {
        ApiResult<List<ColumnMeta>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(ColumnMeta.class, req);
            return result.setData(devTableColumnService.queryModel(ColumnMeta.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<ColumnMeta> get(@PathVariable(required = true) String id) {
        ApiResult<ColumnMeta> result = new ApiResult<>();
        try {
            return result.setData(devTableColumnService.getModel(ColumnMeta.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<Map> createOrUpdate(@RequestBody ColumnMeta form) {
        ApiResult<Map> result = new ApiResult<>();
        try {
            form.afterSet();
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                ColumnMeta meta = devTableColumnService.getModel(ColumnMeta.class, form.getId());
                Assert.notNull(meta, ApiErrorMsg.IS_NULL);
                form = devTableColumnService.upgradeTable(form, meta);
                Map<String, Object> resultMap = devTableColumnService.updateModel(form);
                if (!meta.getName().equalsIgnoreCase(form.getName())) {
                    permissionService.columnPermissionChangeObject(form.getTableName(), form.getName(), meta.getName());
                    permissionService.resetDefaultPermission(PermissionTypeEnum.COLUMN.getValue(), form.getTableName(), form.getAppId());
                }
                result.setData(resultMap);
            } else {
                form.setSynced(ColumnSyncedEnum.FALSE.getValue());
                Map<String, Object> resultMap = devTableColumnService.createModel(form);
                permissionService.resetDefaultPermission(PermissionTypeEnum.COLUMN.getValue(), form.getTableName(), form.getAppId());
                result.setData(resultMap);
            }
            // 选择类型为 组织、用户时
            if (result.isSuccess()) {
                devTableColumnService.automaticGeneration(form);
            }
            // 刷新实体缓存
            if (result.isSuccess() && Strings.isNotEmpty(form.getTableName())) {
                metaManager.refreshDBMeta(form.getTableName());
                // 刷新默认视图
                devViewService.createOrUpdateDefaultTableView(devTableColumnService.getModel(TableMeta.class, form.getTableId()), devTableColumnService.getDefaultViewSql(form.getTableName()));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/insertCommon", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<Map> insertCommon(@RequestBody Map<String, Object> params) {
        ApiResult<Map> result = new ApiResult<>();
        try {
            String tableId = String.valueOf(params.get("tableId"));
            String tableName = String.valueOf(params.get("tableName"));
            String columnIds = String.valueOf(params.get("columnIds"));
            if (Strings.isBlank(tableId) || Strings.isBlank(columnIds)) {
                return result.error().setMsg(ApiErrorMsg.PARAMETER_MISSING);
            }
            //
            TableMeta tableMeta = devTableColumnService.getModel(TableMeta.class, tableId);
            Assert.notNull(tableMeta, ApiErrorMsg.IS_NULL);
            // 需要添加的字段
            FilterGroup columnFilter = new FilterGroup();
            columnFilter.addFilter("id", FilterGroup.Operator.in, columnIds);
            List<ColumnMeta> columnMetas = devTableColumnService.queryModel(ColumnMeta.class, columnFilter);
            if (columnMetas == null || columnMetas.size() == 0) {
                return result;
            }
            // 已有字段，
            List<String> existColumnNames = new ArrayList<>();
            Map<String, Object> columnParams = new HashMap<>();
            columnParams.put("tableId", tableId);
            List<ColumnMeta> existColumns = devTableColumnService.queryModel(ColumnMeta.class, columnParams);
            if (existColumns != null && existColumns.size() > 0) {
                for (ColumnMeta meta : existColumns) {
                    if (!existColumnNames.contains(meta.getName())) {
                        existColumnNames.add(meta.getName());
                    }
                }
            }
            // 添加字段
            for (ColumnMeta meta : columnMetas) {
                if (!existColumnNames.contains(meta.getName())) {
                    meta.setId(null);
                    meta.setTableId(tableMeta.getId());
                    meta.setTableName(tableMeta.getEntityName());
                    meta.setSynced(ColumnSyncedEnum.FALSE.getValue());
                    devTableColumnService.createModel(meta);
                }
            }
            // 刷新实体缓存
            if (result.isSuccess() && Strings.isNotEmpty(tableMeta.getEntityName())) {
                metaManager.refreshDBMeta(tableMeta.getEntityName());
                // 刷新默认视图
                devViewService.createOrUpdateDefaultTableView(devTableColumnService.getModel(TableMeta.class, tableMeta.getId()), devTableColumnService.getDefaultViewSql(tableMeta.getEntityName()));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            ColumnMeta mResult = devTableColumnService.getModel(ColumnMeta.class, id);
            if (mResult != null) {
                devTableColumnService.isDeleteModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ApiErrorMsg.IS_NULL);
            }
            // 刷新实体缓存
            if (result.isSuccess() && Strings.isNotEmpty(mResult.getTableName())) {
                metaManager.refreshDBMeta(mResult.getTableName());
                // 刷新默认视图
                devViewService.createOrUpdateDefaultTableView(devTableColumnService.getModel(TableMeta.class, mResult.getTableId()), devTableColumnService.getDefaultViewSql(mResult.getTableName()));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/queryDefaultMeta", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<ColumnMeta>> queryDefaultMeta() {
        ApiResult<List<ColumnMeta>> result = new ApiResult<>();
        try {
            List<ColumnMeta> defaultColumnMetaList = metaManager.getDefaultColumn();
            return result.setData(defaultColumnMetaList);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/selectType", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<ColumnSelectType>> getSelectType() {
        ApiResult<List<ColumnSelectType>> result = new ApiResult<>();
        try {
            List<ColumnSelectType> selectTypes = metaManager.getColumnSelectType();
            return result.success().setData(selectTypes);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult validate(@RequestBody ColumnMeta form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("column_name", form.getName());
            params.put("table_id", form.getTableId());
            params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
            params.put("app_id", form.getAppId());
            params.put("tenant_code", form.getTenantCode());
            result.setData(devTableColumnService.validate("platform_dev_column", form.getId(), params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }
}
