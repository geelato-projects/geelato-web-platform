package org.geelato.web.platform.m.base.service;

import org.geelato.web.platform.m.base.entity.App;
import org.geelato.web.platform.m.security.entity.RoleAppMap;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.web.platform.m.security.service.RoleAppMapService;
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
public class AppService extends BaseSortableService {
    @Lazy
    @Autowired
    private RoleAppMapService roleAppMapService;

    /**
     * 逻辑删除
     *
     * @param model
     */
    public void isDeleteModel(App model) {
        // 用户删除
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        dao.save(model);
        //
        Map<String, Object> params = new HashMap<>();
        params.put("appId", model.getId());
        List<RoleAppMap> rList = roleAppMapService.queryModel(RoleAppMap.class, params);
        if (rList != null) {
            for (RoleAppMap oModel : rList) {
                oModel.setDelStatus(DeleteStatusEnum.IS.getCode());
                dao.save(oModel);
            }
        }
    }
}
