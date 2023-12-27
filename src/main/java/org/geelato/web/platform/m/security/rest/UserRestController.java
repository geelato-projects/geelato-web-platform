package org.geelato.web.platform.m.security.rest;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ApiResultStatus;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.util.UUIDUtils;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.AccountService;
import org.geelato.web.platform.m.security.service.OrgService;
import org.geelato.web.platform.m.security.service.UserService;
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
@RequestMapping(value = "/api/security/user")
public class UserRestController extends BaseController {
    private static final String DEFAULT_PASSWORD = "12345678";
    private static final int DEFAULT_PASSWORD_DIGIT = 8;

    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("name", "loginName", "orgName", "description"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    @Autowired
    protected AccountService accountService;
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
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<User> pageQueryList = userService.pageQueryModel(User.class, pageNum, pageSize, filterGroup);
            List<User> queryList = userService.queryModel(User.class, filterGroup);

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
        Map<String, Object> params = this.getQueryParameters(User.class, req);
        System.out.println(JSON.toJSON(params));
        try {
            return result.setData(userService.queryModel(User.class, params));
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
            User user = userService.getModel(User.class, id);
            user.setSalt(null);
            user.setPassword(null);
            return result.setData(user);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody User form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> uMap = new HashMap<>();
            // 组织
            if (Strings.isNotBlank(form.getOrgId())) {
                Org oForm = orgService.getModel(Org.class, form.getOrgId());
                if (oForm != null) {
                    form.setOrgName(oForm.getName());
                } else {
                    form.setOrgId(null);
                    form.setOrgName(null);
                }
            }
            // 组织ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 组织存在，方可更新
                User user = userService.getModel(User.class, form.getId());
                if (user != null) {
                    form.setPassword(user.getPassword());
                    form.setSalt(user.getSalt());
                    uMap = userService.updateModel(form);
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                form.setPlainPassword(UUIDUtils.generatePassword(DEFAULT_PASSWORD_DIGIT));
                accountService.entryptPassword(form);
                uMap = userService.createModel(form);
                uMap.put("plainPassword", form.getPlainPassword());
            }
            if (ApiResultStatus.SUCCESS.equals(result.getStatus())) {
                userService.setDefaultOrg(uMap);
                result.setData(uMap);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
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
                result.error().setMsg(ApiErrorMsg.OF_FAIL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.CREATE_FAIL);
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
                    result.setData(userService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.OF_FAIL);
                }
            } else {
                result.error().setMsg(ApiErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.UPDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/resetPwd/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult resetPassword(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            if (Strings.isNotBlank(id)) {
                User user = userService.getModel(User.class, id);
                Assert.notNull(user, ApiErrorMsg.IS_NULL);
                user.setPlainPassword(UUIDUtils.generatePassword(DEFAULT_PASSWORD_DIGIT));
                accountService.entryptPassword(user);
                userService.updateModel(user);
                result.setData(user.getPlainPassword());
            } else {
                result.error().setMsg(ApiErrorMsg.ID_IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult delete(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            if (Strings.isNotBlank(id)) {
                User user = userService.getModel(User.class, id);
                if (user != null) {
                    userService.deleteModel(User.class, id);
                    return result.success().setData(id);
                }
            }
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            User mResult = userService.getModel(User.class, id);
            if (mResult != null) {
                userService.isDeleteModel(mResult);
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

    @RequestMapping(value = "/validate/{type}", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult validate(@PathVariable(required = true) String type, @RequestBody User form) {
        ApiResult result = new ApiResult();
        try {
            if (Strings.isNotBlank(type)) {
                Map<String, String> params = new HashMap<>();
                if ("loginName".equalsIgnoreCase(type)) {
                    params.put("login_name", form.getLoginName());
                } else if ("enName".equalsIgnoreCase(type)) {
                    params.put("en_name", form.getEnName());
                } else if ("mobilePhone".equalsIgnoreCase(type)) {
                    params.put("mobile_phone", form.getMobilePhone());
                    params.put("mobile_prefix", form.getMobilePrefix());
                } else {
                    return result.setData(true);
                }
                params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
                params.put("tenant_code", form.getTenantCode());
                result.setData(userService.validate("platform_user", form.getId(), params));
            } else {
                result.error().setMsg(ApiErrorMsg.PARAMETER_MISSING);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }
}
