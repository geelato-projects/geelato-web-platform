package org.geelato.web.platform.graal.service;

import org.geelato.core.ds.DataSourceManager;
import org.geelato.core.graal.GraalService;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@GraalService(name = "dao", built = "true")
public class GqlService extends RuleService {
    public GqlService() {
        setDao(initDefaultDao());
    }

    private Dao initDefaultDao() {
        DataSource ds = (DataSource) DataSourceManager.singleInstance().getDynamicDataSourceMap().get("primary");
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(ds);
        return new org.geelato.core.orm.Dao(jdbcTemplate);
    }
}
