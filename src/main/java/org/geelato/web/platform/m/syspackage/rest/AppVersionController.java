package org.geelato.web.platform.m.syspackage.rest;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.gql.parser.PageQueryRequest;
import org.geelato.utils.StringUtils;
import org.geelato.utils.ZipUtils;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.DownloadService;
import org.geelato.web.platform.m.syspackage.entity.AppPackage;
import org.geelato.web.platform.m.syspackage.entity.AppVersion;
import org.geelato.web.platform.m.syspackage.service.AppVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

/**
 * @author diabl
 * @date 2024/4/29 10:27
 */
@Controller
@RequestMapping(value = "/api/app/version")
public class AppVersionController extends BaseController {
    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();
    private static final Class<AppVersion> CLAZZ = AppVersion.class;
    private static final String DEFAULT_ORDER_BY = "create_at DESC";

    static {
        OPERATORMAP.put("contains", Arrays.asList("version", "description"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

    private final Logger logger = LoggerFactory.getLogger(AppVersionController.class);
    @Autowired
    private AppVersionService appVersionService;
    @Resource
    private AttachService attachService;
    @Resource
    private DownloadService downloadService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            PageQueryRequest pageQueryRequest = this.getPageQueryParameters(req, DEFAULT_ORDER_BY);
            FilterGroup filterGroup = this.getFilterGroup(CLAZZ, req, OPERATORMAP);
            result = appVersionService.pageQueryModel(CLAZZ, filterGroup, pageQueryRequest);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult<App> isDelete(@PathVariable(required = true) String id) {
        ApiResult<App> result = new ApiResult<>();
        try {
            AppVersion model = appVersionService.getModel(CLAZZ, id);
            Assert.notNull(model, ApiErrorMsg.IS_NULL);
            appVersionService.isDeleteModel(model);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            AppVersion model = appVersionService.getModel(CLAZZ, id);
            result.setData(model);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/package/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getPackage(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            AppVersion appVersion = appVersionService.getModel(CLAZZ, id);
            String appPackageData = "";
            if (appVersion != null && StringUtils.isNotEmpty(appVersion.getPackagePath())) {
                if (appVersion.getPackagePath().contains(".zgdp")) {
                    appPackageData = ZipUtils.readPackageData(appVersion.getPackagePath(), ".gdp");
                } else {
                    Attach attach = attachService.getModel(appVersion.getPackagePath());
                    File file = downloadService.downloadFile(attach.getName(), attach.getPath());
                    appPackageData = ZipUtils.readPackageData(file, ".gdp");
                }
            }
            AppPackage appPackage = null;
            if (StringUtils.isNotEmpty(appPackageData)) {
                appPackage = JSONObject.parseObject(appPackageData, AppPackage.class);
            }
            result.setData(appPackage);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody AppVersion form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                result.setData(appVersionService.updateModel(form));
            } else {
                result.setData(appVersionService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult validate(@RequestBody AppVersion form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("version", form.getVersion());
            params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
            params.put("app_id", form.getAppId());
            params.put("tenant_code", form.getTenantCode());
            result.setData(appVersionService.validate("platform_app_version", form.getId(), params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }
}
