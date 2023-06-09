package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.geelato.web.platform.m.security.entity.*;
import org.geelato.core.enums.DeleteStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

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

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(Role model) {
        // 组织删除
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 角色APP关系表
        Map<String, Object> params = new HashMap<>();
        params.put("roleId", model.getId());
        List<RoleAppMap> aList = roleAppMapService.queryModel(RoleAppMap.class, params);
        if (aList != null) {
            for (RoleAppMap rModel : aList) {
                rModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(rModel);
            }
        }
        // 角色权限关系表
        List<RolePermissionMap> pList = rolePermissionMapService.queryModel(RolePermissionMap.class, params);
        if (aList != null) {
            for (RolePermissionMap rModel : pList) {
                rModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(rModel);
            }
        }
        // 角色菜单关系表
        List<RoleTreeNodeMap> tList = roleTreeNodeMapService.queryModel(RoleTreeNodeMap.class, params);
        if (aList != null) {
            for (RoleTreeNodeMap rModel : tList) {
                rModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(rModel);
            }
        }
        // 角色用户关系表
        List<RoleUserMap> uList = roleUserMapService.queryModel(RoleUserMap.class, params);
        if (uList != null) {
            for (RoleUserMap rModel : uList) {
                rModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(rModel);
            }
        }
    }
}
