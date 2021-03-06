package org.geelato.web.platform.m.base.rest;


import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.mvc.MediaTypes;
import org.geelato.core.orm.Dao;
import org.geelato.core.orm.DbGenerateDao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author itechgee@126.com
 * @date 2017/6/3.
 */
@Controller
@RequestMapping(value = "/api/meta/ddl/")
public class MetaDdlController implements InitializingBean {

    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;

    @Autowired
    protected RuleService ruleService;

    @Autowired
    protected DbGenerateDao dbGenerateDao;

    private MetaManager metaManager = MetaManager.singleInstance();

    private static Logger logger = LoggerFactory.getLogger(MetaDdlController.class);


    /**
     * e.g.:http://localhost:8080/api/meta/ddl/recreate/xxxentity
     * 新建或更新表，不删除表字段
     *
     * @param entity 实体名称
     * @return
     */
    @RequestMapping(value = {"recreate/{entity}"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult recreate(@PathVariable("entity") String entity) {
        ApiMetaResult result = new ApiMetaResult();
        dbGenerateDao.createOrUpdateOneTable(entity, false);
        return result;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        ruleService.setDao(dao);
        dbGenerateDao.setDao(dao);
    }
}
