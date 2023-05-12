package org.geelato.web.platform.m.security.rest;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.OrgService;
import org.geelato.web.platform.m.security.service.UserService;
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
@RequestMapping(value = "/api/security/user")
public class UserRestController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private OrgService orgService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(User.class, req);

            List<User> pageQueryList = userService.pageQueryModel(User.class, pageNum, pageSize, params);
            List<User> queryList = userService.queryModel(User.class, params);

            result.setTotal(queryList != null ? queryList.size() : 0);
            result.setData(new DataItems(pageQueryList, result.getTotal()));
            result.setPage(pageNum);
            result.setSize(pageSize);
            result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult query(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        Map<String, Object> params = this.getQueryParameters(User.class, req);
        System.out.println(JSON.toJSON(params));
        try {
            return result.setData(userService.queryModel(User.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(userService.getModel(User.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody User form) {
        ApiResult result = new ApiResult();
        try {
            // 组织ID为空方可插入
            if (form.getId() != null && form.getId() > 0) {
                // 组织存在，方可更新
                if (userService.isExist(User.class, form.getId())) {
                    form.setDelStatus(0);
                    result.setData(userService.updateModel(form));
                } else {
                    result.error().setMsg(ErrorMsg.IS_NULL);
                }
            } else {
                result.setData(userService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult create(@RequestBody User form) {
        ApiResult result = new ApiResult();
        try {
            // 用户ID为空方可插入
            form.setId(null);
            // 组织ID为空 或 组织ID不为空且组织存在，可插入
            if (orgService.isExist(Org.class, form.getOrgId())) {
                return result.setData(userService.createModel(form));
            } else {
                result.error().setMsg(ErrorMsg.OF_FAIL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.CREATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult update(@RequestBody User form) {
        ApiResult result = new ApiResult();
        try {
            if (userService.isExist(User.class, form.getId())) {
                // 组织ID为空 或 组织ID不为空且组织存在，方可更新
                if (orgService.isExist(Org.class, form.getOrgId())) {
                    form.setDelStatus(0);
                    result.setData(userService.updateModel(form));
                } else {
                    result.error().setMsg(ErrorMsg.OF_FAIL);
                }
            } else {
                result.error().setMsg(ErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.UPDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult delete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            userService.deleteModel(User.class, id);
            result.success();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            User mResult = userService.getModel(User.class, id);
            if (mResult != null) {
                mResult.setDelStatus(1);
                userService.updateModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}
