package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.enums.DeleteStatusEnum;
import org.geelato.web.platform.m.security.enums.IsDefaultOrgEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class OrgUserMapService extends BaseService {
    @Autowired
    private UserService userService;
    @Autowired
    private OrgService orgService;

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Map createModel(OrgUserMap model) {
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        // 清理用户表单
        updateUserDefaultOrg(model);

        return dao.save(model);
    }

    /**
     * 更新一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Map updateModel(OrgUserMap model) {
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        // 清理用户表单
        updateUserDefaultOrg(model);

        return dao.save(model);
    }

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(OrgUserMap model) {
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 清理 用户默认部门
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", model.getOrgId());
        params.put("id", model.getUserId());
        List<User> uList = userService.queryModel(User.class, params);
        if (uList != null) {
            for (User uModel : uList) {
                uModel.setOrgId(0);
                uModel.setOrgName(null);
                dao.save(uModel);
            }
        }
    }

    private void updateUserDefaultOrg(OrgUserMap model) {
        // 用户
        User uModel = userService.getModel(User.class, model.getUserId());
        Assert.isNull(uModel, ErrorMsg.IS_NULL);
        // 组织
        Org oModel = orgService.getModel(Org.class, model.getOrgId());
        Assert.isNull(oModel, ErrorMsg.IS_NULL);
        // 当前默认；原来默认
        if (model.getDefaultOrg() == IsDefaultOrgEnum.IS.getCode()) {
            uModel.setOrgId(oModel.getId());
            uModel.setOrgName(oModel.getName());
            dao.save(uModel);
        } else if (oModel.getId().equals(uModel.getOrgId())) {
            uModel.setOrgId(0);
            uModel.setOrgName(null);
            dao.save(uModel);
        }
    }
}
