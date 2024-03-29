package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.web.platform.enums.PermissionTypeEnum;
import org.geelato.web.platform.enums.RoleTypeEnum;
import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.geelato.web.platform.m.security.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class RoleService extends BaseSortableService {
    @Lazy
    @Autowired
    private RoleAppMapService roleAppMapService;
    @Lazy
    @Autowired
    private RolePermissionMapService rolePermissionMapService;
    @Lazy
    @Autowired
    private RoleTreeNodeMapService roleTreeNodeMapService;
    @Lazy
    @Autowired
    private RoleUserMapService roleUserMapService;
    @Lazy
    @Autowired
    private PermissionService permissionService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(Role model) {
        // 组织删除
        model.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
        super.isDeleteModel(model);
        // 角色APP关系表
        Map<String, Object> params = new HashMap<>();
        params.put("roleId", model.getId());
        List<RoleAppMap> aList = roleAppMapService.queryModel(RoleAppMap.class, params);
        if (aList != null) {
            for (RoleAppMap rModel : aList) {
                roleAppMapService.isDeleteModel(rModel);
            }
        }
        // 角色权限关系表
        List<RolePermissionMap> pList = rolePermissionMapService.queryModel(RolePermissionMap.class, params);
        if (aList != null) {
            for (RolePermissionMap rModel : pList) {
                rolePermissionMapService.isDeleteModel(rModel);
            }
        }
        // 角色菜单关系表
        List<RoleTreeNodeMap> tList = roleTreeNodeMapService.queryModel(RoleTreeNodeMap.class, params);
        if (aList != null) {
            for (RoleTreeNodeMap rModel : tList) {
                roleTreeNodeMapService.isDeleteModel(rModel);
            }
        }
        // 角色用户关系表
        List<RoleUserMap> uList = roleUserMapService.queryModel(RoleUserMap.class, params);
        if (uList != null) {
            for (RoleUserMap rModel : uList) {
                roleUserMapService.isDeleteModel(rModel);
            }
        }
    }

    public Map<String, Object> updateModel(Role form) {
        // 原来的数据
        Role model = getModel(Role.class, form.getId());
        // 更新
        Map<String, Object> formMap = super.updateModel(form);
        // 关联表修改
        if (Strings.isNotBlank(form.getName()) && !form.getName().equals(model.getName())) {
            // 角色APP关系表
            Map<String, Object> params = new HashMap<>();
            params.put("roleId", model.getId());
            List<RoleAppMap> aList = roleAppMapService.queryModel(RoleAppMap.class, params);
            if (aList != null) {
                for (RoleAppMap rModel : aList) {
                    rModel.setRoleName(form.getName());
                    roleAppMapService.updateModel(rModel);
                }
            }
            // 角色权限关系表
            List<RolePermissionMap> pList = rolePermissionMapService.queryModel(RolePermissionMap.class, params);
            if (aList != null) {
                for (RolePermissionMap rModel : pList) {
                    rModel.setRoleName(form.getName());
                    rolePermissionMapService.updateModel(rModel);
                }
            }
            // 角色菜单关系表
            List<RoleTreeNodeMap> tList = roleTreeNodeMapService.queryModel(RoleTreeNodeMap.class, params);
            if (aList != null) {
                for (RoleTreeNodeMap rModel : tList) {
                    rModel.setRoleName(form.getName());
                    roleTreeNodeMapService.updateModel(rModel);
                }
            }
            // 角色用户关系表
            List<RoleUserMap> uList = roleUserMapService.queryModel(RoleUserMap.class, params);
            if (uList != null) {
                for (RoleUserMap rModel : uList) {
                    rModel.setRoleName(form.getName());
                    roleUserMapService.updateModel(rModel);
                }
            }
        }

        return formMap;
    }

    public Map<String, Object> createModel(Role form) {
        // 创建
        Map<String, Object> map = super.createModel(form);
        // 默认权限
        Map<String, Object> params = new HashMap<>();
        params.put("appId", form.getAppId());
        params.put("tenantCode", form.getTenantCode());
        List<TableMeta> tableMetaList = queryModel(TableMeta.class, params);
        List<String> objects = new ArrayList<>();
        for (TableMeta meta : tableMetaList) {
            objects.add(meta.getEntityName());
        }
        if (objects.size() == 0) {
            return map;
        }
        // 拥有的权限
        FilterGroup tableFilter = new FilterGroup();
        tableFilter.addFilter("type", FilterGroup.Operator.in, PermissionTypeEnum.getTablePermissions());
        tableFilter.addFilter("object", FilterGroup.Operator.in, String.join(",", objects));
        tableFilter.addFilter("tenantCode", form.getTenantCode());
        List<Permission> curPermissions = permissionService.queryModel(Permission.class, tableFilter);
        List<String> permissionIds = new ArrayList<>();
        for (Permission permission : curPermissions) {
            permissionIds.add(permission.getId());
        }
        if (permissionIds.size() == 0) {
            return map;
        }
        // 默认权限
        for (Permission permission : curPermissions) {
            for (String code : PermissionService.PERMISSION_DEFAULT_TO_ROLE) {
                if (permission.getCode().equals(String.format("%s%s", permission.getObject(), code))) {
                    rolePermissionMapService.createByRoleAndPermission(JSON.parseObject(JSON.toJSONString(map), Role.class), permission);
                }
            }
        }

        return map;
    }

    /**
     * 查询，平台级、应用级角色
     *
     * @param params
     * @return
     */
    public List<Role> queryRoles(Map<String, Object> params) {
        List<Role> roles = new ArrayList<>();
        String tenantCode = (String) params.get("tenantCode");
        tenantCode = Strings.isNotBlank(tenantCode) ? tenantCode : getSessionTenantCode();
        params.put("tenantCode", tenantCode);
        String appId = (String) params.get("appId");
        if (Strings.isNotBlank(appId)) {
            params.put("type", RoleTypeEnum.APP.getValue());
            List<Role> apps = queryModel(Role.class, params);
            roles.addAll(apps);
            params.put("type", RoleTypeEnum.PLATFORM.getValue());
            params.remove("appId");
            List<Role> platforms = queryModel(Role.class, params);
            roles.addAll(platforms);
        } else {
            // params.put("type", RoleTypeEnum.PLATFORM.getValue());
            roles = queryModel(Role.class, params);
        }
        // appName
        List<String> appIds = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (Role role : roles) {
                if (Strings.isNotBlank(role.getAppId()) && !appIds.contains(role.getAppId())) {
                    appIds.add(role.getAppId());
                }
            }
        }
        List<App> apps = new ArrayList<>();
        if (appIds != null && appIds.size() > 0) {
            FilterGroup filter = new FilterGroup().addFilter("id", FilterGroup.Operator.in, Strings.join(appIds, ','));
            apps = queryModel(App.class, filter);
        }
        // 填充
        if (apps != null && apps.size() > 0) {
            for (Role role : roles) {
                for (App app : apps) {
                    if (Strings.isNotBlank(app.getId()) && app.getId().equals(role.getAppId())) {
                        role.setAppName(app.getName());
                    }
                }
            }
        }

        return roles;
    }
}
