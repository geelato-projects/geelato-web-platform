package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Permission;
import org.geelato.web.platform.m.security.entity.RolePermissionMap;
import org.geelato.web.platform.m.security.enums.DeleteStatusEnum;
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
public class PermissionService extends BaseService {
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
}
