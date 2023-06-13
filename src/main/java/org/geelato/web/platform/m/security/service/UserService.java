package org.geelato.web.platform.m.security.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.enums.DeleteStatusEnum;
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
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 清理 组织用户表
        Map<String, Object> params = new HashMap<>();
        params.put("userId", model.getId());
        List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
        if (oList != null) {
            for (OrgUserMap oModel : oList) {
                oModel.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
                oModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(oModel);
            }
        }
        // 角色用户关系表
        List<RoleUserMap> rList = roleUserMapService.queryModel(RoleUserMap.class, params);
        if (rList != null) {
            for (RoleUserMap rModel : rList) {
                rModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(rModel);
            }
        }
    }

    public void setDefaultOrg(Map<String, Object> model) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", model.get("id"));
        boolean isExit = false;
        List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
        if (oList != null) {
            for (OrgUserMap oModel : oList) {
                if (oModel.getOrgId() != null && oModel.getOrgId().equals(model.get("orgId"))) {
                    isExit = true;
                    if (IsDefaultOrgEnum.IS.getCode() != oModel.getDefaultOrg()) {
                        oModel.setDefaultOrg(IsDefaultOrgEnum.IS.getCode());
                        dao.save(oModel);
                    }
                } else if (IsDefaultOrgEnum.IS.getCode() == oModel.getDefaultOrg()) {
                    oModel.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
                    dao.save(oModel);
                }
            }
        }
        String orgId = String.valueOf(model.get("orgId"));
        if (!isExit && Strings.isNotBlank(orgId)) {
            OrgUserMap oModel = new OrgUserMap();
            oModel.setUserId((String) model.get("id"));
            oModel.setUserName((String) model.get("name"));
            oModel.setOrgId(orgId);
            oModel.setOrgName((String) model.get("orgName"));
            oModel.setDelStatus(DeleteStatusEnum.NO.getCode());
            oModel.setDefaultOrg(IsDefaultOrgEnum.IS.getCode());
            dao.save(oModel);
        }
    }
}