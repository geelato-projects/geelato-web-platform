package org.geelato.web.platform.boot;

import org.geelato.core.biz.rules.BizManagerFactory;
import org.geelato.core.env.EnvManager;
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
import java.util.ArrayList;
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

    protected boolean isNeedResetDb = false;
    protected boolean isIgnoreInitData = false;
    // main 接受的参数
    protected String MAIN_ARGS_RESET_DB = "reset_db";
    protected String MAIN_ARGS_IGNORE_INIT_DATA = "ignore_init_data";
    protected String IGNORE_ENTITY_PREFIX = "ignore_entity_prefix_";
    protected String RESET_ONLY_ENTITY_PREFIX = "reset_entity_only_";
    // 不重置DB的实体前缀
    protected ArrayList ignoreEntityNamePrefixList = new ArrayList();
    protected ArrayList resetOnlyEntityNamePrefixList = new ArrayList();


    private void assertOS() {
        Properties prop = System.getProperties();
        String os = prop.getProperty("os.name");
        logger.info("[操作系统]" + os);
        if (os.toLowerCase().startsWith("win")) {
            isWinOS = true;
        } else {
            isWinOS = false;
        }
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
        parseStartArgs(args);
        initMeta(args);
        initEnv();
        logger.info("[启动应用]...OK");
    }


    private void parseStartArgs(String... args) {
        isNeedResetDb = false;
        isIgnoreInitData = false;
        for (String arg : args) {
            if (this.MAIN_ARGS_RESET_DB.equals(arg)) {
                isNeedResetDb = true;
            } else if (this.MAIN_ARGS_IGNORE_INIT_DATA.equals(arg)) {
                isIgnoreInitData = true;
            } else {
                int index = arg.indexOf(IGNORE_ENTITY_PREFIX);
                if (index != -1) {
                    String entityNamePrefix = arg.substring(index + IGNORE_ENTITY_PREFIX.length()).trim();
                    if (!entityNamePrefix.isEmpty()) {
                        ignoreEntityNamePrefixList.add(entityNamePrefix);
                    }
                }
            }
        }
    }

    public void initEnv(){
        EnvManager.singleInstance().SetDao(dbGenerateDao.getDao());
        EnvManager.singleInstance().EnvInit();
    }
    public void initMeta(String... args) throws IOException {
        MetaRelf.setApplicationContext(applicationContext);
        String[] packageNames = getProperty("geelato.meta.scan-package-names", "org.geelato").split(",");
        for (String packageName : packageNames) {
            MetaManager.singleInstance().scanAndParse(packageName, false);
        }
        MetaManager.singleInstance().parseDBMeta(dbGenerateDao.getDao());
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
        if (isNeedResetDb) {
            logger.info("收到重置数据库命令，开始创建表结构、初始化表数据。");
            //--3、创建表结构
            dbGenerateDao.createAllTables(true, ignoreEntityNamePrefixList);
            //--4、初始化表数据
            if (!isIgnoreInitData) {
                String filePaths = getProperty("geelato.init.sql", "/geelato/web/platform/data/init.sql");
                String[] sqlFiles = filePaths.split(",");
                for (String sqlFile : sqlFiles) {
                    InputStream is = this.getClass().getClassLoader().getResourceAsStream(sqlFile);
                    if (is == null) {
                        // jar:file:/data/geelato-web-quickstart-1.0.0-SNAPSHOT.jar!/BOOT-INF/lib/geelato-web-platform-1.0.0-SNAPSHOT.jar!/geelato.web.platform/data/init.sql
                    }
                    SqlFiles.loadAndExecute(is, dao.getJdbcTemplate(), isWinOS);
                }
            }
        } else {
            logger.info("未收到重置数据库命令，跳过创建表结构、跳过初始化表数据。");
        }
        BizManagerFactory.getBizMvelRuleManager("mvelRule").setDao(dao);
        BizManagerFactory.getBizMvelRuleManager("mvelRule").loadDb(null);
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
        if (isNeedResetDb) {
            //--3、创建表结构
            dbGenerateDao.createAllTables(true, ignoreEntityNamePrefixList);
            //--4、初始化表数据
            if (!isIgnoreInitData) {
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

}
