package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.gql.parser.PageQueryRequest;
import org.geelato.utils.StringUtils;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.service.AppService;
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
@RequestMapping(value = "/api/app")
public class AppController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();
    private static final Class<App> CLAZZ = App.class;
    private static final String DEFAULT_ORDER_BY = "seq_no ASC,update_at DESC";

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
            PageQueryRequest pageQueryRequest = this.getPageQueryParameters(req, DEFAULT_ORDER_BY);
            FilterGroup filterGroup = this.getFilterGroup(CLAZZ, req, OPERATORMAP);
            result = appService.pageQueryModel(CLAZZ, filterGroup, pageQueryRequest);
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
            PageQueryRequest pageQueryRequest = this.getPageQueryParameters(req, DEFAULT_ORDER_BY);
            Map<String, Object> params = this.getQueryParameters(CLAZZ, req);
            result.setData(appService.queryModel(CLAZZ, params, pageQueryRequest.getOrderBy()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/queryByUser", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryByUser(String tenantCode, String userId) {
        ApiResult result = new ApiResult<>();
        try {
            if (StringUtils.isBlank(userId)) {
                org.geelato.core.env.entity.User user = Ctx.getCurrentUser();
                userId = user != null ? user.getUserId() : "";
            }
            if (StringUtils.isBlank(tenantCode) || StringUtils.isBlank(userId)) {
                return result.error().setMsg(ApiErrorMsg.PARAMETER_MISSING);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("tenantCode", tenantCode);
            map.put("userId", userId);
            List<Map<String, Object>> appList = dao.queryForMapList("query_app_by_role_user", map);
            result.setData(appList);
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
            App model = appService.getModel(CLAZZ, id);
            appService.setConnects(model);
            result.setData(model);
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
            if (StringUtils.isNotBlank(form.getId())) {
                result.setData(appService.updateModel(form));
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
            App model = appService.getModel(CLAZZ, id);
            Assert.notNull(model, ApiErrorMsg.IS_NULL);
            appService.isDeleteModel(model);
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

    @RequestMapping(value = "/queryPermissionByPage", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryPermissionByPage(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = this.getQueryParameters(req);
            List<Map<String, Object>> queryList = dao.queryForMapList("platform_permission_by_app_page", params);
            result.setData(queryList);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/queryRolePermissionByPage", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryRolePermissionByPage(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = this.getQueryParameters(req);
            List<Map<String, Object>> queryList = dao.queryForMapList("platform_role_r_permission_by_app_page", params);
            result.setData(queryList);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }
}
