package org.geelato.web.platform.m.security.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.web.platform.enums.IsDefaultOrgEnum;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
import org.geelato.web.platform.m.security.entity.RoleUserMap;
import org.geelato.web.platform.m.security.entity.User;
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
public class UserService extends BaseSortableService {
    @Lazy
    @Autowired
    private OrgUserMapService orgUserMapService;
    @Lazy
    @Autowired
    private RoleUserMapService roleUserMapService;

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
}