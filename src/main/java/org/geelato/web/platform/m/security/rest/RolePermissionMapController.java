package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.RolePermissionMap;
import org.geelato.web.platform.m.security.service.RolePermissionMapService;
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
@RequestMapping(value = "/api/security/role/permission")
public class RolePermissionMapController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("roleName", "permissionName"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(RolePermissionMapController.class);
    @Autowired
    private RolePermissionMapService rolePermissionMapService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(RolePermissionMap.class, req);
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<RolePermissionMap> pageQueryList = rolePermissionMapService.pageQueryModel(RolePermissionMap.class, pageNum, pageSize, filterGroup);
            List<RolePermissionMap> queryList = rolePermissionMapService.queryModel(RolePermissionMap.class, filterGroup);

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
    public ApiResult query(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = this.getQueryParameters(RolePermissionMap.class, req);
            return result.setData(rolePermissionMapService.queryModel(RolePermissionMap.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/insert", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult insert(@RequestBody RolePermissionMap form) {
        ApiResult result = new ApiResult();
        try {
            rolePermissionMapService.insertModel(form);
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
            RolePermissionMap rResult = rolePermissionMapService.getModel(RolePermissionMap.class, id);
            if (rResult != null) {
                rolePermissionMapService.isDeleteModel(rResult);
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
