package org.geelato.web.platform.m.security.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.enums.DeleteStatusEnum;
import org.geelato.web.platform.m.security.enums.IsDefaultOrgEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class UserService extends BaseService {
    @Autowired
    private OrgUserMapService orgUserMapService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(User model) {
        // 用户删除
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
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
    }
}