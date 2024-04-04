package org.geelato.web.platform.m.security.service;

import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.service.AppService;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RoleAppMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author diabl
 */
@Component
public class RoleAppMapService extends BaseService {
    @Autowired
    private RoleService roleService;
    @Autowired
    private AppService appService;

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public RoleAppMap insertModel(RoleAppMap model) {
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        App aModel = appService.getModel(App.class, model.getAppId());
        Assert.notNull(aModel, ApiErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setAppName(aModel.getName());
        return super.createModel(model);
    }

    /**
     * 批量添加
     *
     * @param model
     */
    public void insertModels(RoleAppMap model) {
        // 角色
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        // 应用
        FilterGroup filter = new FilterGroup();
        filter.addFilter("id", FilterGroup.Operator.in, model.getAppIds());
        List<App> appList = appService.queryModel(App.class, filter);
        if (appList == null || appList.isEmpty()) {
            throw new RuntimeException(ApiErrorMsg.IS_NULL);
        }
        // 角色-应用
        FilterGroup filter1 = new FilterGroup();
        filter1.addFilter("appId", FilterGroup.Operator.in, model.getAppIds());
        filter1.addFilter("roleId", model.getRoleId());
        List<RoleAppMap> roleAppMapList = super.queryModel(RoleAppMap.class, filter1);
        List<String> appIds = new ArrayList<>();
        if (roleAppMapList != null && roleAppMapList.size() > 0) {
            for (RoleAppMap roleAppMap : roleAppMapList) {
                if (!appIds.contains(roleAppMap.getAppId())) {
                    appIds.add(roleAppMap.getAppId());
                }
            }
        }
        // 构建
        for (App app : appList) {
            if (!appIds.contains(app.getId())) {
                RoleAppMap roleAppMap = new RoleAppMap();
                roleAppMap.setRoleId(rModel.getId());
                roleAppMap.setRoleName(rModel.getName());
                roleAppMap.setAppId(app.getId());
                roleAppMap.setAppName(app.getName());
                super.createModel(roleAppMap);
            }
        }
    }
}
