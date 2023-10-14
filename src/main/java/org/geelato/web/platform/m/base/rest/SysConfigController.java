package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.entity.SysConfig;
import org.geelato.web.platform.m.base.service.SysConfigService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/15 10:49
 */
@Controller
@RequestMapping(value = "/api/sys/config")
public class SysConfigController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("configKey", "configValue", "remark"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(SysConfigController.class);
    @Autowired
    private SysConfigService sysConfigService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(SysConfig.class, req);
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<SysConfig> pageQueryList = sysConfigService.pageQueryModel(SysConfig.class, pageNum, pageSize, filterGroup);
            List<SysConfig> queryList = sysConfigService.queryModel(SysConfig.class, filterGroup);

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
    public ApiResult<List<SysConfig>> query(HttpServletRequest req) {
        ApiResult<List<SysConfig>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(SysConfig.class, req);
            return result.setData(sysConfigService.queryModel(SysConfig.class, params));
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
            return result.setData(sysConfigService.getModel(SysConfig.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody SysConfig form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (sysConfigService.isExist(SysConfig.class, form.getId())) {
                    result.setData(sysConfigService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(sysConfigService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult<SysConfig> isDelete(@PathVariable(required = true) String id) {
        ApiResult<SysConfig> result = new ApiResult<>();
        try {
            SysConfig mResult = sysConfigService.getModel(SysConfig.class, id);
            if (mResult != null) {
                sysConfigService.isDeleteModel(mResult);
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
    public ApiResult validate(@RequestBody SysConfig form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("config_key", form.getConfigKey());
            params.put("app_id", form.getAppId());
            params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
            params.put("tenant_code", form.getTenantCode());
            result.setData(sysConfigService.validate("platform_sys_config", form.getId(), params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/getValue/{key}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getValue(@PathVariable(required = true) String key) {
        ApiResult result = new ApiResult();
        try {
            if (Strings.isNotBlank(key)) {
                Map<String, Object> params = new HashMap<>();
                params.put("configKey", key);
                List<SysConfig> list = sysConfigService.queryModel(SysConfig.class, params);
                if (list != null && list.size() > 0) {
                    return result.success().setData(list.get(0).getConfigValue());
                }
            }
            return result.success().setData("");
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }
}
