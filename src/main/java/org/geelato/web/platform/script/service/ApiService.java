package org.geelato.web.platform.script.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.script.entity.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author diabl
 */
@Component
public class ApiService extends BaseService {

    @Lazy
    @Autowired
    private ApiParamService apiParamService;

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Api createModel(Api model) {
        // 创建api
        Api api = super.createModel(model);
        // 创建apiParam
        api.setParameters(model.getParameters());
        api.setParameters(apiParamService.batchHandleModelByApis(api));

        return api;
    }

    /**
     * 更新一条数据
     *
     * @param model 实体数据
     * @return
     */
    public Api updateModel(Api model) {
        // 更新api
        Api api = super.updateModel(model);
        // 更新apiParam
        api.setParameters(model.getParameters());
        api.setParameters(apiParamService.batchHandleModelByApis(api));

        return api;
    }
}
