package org.geelato.web.platform.graal.service;

import org.geelato.core.ds.DataSourceManager;
import org.geelato.core.graal.GraalService;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@GraalService(name="dao",type="simple")
public class MetaOperator {
    public Object list(String gql){
        RuleService ruleService=new RuleService();
        JdbcTemplate jdbcTemplate=new JdbcTemplate();
        jdbcTemplate.setDataSource((DataSource) DataSourceManager.singleInstance().getDymanicDataSourceMap().get("primary"));
        ruleService.setDao(new Dao(jdbcTemplate));
        return ruleService.queryForMapList(gql, false);
    }
}
