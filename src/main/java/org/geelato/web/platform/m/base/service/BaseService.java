package org.geelato.web.platform.m.base.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.Ctx;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.BaseEntity;
import org.geelato.core.orm.Dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author diabl
 */
@Service
public class BaseService {
    private static final String DEFAULT_ORDER_BY = "update_at DESC";
    @Autowired
    @Qualifier("primaryDao")
    public Dao dao;

    public FilterGroup filterGroup = new FilterGroup().addFilter(ColumnDefault.DEL_STATUS_FIELD, String.valueOf(DeleteStatusEnum.NO.getCode()));

    /**
     * 分页查询
     *
     * @param entity   查询实体
     * @param pageNum  页码
     * @param pageSize 分页数量
     * @param params   条件参数
     * @param <T>
     * @return
     */
    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, String orderBy, Map<String, Object> params) {
        dao.setDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, params, pageNum, pageSize, orderBy);
    }

    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, Map<String, Object> params) {
        return this.pageQueryModel(entity, pageNum, pageSize, BaseService.DEFAULT_ORDER_BY, params);
    }

    /**
     * 分页查询
     *
     * @param entity   查询实体
     * @param pageNum  页码
     * @param pageSize 分页数量
     * @param filter   条件参数
     * @param <T>
     * @return
     */
    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, String orderBy, FilterGroup filter) {
        dao.setDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, filter, pageNum, pageSize, orderBy);
    }

    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, FilterGroup filter) {
        return this.pageQueryModel(entity, pageNum, pageSize, BaseService.DEFAULT_ORDER_BY, filter);
    }

    /**
     * 全量查询
     *
     * @param entity 查询实体
     * @param params 条件参数
     */
    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params, String orderBy) {
        dao.setDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, params, orderBy);
    }

    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params) {
        return queryModel(entity, params, BaseService.DEFAULT_ORDER_BY);
    }

    /**
     * 全量查询
     *
     * @param entity 查询实体
     * @param filter 条件参数
     */
    public <T> List<T> queryModel(Class<T> entity, FilterGroup filter, String orderBy) {
        dao.setDefaultFilter(true, filterGroup);
        orderBy = Strings.isNotBlank(orderBy) ? orderBy : BaseService.DEFAULT_ORDER_BY;
        return dao.queryList(entity, filter, orderBy);
    }

    public <T> List<T> queryModel(Class<T> entity, FilterGroup filter) {
        dao.setDefaultFilter(true, filterGroup);
        return queryModel(entity, filter, BaseService.DEFAULT_ORDER_BY);
    }

    /**
     * 单条数据获取
     *
     * @param entity 查询实体
     * @param id     实体id
     */
    public <T> T getModel(Class<T> entity, String id) {
        return dao.queryForObject(entity, id);
    }

    /**
     * 创建一条数据
     *
     * @param model 实体数据
     * @param <T>
     * @return
     */
    public <T extends BaseEntity> Map createModel(T model) {
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        if (Strings.isBlank(model.getTenantCode())) {
            model.setTenantCode(getSessionTenantCode());
        }
        return dao.save(model);
    }

    /**
     * 更新一条数据
     *
     * @param model 实体数据
     * @param <T>
     * @return
     */
    public <T extends BaseEntity> Map updateModel(T model) {
        model.setDelStatus(DeleteStatusEnum.NO.getCode());
        return dao.save(model);
    }

    /**
     * 逻辑删除
     *
     * @param model
     * @param <T>
     */
    public <T extends BaseEntity> void isDeleteModel(T model) {
        model.setDelStatus(DeleteStatusEnum.IS.getCode());
        model.setDeleteAt(new Date());
        dao.save(model);
    }

    /**
     * 删除一条数据
     *
     * @param entity 实体
     * @param id     实体id
     */
    public void deleteModel(Class entity, String id) {
        dao.delete(entity, "id", id);
    }

    /**
     * 是否存在数据
     *
     * @param entity 查询实体
     * @param id     实体id
     * @return
     */
    public boolean isExist(Class entity, String id) {
        if (Strings.isNotBlank(id)) {
            return dao.queryForObject(entity, id) != null;
        }
        return false;
    }

    /**
     * 是否存在数据
     *
     * @param entity     查询实体
     * @param fieldName  关联实体字段名称
     * @param fieldValue 关联实体字段值
     */
    public <T> boolean isExist(Class<T> entity, String fieldName, Object fieldValue) {
        if (fieldValue != null) {
            Map<String, Object> params = new HashMap<>();
            params.put(fieldName, fieldValue);
            List<T> userList = dao.queryList(entity, params, null);
            return userList != null && !userList.isEmpty();
        }

        return false;
    }


    public boolean validate(String tableName, String id, Map<String, String> params) {
        Map<String, Object> map = new HashMap<>();
        // 租户编码
        if (Strings.isBlank(params.get("tenant_code"))) {
            params.put("tenant_code", getSessionTenantCode());
        }
        // 查询表格
        if (Strings.isBlank(tableName)) {
            return false;
        }
        map.put("tableName", tableName);
        // 排除本身
        map.put("id", Strings.isNotBlank(id) ? id : null);
        // 条件限制
        List<JSONObject> list = new ArrayList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            Map<String, String> jParams = new HashMap<>();
            if (Strings.isNotBlank(param.getKey())) {
                jParams.put("key", param.getKey());
                jParams.put("value", param.getValue());
                list.add(JSONObject.parseObject(JSON.toJSONString(jParams)));
            }
        }
        map.put("condition", list);
        List<Map<String, Object>> vlist = dao.queryForMapList("platform_validate", map);
        return vlist.isEmpty();
    }

    /**
     * @return 当前会话信息
     */
    protected String getSessionTenantCode() {
        return Ctx.getCurrentTenantCode();
    }
}
