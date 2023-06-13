package org.geelato.web.platform.m.model.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.model.service.DevTableColumnService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/model/table/column")
public class DevTableColumnController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(DevTableColumnController.class);
    private MetaManager metaManager = MetaManager.singleInstance();
    @Autowired
    private DevTableColumnService devTableColumnService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult<DataItems> pageQuery(HttpServletRequest req) {
        ApiPagedResult<DataItems> result = new ApiPagedResult<>();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(ColumnMeta.class, req);

            List<ColumnMeta> pageQueryList = devTableColumnService.pageQueryModel(ColumnMeta.class, pageNum, pageSize, params);
            List<ColumnMeta> queryList = devTableColumnService.queryModel(ColumnMeta.class, params);

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
    public ApiResult<ColumnMeta> get(@PathVariable(required = true) long id) {
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
            if (form.getId() != null && form.getId() > 0) {
                // 存在，方可更新
                if (devTableColumnService.isExist(ColumnMeta.class, form.getId())) {
                    result.setData(devTableColumnService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(devTableColumnService.createModel(form));
            }
            // 刷新实体缓存
            if (result.isSuccess() && Strings.isNotEmpty(form.getTableName())) {
                metaManager.refreshDBMeta(form.getTableName());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            ColumnMeta mResult = devTableColumnService.getModel(ColumnMeta.class, id);
            if (mResult != null) {
                devTableColumnService.isDeleteModel(mResult);
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

    @RequestMapping(value = "/queryDefaultMeta", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<ColumnMeta>> queryDefaultMeta() {
        ApiResult<List<ColumnMeta>> result = new ApiResult<>();
        try {
            List<ColumnMeta> defaultColumnMetaList = MetaManager.singleInstance().getDefaultColumn();
            return result.setData(defaultColumnMetaList);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/validate", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<ColumnMeta> validate(String id, String tableId, String name) {
        ApiResult<ColumnMeta> result = new ApiResult<>();
        try {

        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }
}
