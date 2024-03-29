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
import org.geelato.core.gql.parser.PageQueryRequest;
import org.geelato.core.util.UUIDUtils;
import org.geelato.web.platform.m.base.rest.BaseController;
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
    private static final Class<User> CLAZZ = User.class;

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
            PageQueryRequest pageQueryRequest = this.getPageQueryParameters(req);
            Map<String, Object> params = this.getQueryParameters(CLAZZ, req);
            result.setData(userService.queryModel(CLAZZ, params, pageQueryRequest.getOrderBy()));
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
            PageQueryRequest pageQueryRequest = this.getPageQueryParameters(req);
            Map<String, Object> params = this.getQueryParameters(CLAZZ, req);
            result.setData(userService.queryModel(CLAZZ, params, pageQueryRequest.getOrderBy()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/queryByParams", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult query(@RequestBody Map<String, Object> params) {
        ApiResult result = new ApiResult();
        try {
            if (params != null && !params.isEmpty()) {
                FilterGroup filterGroup = new FilterGroup();
                filterGroup.addFilter("id", FilterGroup.Operator.in, String.valueOf(params.get("ids")));
                return result.setData(userService.queryModel(CLAZZ, filterGroup));
            }
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
            User model = userService.getModel(CLAZZ, id);
            model.setSalt(null);
            model.setPassword(null);
            model.setPlainPassword(null);
            result.setData(model);
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
                User user = userService.getModel(CLAZZ, form.getId());
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
                userService.setDefaultOrg(JSON.parseObject(JSON.toJSONString(uMap), User.class));
            }
            uMap.put("salt", null);
            uMap.put("password", null);
            result.setData(uMap);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/resetPwd/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult resetPassword(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            if (Strings.isNotBlank(id)) {
                User user = userService.getModel(CLAZZ, id);
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

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            User model = userService.getModel(CLAZZ, id);
            Assert.notNull(model, ApiErrorMsg.IS_NULL);
            userService.isDeleteModel(model);
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
                } else if ("jobNumber".equalsIgnoreCase(type)) {
                    params.put("job_number", form.getJobNumber());
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
