package org.geelato.web.platform.m.security.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RoleUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Map insertModel(RoleUserMap model) {
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        User uModel = userService.getModel(User.class, model.getUserId());
        Assert.notNull(uModel, ApiErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setUserName(uModel.getName());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }

    /**
     * 批量，不重复插入
     *
     * @param model
     */
    public void insertModels(RoleUserMap model) {
        String tenantCode = Strings.isNotBlank(model.getTenantCode()) ? model.getTenantCode() : getSessionTenantCode();
        // 角色存在，
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        // 用户信息，
        FilterGroup userFilter = new FilterGroup();
        userFilter.addFilter("id", FilterGroup.Operator.in, model.getUserId());
        userFilter.addFilter("tenantCode", tenantCode);
        List<User> users = userService.queryModel(User.class, userFilter);
        Assert.notNull(users, ApiErrorMsg.IS_NULL);
        // 角色用户信息，
        FilterGroup roleUserFilter = new FilterGroup();
        roleUserFilter.addFilter("roleId", model.getRoleId());
        roleUserFilter.addFilter("userId", FilterGroup.Operator.in, model.getUserId());
        roleUserFilter.addFilter("tenantCode", tenantCode);
        List<RoleUserMap> roleUserMaps = this.queryModel(RoleUserMap.class, roleUserFilter);
        // 对比插入
        List<String> existUserIds = new ArrayList<>();
        if (roleUserMaps != null && roleUserMaps.size() > 0) {
            for (RoleUserMap map : roleUserMaps) {
                if (Strings.isNotBlank(map.getUserId()) && !existUserIds.contains(map.getUserId())) {
                    existUserIds.add(map.getUserId());
                }
            }
        }
        // 构建
        for (User user : users) {
            if (!existUserIds.contains(user.getId())) {
                RoleUserMap userMap = new RoleUserMap();
                userMap.setRoleId(rModel.getId());
                userMap.setRoleName(rModel.getName());
                userMap.setUserId(user.getId());
                userMap.setUserName(user.getName());
                this.createModel(userMap);
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
        if (Strings.isBlank(userId)) {
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
        params.put("tenantCode", Strings.isNotBlank(tenantCode) ? tenantCode : getSessionTenantCode());
        List<RoleUserMap> roleUserMaps = queryModel(RoleUserMap.class, params);
        if (roleUserMaps != null && roleUserMaps.size() > 0) {
            for (RoleUserMap roleUserMap : roleUserMaps) {
                for (Role role : roleList) {
                    if (Strings.isNotBlank(roleUserMap.getRoleId()) && roleUserMap.getRoleId().equals(role.getId())) {
                        result.add(role);
                    }
                }
            }
        }

        return result;
    }
}
