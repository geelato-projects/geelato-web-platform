package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.gql.parser.PageQueryRequest;
import org.geelato.web.platform.enums.IsDefaultOrgEnum;
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
public class UserService extends BaseSortableService {
    @Lazy
    @Autowired
    private OrgUserMapService orgUserMapService;
    @Lazy
    @Autowired
    private RoleUserMapService roleUserMapService;
    @Lazy
    @Autowired
    private RoleService roleService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(User model) {
        // 用户删除
        model.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
        super.isDeleteModel(model);
        // 清理 组织用户表
        Map<String, Object> params = new HashMap<>();
        params.put("userId", model.getId());
        List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
        if (oList != null) {
            for (OrgUserMap oModel : oList) {
                oModel.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
                orgUserMapService.isDeleteOrgUserMap(oModel);
            }
        }
        // 角色用户关系表
        List<RoleUserMap> rList = roleUserMapService.queryModel(RoleUserMap.class, params);
        if (rList != null) {
            for (RoleUserMap rModel : rList) {
                roleUserMapService.isDeleteModel(rModel);
            }
        }
    }

    public void setDefaultOrg(User model) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", model.getId());
        boolean isExit = false;
        List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
        if (oList != null) {
            for (OrgUserMap oModel : oList) {
                if (oModel.getOrgId() != null && oModel.getOrgId().equals(model.getOrgId())) {
                    isExit = true;
                    if (IsDefaultOrgEnum.IS.getCode() != oModel.getDefaultOrg()) {
                        oModel.setDefaultOrg(IsDefaultOrgEnum.IS.getCode());
                        orgUserMapService.updateModel(oModel);
                    }
                } else if (IsDefaultOrgEnum.IS.getCode() == oModel.getDefaultOrg()) {
                    oModel.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
                    orgUserMapService.updateModel(oModel);
                }
            }
        }
        String orgId = String.valueOf(model.getOrgId());
        if (!isExit && Strings.isNotBlank(orgId)) {
            OrgUserMap oModel = new OrgUserMap();
            oModel.setUserId(model.getId());
            oModel.setUserName(model.getName());
            oModel.setOrgId(orgId);
            oModel.setOrgName(model.getOrgName());
            oModel.setDefaultOrg(IsDefaultOrgEnum.IS.getCode());
            orgUserMapService.createModel(oModel);
        }
    }


    public ApiPagedResult pageQueryModelOf(FilterGroup filter, PageQueryRequest request, String appId, String tenantCode) {
        ApiPagedResult result = new ApiPagedResult();
        Map<String, Object> resultMap = new HashMap<>();
        // 用户查询
        dao.setDefaultFilter(true, filterGroup);
        List<User> pageQueryList = dao.pageQueryList(User.class, filter, request);
        List<User> queryList = dao.queryList(User.class, filter, request.getOrderBy());
        // 分页结果
        result.setPage(request.getPageNum());
        result.setSize(request.getPageSize());
        result.setTotal(queryList != null ? queryList.size() : 0);
        result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        result.setData(new DataItems(pageQueryList, result.getTotal()));
        List<String> userIds = new ArrayList<>();
        if (pageQueryList != null && pageQueryList.size() > 0) {
            for (User model : pageQueryList) {
                model.setSalt(null);
                model.setPassword(null);
                model.setPlainPassword(null);
                if (!userIds.contains(model.getId())) {
                    userIds.add(model.getId());
                }
            }
        } else {
            return result;
        }
        // 角色查询
        Map<String, Object> params = new HashMap<>();
        params.put("appId", appId);
        params.put("tenantCode", tenantCode);
        List<Role> queryRoles = roleService.queryRoles(params);
        resultMap.put("role", queryRoles);
        List<String> roleIds = new ArrayList<>();
        if (pageQueryList != null && pageQueryList.size() > 0) {
            for (Role model : queryRoles) {
                if (!roleIds.contains(model.getId())) {
                    roleIds.add(model.getId());
                }
            }
        } else {
            return result;
        }
        // 角色用户查询
        List<RoleUserMap> roleUserMaps = roleUserMapService.queryModelByIds(String.join(",", roleIds), String.join(",", userIds));
        List<Map<String, Object>> tableList = new ArrayList<>();
        for (User user : pageQueryList) {
            Map<String, Object> tableParams = JSON.parseObject(JSON.toJSONString(user), Map.class);
            for (Role role : queryRoles) {
                tableParams.put(role.getId(), false);
                if (roleUserMaps != null && roleUserMaps.size() > 0) {
                    for (RoleUserMap map : roleUserMaps) {
                        if (role.getId().equals(map.getRoleId()) && user.getId().equals(map.getUserId())) {
                            tableParams.put(role.getId(), true);
                            break;
                        }
                    }
                }
            }
            tableList.add(tableParams);
        }
        resultMap.put("table", tableList);

        result.setData(new DataItems(resultMap, result.getTotal()));
        return result;
    }
}