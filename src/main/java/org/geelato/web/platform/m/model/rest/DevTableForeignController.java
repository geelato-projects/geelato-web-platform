package org.geelato.web.platform.m.model.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.TableForeign;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.model.service.DevTableForeignService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/model/table/foreign")
public class DevTableForeignController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("mainTableSchema", "mainTable", "mainTableCol", "foreignTableSchema", "foreignTable", "foreignTableCol", "description"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(DevTableForeignController.class);
    @Autowired
    private DevTableForeignService devTableForeignService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult<DataItems> pageQuery(HttpServletRequest req) {
        ApiPagedResult<DataItems> result = new ApiPagedResult<>();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(TableForeign.class, req);
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<TableForeign> pageQueryList = devTableForeignService.pageQueryModel(TableForeign.class, pageNum, pageSize, filterGroup);
            List<TableForeign> queryList = devTableForeignService.queryModel(TableForeign.class, filterGroup);

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
    public ApiResult<List<TableForeign>> query(HttpServletRequest req) {
        ApiResult<List<TableForeign>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(TableForeign.class, req);
            return result.setData(devTableForeignService.queryModel(TableForeign.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<TableForeign> get(@PathVariable(required = true) String id) {
        ApiResult<TableForeign> result = new ApiResult<>();
        try {
            return result.setData(devTableForeignService.getModel(TableForeign.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<Map> createOrUpdate(@RequestBody TableForeign form) {
        ApiResult<Map> result = new ApiResult<>();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (devTableForeignService.isExist(TableForeign.class, form.getId())) {
                    result.setData(devTableForeignService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(devTableForeignService.createModel(form));
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
            TableForeign mResult = devTableForeignService.getModel(TableForeign.class, id);
            if (mResult != null) {
                devTableForeignService.isDeleteModel(mResult);
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
}
