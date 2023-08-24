package org.geelato.web.platform.m.security.service;

import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.web.platform.enums.IsDefaultOrgEnum;
import org.geelato.web.platform.m.base.service.BaseSortableService;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
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
public class OrgService extends BaseSortableService {
    @Lazy
    @Autowired
    private OrgUserMapService orgUserMapService;
    @Autowired
    private UserService userService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(Org model) {
        // 组织删除
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        // 清理 组织用户表
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", model.getId());
        List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
        if (oList != null) {
            for (OrgUserMap oModel : oList) {
                oModel.setDefaultOrg(IsDefaultOrgEnum.NO.getCode());
                oModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(oModel);
            }
        }
        // 清理 用户表
        List<User> uList = userService.queryModel(User.class, params);
        if (oList != null) {
            for (User uModel : uList) {
                uModel.setOrgId(null);
                uModel.setOrgName(null);
                dao.save(uModel);
            }
        }
    }

    /**
     * 全量查询
     *
     * @param params 条件参数
     * @return
     */
    public List<Map<String, Object>> queryTree(Map<String, Object> params) {
        return dao.queryForMapList("query_tree_platform_org", params);
    }
}