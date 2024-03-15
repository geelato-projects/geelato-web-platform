package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.env.EnvManager;
import org.geelato.core.env.entity.SysConfig;
import org.geelato.core.util.StringUtils;
import org.geelato.web.platform.m.base.entity.CacheItemMeta;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/api/config")
public class ConfigController extends BaseController {


    @RequestMapping(value = {"", ""}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult list(HttpServletRequest request) {
        ApiResult result = new ApiResult();
        String tenantCode=request.getParameter("tenantCode");
        String appId=request.getParameter("appId");
        String purpose=request.getParameter("purpose");
        Map<String, SysConfig> configMap=EnvManager.singleInstance().getConfigMap();
        Map<String,Object> rtnConfigMap=new HashMap<>();
        Map<String,String> globalConfigMap=new HashMap<>();
        Map<String,String> tenantConfigMap=new HashMap<>();
        Map<String,String> appConfigMap=new HashMap<>();
        for (Map.Entry<String,SysConfig> entry: configMap.entrySet()) {
            SysConfig config = entry.getValue();
            if(StringUtils.isEmpty(config.getTenantCode())){
                globalConfigMap.put(config.getConfigKey(),config.getConfigValue());
                rtnConfigMap.put("sys",globalConfigMap);
            }
            if(!StringUtils.isEmpty(tenantCode)&&config.getTenantCode().equals(tenantCode)){
                tenantConfigMap.put(config.getConfigKey(),config.getConfigValue());
                rtnConfigMap.put("tenant",tenantConfigMap);
            }
            if(!StringUtils.isEmpty(appId)&&config.getAppId().equals(appId)){
                appConfigMap.put(config.getConfigKey(),config.getConfigValue());
                rtnConfigMap.put("app",appConfigMap);
            }

        }
        result.setData(rtnConfigMap);
        return result;
    }

}
