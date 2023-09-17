package org.geelato.web.platform.m.security.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.web.platform.m.base.entity.TreeNode;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.base.service.TreeNodeService;
import org.geelato.web.platform.m.security.entity.Role;
import org.geelato.web.platform.m.security.entity.RoleTreeNodeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author diabl
 */
@Component
public class RoleTreeNodeMapService extends BaseService {
    @Autowired
    private RoleService roleService;
    @Autowired
    private TreeNodeService treeNodeService;

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Map insertModel(RoleTreeNodeMap model) {
        Role rModel = roleService.getModel(Role.class, model.getRoleId());
        Assert.notNull(rModel, ApiErrorMsg.IS_NULL);
        TreeNode tModel = treeNodeService.getModel(TreeNode.class, model.getTreeNodeId());
        Assert.notNull(tModel, ApiErrorMsg.IS_NULL);
        // 构建
        model.setId(null);
        model.setRoleName(rModel.getName());
        model.setTreeNodeText(tModel.getText());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }
}
