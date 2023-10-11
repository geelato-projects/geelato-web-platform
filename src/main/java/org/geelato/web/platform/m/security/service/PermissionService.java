package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ResourcesFiles;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.util.FastJsonUtils;
import org.geelato.web.platform.enums.PermissionTypeEnum;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Permission;
import org.geelato.web.platform.m.security.entity.RolePermissionMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class PermissionService extends BaseService {
    public static final String PERMISSION_COLUMN = ResourcesFiles.PERMISSION_COLUMN_DEFAULT_JSON;
    public static final String PERMISSION_TABLE = ResourcesFiles.PERMISSION_TABLE_DEFAULT_JSON;
    @Lazy
    @Autowired
    private RolePermissionMapService rolePermissionMapService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(Permission model) {
        // 用户删除
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 角色权限关系表
        Map<String, Object> params = new HashMap<>();
        params.put("permissionId", model.getId());
        List<RolePermissionMap> rList = rolePermissionMapService.queryModel(RolePermissionMap.class, params);
        if (rList != null) {
            for (RolePermissionMap oModel : rList) {
                oModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(oModel);
            }
        }
    }

    public boolean isDefault(Permission model) {
        boolean isDef = false;
        if (Strings.isBlank(model.getCode()) || Strings.isBlank(model.getType()) || Strings.isBlank(model.getObject())) {
            return isDef;
        }
        List<Permission> defaultPermissions = new ArrayList<>();
        if (PermissionTypeEnum.DP.getValue().equals(model.getType())) {
            defaultPermissions = getDefaultPermission(PermissionService.PERMISSION_TABLE);
        } else if (PermissionTypeEnum.EP.getValue().equals(model.getType())) {
            defaultPermissions = getDefaultPermission(PermissionService.PERMISSION_COLUMN);
        }
        if (defaultPermissions != null && defaultPermissions.size() > 0) {
            for (Permission dp : defaultPermissions) {
                if (model.getCode().equals(String.format("%s%s", model.getObject(), dp.getCode()))) {
                    isDef = true;
                    break;
                }
            }
        }

        return isDef;
    }

    public List<Permission> getDefaultPermission(String jsonFile) {
        List<Permission> defaultPermissions = new ArrayList<Permission>();

        try {
            String jsonStr = FastJsonUtils.readJsonFile(jsonFile);
            List<Permission> permissionList = JSON.parseArray(jsonStr, Permission.class);
            if (permissionList != null && !permissionList.isEmpty()) {
                for (Permission permission : permissionList) {
                    permission.afterSet();
                    defaultPermissions.add(permission);
                }
            }
        } catch (IOException e) {
            defaultPermissions = new ArrayList<>();
        }

        return defaultPermissions;
    }

    public void resetDefaultPermission(String type, String object) {
        if (PermissionTypeEnum.DP.getValue().equals(type)) {
            resetTableDefaultPermission(type, object);
        } else if (PermissionTypeEnum.EP.getValue().equals(type)) {
            resetColumnDefaultPermission(type, object);
        } else {
            throw new RuntimeException("[type] non-being");
        }
    }

    public void resetTableDefaultPermission(String type, String object) {
        // 当前权限
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("object", object);
        params.put("tenantCode", getSessionTenantCode());
        List<Permission> curPermissions = queryModel(Permission.class, params);
        // 默认权限
        List<Permission> defPermissions = getDefaultPermission(PermissionService.PERMISSION_TABLE);
        if (defPermissions != null && defPermissions.size() > 0) {
            if (curPermissions != null && curPermissions.size() > 0) {
                for (Permission dModel : defPermissions) {
                    boolean isExist = false;
                    for (Permission cModel : curPermissions) {
                        if (cModel.getCode().equals(String.format("%s%s", cModel.getObject(), dModel.getCode()))) {
                            cModel.setName(dModel.getName());
                            cModel.setRule(dModel.getRule());
                            cModel.setDescription(dModel.getDescription());
                            updateModel(cModel);
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        dModel.setObject(object);
                        dModel.setCode(String.format("%s%s", object, dModel.getCode()));
                        createModel(dModel);
                    }
                }
            } else {
                for (Permission dModel : defPermissions) {
                    dModel.setObject(object);
                    dModel.setCode(String.format("%s%s", object, dModel.getCode()));
                    createModel(dModel);
                }
            }
        }
    }

    public void resetColumnDefaultPermission(String type, String tableName) {
        // 表头
        Map<String, Object> colParams = new HashMap<>();
        colParams.put("tableName", tableName);
        colParams.put("tenantCode", getSessionTenantCode());
        List<ColumnMeta> columnMetas = queryModel(ColumnMeta.class, colParams);
        // 默认字段
        List<String> columnObjects = new ArrayList<>();
        if (columnMetas != null && columnMetas.size() > 0) {
            for (ColumnMeta model : columnMetas) {
                columnObjects.add(tableName + ":" + model.getName());
            }
        }
        // 当前权限
        List<Permission> permissions = new ArrayList<>();
        if (columnObjects != null && columnObjects.size() > 0) {
            FilterGroup filter = new FilterGroup();
            filter.addFilter("type", type);
            filter.addFilter("object", FilterGroup.Operator.in, Strings.join(columnObjects, ','));
            filter.addFilter("tenantCode", getSessionTenantCode());
            permissions = queryModel(Permission.class, filter);
        }
        // 默认字段
        List<String> permissionIds = new ArrayList<>();
        if (permissions != null && permissions.size() > 0) {
            for (Permission model : permissions) {
                permissionIds.add(model.getId());
            }
        }
        // 默认权限
        List<Permission> defPermissions = getDefaultPermission(PermissionService.PERMISSION_COLUMN);
        // 构建权限
        if (columnMetas != null && columnMetas.size() > 0) {
            for (ColumnMeta column : columnMetas) {
                if (defPermissions != null && defPermissions.size() > 0) {
                    for (Permission dModel : defPermissions) {
                        Permission permission = new Permission();
                        permission.setName(dModel.getName());
                        permission.setCode(String.format("%s_%s%s", column.getTableName(), column.getName(), dModel.getCode()));
                        permission.setType(type);
                        permission.setObject(String.format("%s:%s", column.getTableName(), column.getName()));
                        permission.setRule(dModel.getRule());
                        permission.setDescription(dModel.getDescription());
                        boolean isExist = false;
                        if (permissions != null && permissions.size() > 0) {
                            for (Permission cModel : permissions) {
                                if (permission.getCode().equals(cModel.getCode()) && permission.getObject().equals(cModel.getObject())) {
                                    isExist = true;
                                    cModel.setName(permission.getName());
                                    cModel.setDescription(permission.getDescription());
                                    cModel.setRule(permission.getRule());
                                    updateModel(cModel);
                                }
                            }
                        }
                        if (!isExist) {
                            createModel(permission);
                        }
                    }
                }
            }
        }
        // 重置角色权限
        if (permissionIds != null && permissionIds.size() > 0) {
            FilterGroup filter = new FilterGroup();
            filter.addFilter("permissionId", FilterGroup.Operator.in, Strings.join(permissionIds, ','));
            filter.addFilter("tenantCode", getSessionTenantCode());
            List<RolePermissionMap> rolePermissionMaps = queryModel(RolePermissionMap.class, filter);
            if (rolePermissionMaps != null && rolePermissionMaps.size() > 0) {
                for (RolePermissionMap dModel : rolePermissionMaps) {
                    rolePermissionMapService.deleteModel(RolePermissionMap.class, dModel.getId());
                }
            }
        }
    }
}