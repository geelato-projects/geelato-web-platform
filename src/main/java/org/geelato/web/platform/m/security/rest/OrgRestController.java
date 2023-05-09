package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.service.OrgService;
import org.geelato.web.platform.m.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/*
    created by chengx
 */
@Controller
@RequestMapping(value = "/api/security/org")
public class OrgRestController extends BaseController {
    private static final String ORG_ID_IS_NULL = "组织ID为空";
    private static final String ORG_IS_NULL = "组织不存在";
    private static final String ORG_FOR_USER = "组织已被引用";
    private static final String ORG_CREATE_FAIL = "组织创建失败";
    private static final String ORG_UPDATE_FAIL = "组织更新失败";
    private static final String ORG_DELETE_FAIL = "组织删除失败";
    private static final String ORG_QUERY_FAIL = "组织查询失败";
    @Autowired
    private OrgService orgService;
    @Autowired
    private UserService userService;
    private final Logger logger = LoggerFactory.getLogger(OrgRestController.class);

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQueryOrg() {
        ApiPagedResult result = new ApiPagedResult();
        try {
            return orgService.pageQueryOrg();
        } catch (Exception e) {
            logger.error(e.getMessage());

            result.error().setMsg(ORG_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryOrg(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        Map<String, Object> params = this.getQueryParameters(req);
        try {
            return result.setData(orgService.queryOrg(params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ORG_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getOrg(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(orgService.getOrg(id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ORG_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrg(@RequestBody Org org) {
        ApiResult result = new ApiResult();
        try {
            // 组织ID为空方可插入
            org.setId(null);
            return result.setData(orgService.createOrg(org));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ORG_CREATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult updateOrg(@RequestBody Org org) {
        ApiResult result = new ApiResult();
        try {
            // 组织ID不为空，方可更新
            if (org.getId() != null && org.getId() > 0) {
                // 组织存在，方可更新
                Org orgResult = orgService.getOrg(org.getId());
                if (orgResult != null) {
                    return result.setData(orgService.updateOrg(org));
                } else {
                    result.setMsg(ORG_IS_NULL);
                }
            } else {
                result.setMsg(ORG_ID_IS_NULL);
            }
            result.error();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ORG_UPDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult deleteOrg(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            // 被用户引用时，不可删除
            if (!userService.isExistUser(id)) {
                orgService.deleteOrg(id);
                result.success();
            } else {
                result.error().setMsg(ORG_FOR_USER);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ORG_DELETE_FAIL);
        }

        return result;
    }

}
