package org.geelato.web.platform.m.security.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.web.platform.enums.IsDefaultOrgEnum;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
import org.geelato.web.platform.m.security.entity.User;
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
    public Map insertModel(OrgUserMap model) {
        User uModel = userService.getModel(User.class, model.getUserId());
        Assert.notNull(uModel, ApiErrorMsg.IS_NULL);
        Org oModel = orgService.getModel(Org.class, model.getOrgId());
        Assert.notNull(oModel, ApiErrorMsg.IS_NULL);
        // 清理用户表单
        // 当前默认；原来默认
        if (model.getDefaultOrg() == IsDefaultOrgEnum.IS.getCode()) {
            uModel.setOrgId(oModel.getId());
            uModel.setOrgName(oModel.getName());
            dao.save(uModel);
        } else if (oModel.getId().equals(uModel.getOrgId())) {
            uModel.setOrgId(null);
            uModel.setOrgName(null);
            dao.save(uModel);
        }
        // 构建
        model.setId(null);
        model.setUserName(uModel.getName());
        model.setOrgName(oModel.getName());
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(OrgUserMap model) {
        isDeleteOrgUserMap(model);
        // 清理 用户默认部门
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", model.getOrgId());
        params.put("id", model.getUserId());
        List<User> uList = userService.queryModel(User.class, params);
        if (uList != null) {
            for (User uModel : uList) {
                uModel.setOrgId(null);
                uModel.setOrgName(null);
                dao.save(uModel);
            }
        }
    }

    /**
     * 基础逻辑删除
     *
     * @param model
     */
    public void isDeleteOrgUserMap(OrgUserMap model) {
        model.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
        super.isDeleteModel(model);
    }
}
