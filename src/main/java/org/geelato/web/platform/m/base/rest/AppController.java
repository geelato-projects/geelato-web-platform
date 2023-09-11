package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.service.AppService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/app")
public class AppController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("name", "code", "description"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private AppService appService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(App.class, req);
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<App> pageQueryList = appService.pageQueryModel(App.class, pageNum, pageSize, filterGroup);
            List<App> queryList = appService.queryModel(App.class, filterGroup);

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
    public ApiResult<List<App>> query(HttpServletRequest req) {
        ApiResult<List<App>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(App.class, req);
            return result.setData(appService.queryModel(App.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(appService.getModel(App.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody App form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (appService.isExist(App.class, form.getId())) {
                    result.setData(appService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(appService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult<App> isDelete(@PathVariable(required = true) String id) {
        ApiResult<App> result = new ApiResult<>();
        try {
            App mResult = appService.getModel(App.class, id);
            if (mResult != null) {
                appService.isDeleteModel(mResult);
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

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult validate(@RequestBody App form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("code", form.getCode());
            params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
            params.put("tenant_code", form.getTenantCode());
            result.setData(appService.validate("platform_app", form.getId(), params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }
}
