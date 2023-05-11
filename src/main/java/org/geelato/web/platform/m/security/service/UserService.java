package org.geelato.web.platform.m.security.service;

import org.geelato.core.meta.model.entity.BaseSortableEntity;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.security.entity.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class UserService {
    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    public <T> List<T> pageQueryModel(Class<T> entity, int pageNum, int pageSize, Map<String, Object> params) {
        return dao.queryList(entity, pageNum, pageSize, params);
    }

    public <T> List<T> queryModel(Class<T> entity, Map<String, Object> params) {
        return dao.queryList(entity, params);
    }

    public <T> T getModel(Class<T> entity, long id) {
        return dao.queryForObject(entity, id);
    }

    public <T extends BaseSortableEntity> Map createModel(T model) {
        model.setSeqNo(model.getSeqNo() > 0 ? model.getSeqNo() : Constants.SEQ_NO_DEFAULT);
        model.setDelStatus(0);
        return dao.save(model);
    }

    public <T extends BaseSortableEntity> Map updateModel(T model) {
        return dao.save(model);
    }

    public void deleteModel(Class entity, long id) {
        dao.delete(entity, "id", id);
    }

    public boolean isExist(Class entity, long id) {
        if (id > 0) {
            return dao.queryForObject(entity, id) != null;
        }
        return false;
    }

    public <T> boolean isExist(Class<T> entity, String fieldName, long id) {
        if (id > 0) {
            Map<String, Object> params = new HashMap<>();
            params.put(fieldName, id);
            List<T> userList = dao.queryList(entity, params);
            return userList != null && !userList.isEmpty();
        }

        return false;
    }
}
