package org.geelato.web.platform.m.security.service;

import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.security.entity.User;
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

    public ApiPagedResult pageQueryUser() {
        return null;
    }

    public List<User> queryUser(Map<String, Object> params) {
        return dao.queryList(User.class, params);
    }

    public User getUser(long id) {
        return this.dao.queryForObject(User.class, id);
    }

    public Map createUser(User user) {
        return dao.save(user);
    }

    public Map updateUser(User user) {
        return dao.save(user);
    }

    public void deleteUser(long id) {
        dao.delete(User.class, "id", id);
    }

    public boolean isExistUser(long id) {
        if (id > 0) {
            Map<String, Object> params = new HashMap<>();
            params.put("orgId", id);
            List<User> userList = queryUser(params);
            return userList != null && !userList.isEmpty();
        }

        return false;
    }
}
