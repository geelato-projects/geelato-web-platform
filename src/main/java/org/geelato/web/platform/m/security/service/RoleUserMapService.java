package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RoleUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.enums.DeleteStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

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
        Assert.isNull(rModel, ErrorMsg.IS_NULL);
        User uModel = userService.getModel(User.class, model.getUserId());
        Assert.isNull(uModel, ErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setUserName(uModel.getName());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        return dao.save(model);
    }
}
