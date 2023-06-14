package org.geelato.web.platform.m.base.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ViewService extends  BaseService  {

    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, Map<String, Object> params) {

        //TODO 获取权限拼接
        return dao.queryList(entity, pageNum, pageSize, params);
    }


    private Map<String,Object> permissionFilterRule(){
        Map<String, Object> filterRuleParams=new HashMap<>();

        filterRuleParams.put("creator","");
        filterRuleParams.put("org","");

        return filterRuleParams;
    }
}
