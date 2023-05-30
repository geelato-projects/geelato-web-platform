package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.Permission;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RolePermissionMap;
import org.geelato.web.platform.m.security.enums.DeleteStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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
        Assert.notNull(rModel, ErrorMsg.IS_NULL);
        Permission pModel = permissionService.getModel(Permission.class, model.getPermissionId());
        Assert.notNull(pModel, ErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setPermissionName(pModel.getName());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        return dao.save(model);
    }
}
