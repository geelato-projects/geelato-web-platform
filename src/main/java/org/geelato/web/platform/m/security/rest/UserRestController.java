package org.geelato.web.platform.m.security.rest;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.OrgService;
import org.geelato.web.platform.m.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/security/user")
public class UserRestController extends BaseController {
    private static final String USER_ID_IS_NULL = "用户ID为空";
    private static final String USER_IS_NULL = "用户不存在";
    private static final String USER_OF_ORG = "关联组织不存在";
    private static final String USER_CREATE_FAIL = "用户创建失败";
    private static final String USER_UPDATE_FAIL = "用户更新失败";
    private static final String USER_DELETE_FAIL = "用户删除失败";
    private static final String USER_QUERY_FAIL = "用户查询失败";
    @Autowired
    private UserService userService;
    @Autowired
    private OrgService orgService;
    private final Logger logger = LoggerFactory.getLogger(UserRestController.class);

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQueryUser() {
        ApiPagedResult result = new ApiPagedResult();
        try {
            return userService.pageQueryUser();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryUser(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        Map<String, Object> params = this.getQueryParameters(req);
        System.out.println(JSON.toJSON(params));
        try {
            return result.setData(userService.queryUser(params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getUser(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(userService.getUser(id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createUser(@RequestBody User user) {
        ApiResult result = new ApiResult();
        try {
            // 用户ID为空方可插入
            user.setId(null);
            // 组织ID为空 或 组织ID不为空且组织存在，可插入
            if (orgService.isExistOrg(user.getOrgId())) {
                return result.setData(userService.createUser(user));
            } else {
                result.error().setMsg(USER_OF_ORG);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_CREATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult updateUser(@RequestBody User user) {
        ApiResult result = new ApiResult();
        try {
            // 用户ID不为空，方可更新
            if (user.getId() != null && user.getId() > 0) {
                // 用户存在，方可更新
                User userResult = userService.getUser(user.getId());
                if (userResult != null) {
                    // 组织ID为空 或 组织ID不为空且组织存在，方可更新
                    if (orgService.isExistOrg(user.getOrgId())) {
                        return result.setData(userService.updateUser(user));
                    } else {
                        result.setMsg(USER_OF_ORG);
                    }
                } else {
                    result.setMsg(USER_IS_NULL);
                }
            } else {
                result.setMsg(USER_ID_IS_NULL);
            }
            result.error();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_UPDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult deleteUser(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            userService.deleteUser(id);
            result.success();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(USER_DELETE_FAIL);
        }

        return result;
    }

}
