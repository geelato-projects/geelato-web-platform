package org.geelato.web.platform.m.model.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.model.service.DevTableColumnService;
import org.geelato.web.platform.m.model.service.DevTableService;
import org.geelato.web.platform.m.model.service.DevViewService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/model/table")
public class DevTableController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(DevTableController.class);
    private MetaManager metaManager = MetaManager.singleInstance();
    @Autowired
    private DevTableService devTableService;
    @Autowired
    private DevTableColumnService devTableColumnService;
    @Autowired
    private DevViewService devViewService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult<DataItems> pageQuery(HttpServletRequest req) {
        ApiPagedResult<DataItems> result = new ApiPagedResult<>();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(TableMeta.class, req);

            List<TableMeta> pageQueryList = devTableService.pageQueryModel(TableMeta.class, pageNum, pageSize, params);
            List<TableMeta> queryList = devTableService.queryModel(TableMeta.class, params);

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
    public ApiResult<List<TableMeta>> query(HttpServletRequest req) {
        ApiResult<List<TableMeta>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(TableMeta.class, req);
            return result.setData(devTableService.queryModel(TableMeta.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<TableMeta> get(@PathVariable(required = true) String id) {
        ApiResult<TableMeta> result = new ApiResult<>();
        try {
            return result.setData(devTableService.getModel(TableMeta.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<Map> createOrUpdate(@RequestBody TableMeta form) {
        ApiResult<Map> result = new ApiResult<>();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (devTableService.isExist(TableMeta.class, form.getId())) {
                    result.setData(devTableService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                Map<String, Object> resultMap = devTableService.createModel(form);
                form.setId(resultMap.get("id").toString());
                devTableColumnService.createDefaultColumn(form);
                result.setData(resultMap);
            }
            if (result.isSuccess() && Strings.isNotEmpty(form.getEntityName())) {
                // 刷新实体缓存
                metaManager.refreshDBMeta(form.getEntityName());
                // 刷新默认视图
                devViewService.createOrUpdateDefaultTableView(form, devTableColumnService.getDefaultViewSql(form.getEntityName()));

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
            TableMeta mResult = devTableService.getModel(TableMeta.class, id);
            if (mResult != null) {
                devTableService.isDeleteModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ApiErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/queryDefaultView/{entityName}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<String> queryDefaultView(@PathVariable(required = true) String entityName) {
        ApiResult<String> result = new ApiResult<>();
        try {
            return result.setData(devTableColumnService.getDefaultViewSql(entityName));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/resetDefaultView", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<String> resetDefaultView(@RequestBody TableMeta form) {
        ApiResult<String> result = new ApiResult<>();
        try {
            if (Strings.isNotBlank(form.getEntityName())) {
                Assert.notNull(form, ApiErrorMsg.IS_NULL);
                Map<String, Object> params = new HashMap<>();
                params.put("id", form.getId());
                params.put("connectId", form.getConnectId());
                params.put("entityName", form.getEntityName());
                List<TableMeta> tableMetaList = devTableService.queryModel(TableMeta.class, params);
                if (tableMetaList != null && tableMetaList.size() > 0) {
                    for (TableMeta meta : tableMetaList) {
                        devViewService.createOrUpdateDefaultTableView(meta, devTableColumnService.getDefaultViewSql(meta.getEntityName()));
                    }
                }
            } else {
                result.error().setMsg(ApiErrorMsg.UPDATE_FAIL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.UPDATE_FAIL);
        }

        return result;
    }


    @RequestMapping(value = {"/reset/{tableId}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult resetModelFormTable(@PathVariable("tableId") String tableId) {
        ApiMetaResult result = new ApiMetaResult();
        try {
            if (Strings.isNotBlank(tableId)) {
                // dev_table
                TableMeta tableMeta = devTableService.getModel(TableMeta.class, tableId);
                Assert.notNull(tableMeta, ApiErrorMsg.IS_NULL);
                // 禁用的不同步
                if (EnableStatusEnum.DISABLED.getCode() == tableMeta.getEnableStatus()) {
                    return (ApiMetaResult) result.error().setMsg(ApiErrorMsg.OBJECT_DISABLED);
                }
                /*if (Strings.isBlank(tableMeta.getTableName())) {
                    return (ApiMetaResult) result.error().setMsg(ApiErrorMsg.UPDATE_FAIL);
                }*/
                devTableService.resetTableByDataBase(tableMeta);
            } else {
                result.error().setMsg(ApiErrorMsg.ID_IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.UPDATE_FAIL);
        }
        return result;
    }
}