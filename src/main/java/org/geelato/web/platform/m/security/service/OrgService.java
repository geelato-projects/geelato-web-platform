package org.geelato.web.platform.m.security.service;

import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.security.entity.Org;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Component
public class OrgService {
    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    public ApiPagedResult pageQueryOrg() {
        return null;
    }

    public List<Org> queryOrg(Map<String, Object> params) {
        return dao.queryList(Org.class, params);
    }

    public Org getOrg(long id) {
        return dao.queryForObject(Org.class, id);
    }

    public Map createOrg(Org org) {
        return dao.save(org);
    }

    public Map updateOrg(Org org) {
        return dao.save(org);
    }

    public void deleteOrg(long id) {
        dao.delete(Org.class, "id", id);
    }

    public boolean isExistOrg(long id) {
        if (id > 0) {
            Org org = getOrg(id);
            return org != null;
        }
        return false;
    }
}
