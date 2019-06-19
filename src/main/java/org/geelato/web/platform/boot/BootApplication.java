package org.geelato.web.platform.boot;

import org.geelato.core.biz.rules.BizManagerFactory;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.MetaRelf;
import org.geelato.core.orm.Dao;
import org.geelato.core.orm.DbGenerateDao;
import org.geelato.core.orm.SqlFiles;
import org.geelato.core.script.sql.SqlScriptManagerFactory;
import org.geelato.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// 在繼承的类中编写该注解
//@SpringBootApplication
@ComponentScan(basePackages = {"org.geelato"})
public class BootApplication implements CommandLineRunner, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(BootApplication.class);
    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;

    @Autowired
    protected DbGenerateDao dbGenerateDao;

    protected boolean isWinOS;

    // main 接受的参数
    protected String MAIN_ARGS_RESET_DB = "reset_db";


    private void assertOS() {
        Properties prop = System.getProperties();
        String os = prop.getProperty("os.name");
        logger.info("[操作系统]" + os);
        if (os.toLowerCase().startsWith("win"))
            isWinOS = true;
        else
            isWinOS = false;
    }

    /**
     * @param args 每一个参数
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        logger.info("[启动参数]：" + StringUtils.join(args, ","));
        logger.info("[配置文件]：" + applicationContext.getEnvironment().getProperty("geelato.env"));
        logger.info("[启动应用]...ing");
        assertOS();
        initMeta(args);
        logger.info("[启动应用]...OK");
    }

    public void initMeta(String... args) throws IOException {
        // 解析元数据
        MetaRelf.setApplicationContext(applicationContext);
        String[] packageNames = getProperty("geelato.meta.scan-package-names", "org.geelato").split(",");
        for (String packageName : packageNames) {
            MetaManager.singleInstance().scanAndParse(packageName, false);
        }
        // 解析脚本：sql、业务规则
        if (this.getClass().getClassLoader() == null || this.getClass().getClassLoader().getResource("//") == null) {
            initFromFatJar(args);
        } else {
            initFromExploreFile(args);
        }
    }

    /**
     * 配置文件不打包在jar包中运行，可基于文件系统加载配置文件
     *
     * @throws IOException
     */
    protected void initFromExploreFile(String... args) throws IOException {
        //String path =applicationContext.getEnvironment().getProperty("geelato.res.path").trim();
        String path = this.getClass().getClassLoader().getResource("//").getPath();
        //由测试类启动时，修改资源目录为源码下的资源目录
        path = path.replace("test-classes", "classes");
        //--1、sql
        SqlScriptManagerFactory.get(Dao.SQL_TEMPLATE_MANAGER).loadFiles(path + "/geelato/web/platform/sql/");
        //--2、业务规则
        BizManagerFactory.getBizRuleScriptManager("rule").setDao(dao);
        BizManagerFactory.getBizRuleScriptManager("rule").loadFiles(path + "/geelato/web/platform/rule/");
        if (isNeedResetDb(args)) {
            logger.info("收到重置数据库命令，开始创建表结构、初始化表数据。");
            //--3、创建表结构
            dbGenerateDao.createAllTables(true);
            //--4、初始化表数据
            String filePaths = getProperty("geelato.init.sql", "/geelato/web/platform/data/init.sql");
            String[] sqlFiles = filePaths.split(",");
            for (String sqlFile : sqlFiles) {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(sqlFile);
                if (is == null) {
                    // jar:file:/data/geelato-web-quickstart-1.0.0-SNAPSHOT.jar!/BOOT-INF/lib/geelato-web-platform-1.0.0-SNAPSHOT.jar!/geelato.web.platform/data/init.sql
                }
                SqlFiles.loadAndExecute(is, dao.getJdbcTemplate(), isWinOS);
            }
        } else {
            logger.info("未收到重置数据库命令，跳过创建表结构、跳过初始化表数据。");
        }
        BizManagerFactory.getBizMvelRuleManager("mvelRule").setDao(dao);
        BizManagerFactory.getBizMvelRuleManager("mvelRule").loadDb(null);
    }

    protected boolean isNeedResetDb(String... args) {
        for (String arg : args) {
            if (this.MAIN_ARGS_RESET_DB.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打包成单个fatJar文件运行时，加载的资源不能采用文件系统加载，需采用流的方式加载
     *
     * @throws IOException
     */
    protected void initFromFatJar(String... args) throws IOException {
        //--1、sql
        SqlScriptManagerFactory.get(Dao.SQL_TEMPLATE_MANAGER).loadResource("/geelato/web/platform/sql/**/*.sql");
        //--2、业务规则
        BizManagerFactory.getBizRuleScriptManager("rule").setDao(dao);
        BizManagerFactory.getBizRuleScriptManager("rule").loadResource("/geelato/web/platform/rule/**/*.js");
        if (isNeedResetDb(args)) {
            //--3、创建表结构
            dbGenerateDao.createAllTables(true);
            //--4、初始化表数据
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String filePaths = getProperty("geelato.init.sql", "/geelato/web/platform/data/init.sql");
            String[] sqlFiles = filePaths.split(",");
            for (String sqlFile : sqlFiles) {
                try {
                    Resource[] resources = resolver.getResources(sqlFile);
                    for (Resource resource : resources) {
                        InputStream is = resource.getInputStream();
                        SqlFiles.loadAndExecute(is, dao.getJdbcTemplate(), isWinOS);
                    }
                } catch (IOException e) {
                    logger.error("加载、初始化数据（" + sqlFile + "）失败。", e);
                }
            }

        } else {
            logger.info("未收到重置数据库命令，跳过创建表结构、跳过初始化表数据。");
        }
        BizManagerFactory.getBizMvelRuleManager("mvelRule").setDao(dao);
        BizManagerFactory.getBizMvelRuleManager("mvelRule").loadDb(null);
    }

    protected String getProperty(String key, String defaultValue) {
        String value = applicationContext.getEnvironment().getProperty(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dbGenerateDao.setDao(dao);
    }

    /**
     * @param args reset_db:若参数中，带有该字符串，表示需要重置数据库结构、重新初始化数据，一般用于开发
     */
    public static void main(String[] args) {
//        if (args.length == 0) {
//            args = new String[1];
//            args[0] = "false";
//        }
//        SpringApplication.run(BootApplication.class, args);
    }
}
