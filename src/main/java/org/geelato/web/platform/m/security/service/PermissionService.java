package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ResourcesFiles;
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
import java.util.*;

/**
 * @author diabl
 */
@Component
public class PermissionService extends BaseService {
    public static final String PERMISSION_COLUMN = ResourcesFiles.PERMISSION_COLUMN_DEFAULT_JSON;
    public static final String PERMISSION_TABLE = ResourcesFiles.PERMISSION_TABLE_DEFAULT_JSON;
    private static final String[] PERMISSION_TO_ROLE = {"&myself", "&insert", "&update", "&delete"};
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
        super.isDeleteModel(model);
        // 角色权限关系表
        Map<String, Object> params = new HashMap<>();
        params.put("permissionId", model.getId());
        List<RolePermissionMap> rList = rolePermissionMapService.queryModel(RolePermissionMap.class, params);
        if (rList != null) {
            for (RolePermissionMap oModel : rList) {
                rolePermissionMapService.isDeleteModel(oModel);
            }
        }
    }

    public boolean isDefault(Permission model) {
        boolean isDef = false;
        if (Strings.isBlank(model.getCode()) || Strings.isBlank(model.getType()) || Strings.isBlank(model.getObject())) {
            return isDef;
        }
        List<Permission> defaultPermissions = new ArrayList<>();
        if (PermissionTypeEnum.MODEL.getValue().equals(model.getType())) {
            defaultPermissions = getDefaultPermission(PermissionService.PERMISSION_TABLE);
        } else if (PermissionTypeEnum.COLUMN.getValue().equals(model.getType())) {
            defaultPermissions = getDefaultPermission(PermissionService.PERMISSION_COLUMN);
        }
        if (defaultPermissions != null && defaultPermissions.size() > 0) {
            for (Permission permission : defaultPermissions) {
                if (model.getCode().equals(String.format("%s%s", model.getObject(), permission.getCode()))) {
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

    /**
     * @param curObject 新的
     * @param sorObject 旧的
     */
    public void tablePermissionChangeObject(String curObject, String sorObject) {
        List<Permission> permissions = new ArrayList<>();
        // 表格权限
        Map<String, Object> params = new HashMap<>();
        params.put("type", PermissionTypeEnum.MODEL.getValue());
        params.put("object", sorObject);
        params.put("tenantCode", getSessionTenantCode());
        List<Permission> tPermissions = queryModel(Permission.class, params);
        // 修改 object
        if (tPermissions != null && tPermissions.size() > 0) {
            for (Permission permission : tPermissions) {
                // tableName&XX
                if (permission.getCode().startsWith(sorObject + "&")) {
                    permission.setCode(permission.getCode().replace(sorObject + "&", curObject + "&"));
                }
                // tableName
                permission.setObject(curObject);
                updateModel(permission);
            }
        }
        // 字段权限
        FilterGroup filter = new FilterGroup();
        filter.addFilter("type", PermissionTypeEnum.COLUMN.getValue());
        filter.addFilter("object", FilterGroup.Operator.startWith, String.format("%s:", sorObject));
        filter.addFilter("tenantCode", getSessionTenantCode());
        List<Permission> cPermissions = queryModel(Permission.class, filter);
        // 修改 object
        if (cPermissions != null && cPermissions.size() > 0) {
            for (Permission permission : cPermissions) {
                // tableName:columnName&XX
                if (permission.getCode().startsWith(sorObject + ":")) {
                    permission.setCode(permission.getCode().replace(sorObject + ":", curObject + ":"));
                }
                // tableName:columnName
                if (permission.getObject().startsWith(sorObject + ":")) {
                    permission.setObject(permission.getObject().replace(sorObject + ":", curObject + ":"));
                }
                updateModel(permission);
            }
        }
    }

    /**
     * @param tableName 表格
     * @param curObject 新字段
     * @param sorObject 旧字段
     */
    public void columnPermissionChangeObject(String tableName, String curObject, String sorObject) {
        // 字段权限
        Map<String, Object> params = new HashMap<>();
        params.put("type", PermissionTypeEnum.COLUMN.getValue());
        params.put("object", String.format("%s:%s", tableName, sorObject));
        params.put("tenantCode", getSessionTenantCode());
        List<Permission> permissions = queryModel(Permission.class, params);
        // 修改 object
        if (permissions != null && permissions.size() > 0) {
            for (Permission permission : permissions) {
                String object = String.format("%s:%s", tableName, curObject);
                // tableName:columnName&XX
                if (permission.getCode().startsWith(permission.getObject())) {
                    permission.setCode(permission.getCode().replace(permission.getObject() + "&", object + "&"));
                }
                // tableName:columnName
                permission.setObject(object);
                updateModel(permission);
            }
        }
    }

    public void resetDefaultPermission(String type, String object) {
        if (PermissionTypeEnum.MODEL.getValue().equals(type)) {
            resetTableDefaultPermission(type, object);
        } else if (PermissionTypeEnum.COLUMN.getValue().equals(type)) {
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
                        createDefaultPermission(object, dModel);
                    }
                }
            } else {
                for (Permission dModel : defPermissions) {
                    createDefaultPermission(object, dModel);
                }
            }
        }
    }

    private void createDefaultPermission(String object, Permission dModel) {
        String defaultCode = dModel.getCode();
        dModel.setObject(object);
        dModel.setCode(String.format("%s%s", object, defaultCode));
        Map<String, Object> permission = createModel(dModel);
        if (Arrays.asList(PERMISSION_TO_ROLE).contains(defaultCode)) {
            rolePermissionMapService.createAllRoleOfDefaultPermission(JSON.parseObject(JSON.toJSONString(permission), Permission.class));
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
                        permission.setCode(String.format("%s:%s%s", column.getTableName(), column.getName(), dModel.getCode()));
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
                    rolePermissionMapService.isDeleteModel(dModel);
                }
            }
        }
    }
}