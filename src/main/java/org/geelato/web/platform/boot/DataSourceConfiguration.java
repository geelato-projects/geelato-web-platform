package org.geelato.web.platform.boot;

import com.zaxxer.hikari.HikariDataSource;
import org.geelato.core.ds.DataSourceManager;
import org.geelato.core.orm.Dao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import javax.xml.crypto.Data;
import java.util.HashMap;
import java.util.Map;


/**
 * @author geelato
 */
@Configuration
public class DataSourceConfiguration extends BaseConfiguration {


    @Bean(name = "primaryDataSource")
    @Qualifier("primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    @Bean(name = "primaryJdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    @Bean(name = "primaryDao")
    public Dao primaryDao(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new Dao(jdbcTemplate);
    }
    @Bean(name = "secondaryDataSource")
    @Qualifier("secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
       return DataSourceBuilder.create().build();
    }
    @Bean(name = "secondaryJdbcTemplate")
    public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    @Bean(name = "secondaryDao")
    public Dao secondaryDao(@Qualifier("secondaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new Dao(jdbcTemplate);
    }

    @Bean(name = "dynamicDataSource")
    @Qualifier("dynamicDataSource")
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicDatasource=new DynamicDataSource();
        Map<Object, Object> dymanicDataSourceMap=DataSourceManager.singleInstance().getDymanicDataSourceMap();
        dymanicDataSourceMap.put("primary",primaryDataSource());
        dynamicDatasource.setTargetDataSources(dymanicDataSourceMap);
        dynamicDatasource.setDefaultTargetDataSource(primaryDataSource());
        return dynamicDatasource;
    }

    @Bean(name = "dynamicJdbcTemplate")
    public JdbcTemplate dynamicJdbcTemplate(@Qualifier("dynamicDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "dynamicDao")
    public Dao dynamicDao(@Qualifier("dynamicJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new Dao(jdbcTemplate);
    }
}
