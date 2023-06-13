package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.meta.model.entity.BaseEntity;
import org.geelato.core.orm.Dao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Service
public class BaseService {
    @Autowired
    @Qualifier("primaryDao")
    public Dao dao;

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
    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, Map<String, Object> params) {
        return dao.queryList(entity, pageNum, pageSize, params);
    }

    /**
     * 全量查询
     *
     * @param entity 查询实体
     * @param params 条件参数
     * @param <T>
     * @return
     */
    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params) {
        return dao.queryList(entity, params);
    }

    /**
     * 单条数据获取
     *
     * @param entity 查询实体
     * @param id     实体id
     * @param <T>
     * @return
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
     * @param <T>
     * @return
     */
    public <T> boolean isExist(Class<T> entity, String fieldName, Object fieldValue) {
        if (fieldValue != null) {
            Map<String, Object> params = new HashMap<>();
            params.put(fieldName, fieldValue);
            List<T> userList = dao.queryList(entity, params);
            return userList != null && !userList.isEmpty();
        }

        return false;
    }
}
