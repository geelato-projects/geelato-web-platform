package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Permission;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RolePermissionMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class RolePermissionMapService extends BaseService {
    @Autowired
    private RoleService roleService;
    @Autowired
    private PermissionService permissionService;

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Map insertModel(RolePermissionMap model) {
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        Permission pModel = permissionService.getModel(Permission.class, model.getPermissionId());
        Assert.notNull(pModel, ApiErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setPermissionName(pModel.getName());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }

    /**
     * 表格权限
     *
     * @param type
     * @param object
     * @param appId
     * @param tenantCode
     * @return
     */
    public Map<String, JSONArray> queryTablePermissions(String type, String object, String appId, String tenantCode) {
        tenantCode = Strings.isNotBlank(tenantCode) ? tenantCode : getSessionTenantCode();
        Map<String, JSONArray> tablePermissionMap = new HashMap<>();
        // 表头，表格权限
        Map<String, Object> perParams = new HashMap<>();
        perParams.put("type", type);
        perParams.put("object", object);
        List<Permission> permissions = permissionService.queryModel(Permission.class, perParams);
        // 默认字段

        List<String> permissionIds = new ArrayList<>();
        if (permissions != null && permissions.size() > 0) {
            for (Permission model : permissions) {
                model.setDefault(permissionService.isDefault(model));
                permissionIds.add(model.getId());

            }
            tablePermissionMap.put("permission", JSON.parseArray(JSON.toJSONString(permissions)));
        } else {
            tablePermissionMap.put("permission", null);
        }
        // 第一列，角色
        Map<String, Object> roleParams = new HashMap<>();
        perParams.put("appId", appId);
        perParams.put("tenantCode", tenantCode);
        List<Role> roles = roleService.queryRoles(roleParams);
        List<String> roleIds = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (Role model : roles) {
                roleIds.add(model.getId());
            }
            tablePermissionMap.put("role", JSON.parseArray(JSON.toJSONString(roles)));
        } else {
            tablePermissionMap.put("role", null);
        }
        // 数据
        List<RolePermissionMap> rolePermissionMaps = new ArrayList<>();
        if (permissionIds.size() > 0 && roleIds.size() > 0) {
            FilterGroup filter = new FilterGroup();
            filter.addFilter("permissionId", FilterGroup.Operator.in, Strings.join(permissionIds, ','));
            filter.addFilter("roleId", FilterGroup.Operator.in, Strings.join(roleIds, ','));
            filter.addFilter("tenantCode", tenantCode);
            rolePermissionMaps = queryModel(RolePermissionMap.class, filter);
        }
        // 构建表格数据
        List<Map<String, Object>> tableMapList = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (Role role : roles) {
                Map<String, Object> tableMap = new HashMap<>();
                tableMap.put("id", role.getId());
                tableMap.put("appName", role.getAppName());
                tableMap.put("name", role.getName());
                tableMap.put("code", role.getCode());
                tableMap.put("type", role.getType());
                tableMap.put("description", role.getDescription());
                if (permissions != null && permissions.size() > 0) {
                    for (Permission permission : permissions) {
                        tableMap.put(permission.getId(), false);
                        if (rolePermissionMaps != null && rolePermissionMaps.size() > 0) {
                            for (RolePermissionMap model : rolePermissionMaps) {
                                if (role.getId().equals(model.getRoleId()) && permission.getId().equals(model.getPermissionId())) {
                                    tableMap.put(permission.getId(), true);
                                    break;
                                }
                            }
                        }
                    }
                }
                tableMapList.add(tableMap);
            }
        }
        tablePermissionMap.put("table", tableMapList.size() > 0 ? JSON.parseArray(JSON.toJSONString(tableMapList)) : null);

        return tablePermissionMap;
    }

    public void insertTablePermission(@RequestBody RolePermissionMap form) {
        if (Strings.isNotBlank(form.getRoleId()) && Strings.isNotBlank(form.getPermissionId())) {
            Map<String, Object> params = new HashMap<>();
            params.put("roleId", form.getRoleId());
            params.put("permissionId", form.getPermissionId());
            params.put("tenantCode", Strings.isNotBlank(form.getTenantCode()) ? form.getTenantCode() : getSessionTenantCode());
            List<RolePermissionMap> maps = queryModel(RolePermissionMap.class, params);
            if (maps != null && maps.size() > 0) {
                for (RolePermissionMap map : maps) {
                    deleteModel(RolePermissionMap.class, map.getId());
                }
            } else {
                insertModel(form);
            }
        } else {
            throw new RuntimeException(ApiErrorMsg.PARAMETER_MISSING);
        }
    }
}
