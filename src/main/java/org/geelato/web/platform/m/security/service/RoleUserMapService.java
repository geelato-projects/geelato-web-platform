package org.geelato.web.platform.m.security.service;

import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.utils.StringUtils;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RoleUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class RoleUserMapService extends BaseService {
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;

    /**
     * 根据角色ID和用户ID查询角色用户映射列表
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 角色用户映射列表
     */
    public List<RoleUserMap> queryModelByIds(String roleId, String userId) {
        List<RoleUserMap> list = new ArrayList<>();
        if (StringUtils.isNotBlank(roleId) && StringUtils.isNotBlank(userId)) {
            FilterGroup filter = new FilterGroup();
            filter.addFilter("roleId", FilterGroup.Operator.in, roleId);
            filter.addFilter("userId", FilterGroup.Operator.in, userId);
            list = this.queryModel(RoleUserMap.class, filter);
        }

        return list;
    }

    /**
     * 批量，不重复插入角色用户映射关系
     *
     * @param model 角色用户映射关系对象
     * @return 插入的角色用户映射关系列表
     * @throws RuntimeException 当角色或用户信息为空时抛出异常
     */
    public List<RoleUserMap> insertModels(RoleUserMap model) {
        // 角色存在，
        List<Role> roles = roleService.getModelsById(Role.class, model.getRoleId());
        if (roles == null || roles.size() == 0) {
            throw new RuntimeException(ApiErrorMsg.IS_NULL);
        }
        // 用户信息，
        List<User> users = userService.getModelsById(User.class, model.getUserId());
        if (users == null || users.size() == 0) {
            throw new RuntimeException(ApiErrorMsg.IS_NULL);
        }
        // 角色用户信息，
        List<RoleUserMap> roleUserMaps = this.queryModelByIds(model.getRoleId(), model.getUserId());
        // 对比插入
        List<RoleUserMap> list = new ArrayList<>();
        for (Role role : roles) {
            for (User user : users) {
                boolean isExist = false;
                if (roleUserMaps != null && roleUserMaps.size() > 0) {
                    for (RoleUserMap map : roleUserMaps) {
                        if (role.getId().equals(map.getRoleId()) && user.getId().equals(map.getUserId())) {
                            isExist = true;
                            break;
                        }
                    }
                }
                if (!isExist) {
                    RoleUserMap userMap = new RoleUserMap();
                    userMap.setRoleId(role.getId());
                    userMap.setRoleName(role.getName());
                    userMap.setUserId(user.getId());
                    userMap.setUserName(user.getName());
                    userMap = this.createModel(userMap);
                    list.add(userMap);
                }
            }
        }

        return list;
    }

    public void switchModel(RoleUserMap model) {
        // 角色存在，
        List<Role> roles = roleService.getModelsById(Role.class, model.getRoleId());
        if (roles == null || roles.size() == 0) {
            throw new RuntimeException(ApiErrorMsg.IS_NULL);
        }
        // 用户信息，
        List<User> users = userService.getModelsById(User.class, model.getUserId());
        if (users == null || users.size() == 0) {
            throw new RuntimeException(ApiErrorMsg.IS_NULL);
        }
        // 角色用户信息，
        List<RoleUserMap> roleUserMaps = this.queryModelByIds(model.getRoleId(), model.getUserId());
        for (Role role : roles) {
            for (User user : users) {
                boolean isExist = false;
                if (roleUserMaps != null && roleUserMaps.size() > 0) {
                    for (RoleUserMap map : roleUserMaps) {
                        if (role.getId().equals(map.getRoleId()) && user.getId().equals(map.getUserId())) {
                            isExist = true;
                            this.isDeleteModel(map);
                        }
                    }
                }
                if (!isExist) {
                    RoleUserMap userMap = new RoleUserMap();
                    userMap.setRoleId(role.getId());
                    userMap.setRoleName(role.getName());
                    userMap.setUserId(user.getId());
                    userMap.setUserName(user.getName());
                    this.createModel(userMap);
                }
            }
        }
    }

    /**
     * 获取拥有的角色
     *
     * @param userId
     * @param appId
     * @return
     */
    public List<Role> queryRoleByUser(String userId, String appId, String tenantCode) {
        List<Role> result = new ArrayList<>();
        if (StringUtils.isBlank(userId)) {
            return result;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        List<Role> roleList = roleService.queryRoles(params);
        if (roleList == null || roleList.size() == 0) {
            return result;
        }
        params.clear();
        params.put("userId", userId);
        params.put("tenantCode", StringUtils.isNotBlank(tenantCode) ? tenantCode : getSessionTenantCode());
        List<RoleUserMap> roleUserMaps = queryModel(RoleUserMap.class, params);
        if (roleUserMaps != null && roleUserMaps.size() > 0) {
            for (RoleUserMap roleUserMap : roleUserMaps) {
                for (Role role : roleList) {
                    if (StringUtils.isNotBlank(roleUserMap.getRoleId()) && roleUserMap.getRoleId().equals(role.getId())) {
                        result.add(role);
                    }
                }
            }
        }

        return result;
    }
}
