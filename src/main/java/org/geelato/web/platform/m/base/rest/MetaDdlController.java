package org.geelato.web.platform.m.base.rest;


import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.orm.DbGenerateDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author itechgee@126.com
 * @date 2017/6/3.
 */
@Controller
@RequestMapping(value = "/api/meta/ddl/")
public class MetaDdlController extends BaseController implements InitializingBean {

    @Autowired
    protected DbGenerateDao dbGenerateDao;

    private static Logger logger = LoggerFactory.getLogger(MetaDdlController.class);


    /**
     * e.g.:http://localhost:8080/api/meta/ddl/recreate/xxxentity
     * 新建或更新表，不删除表字段
     *
     * @param entity 实体名称
     * @return
     */
    @RequestMapping(value = {"table/{entity}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult recreate(@PathVariable("entity") String entity) {
        ApiMetaResult result = new ApiMetaResult();
        try {
            dbGenerateDao.createOrUpdateOneTable(entity, false);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error().setMsg(ex.getCause().getMessage());
        }
        return result;
    }

    /**
     * 新建更新视图
     *
     * @return
     */
    @RequestMapping(value = {"view/{view}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult recreate(@PathVariable("view") String view, @RequestBody Map<String, String> params) {
        ApiMetaResult result = new ApiMetaResult();
        dbGenerateDao.createOrUpdateView(view, params.get("sql"));
        return result;
    }

    @RequestMapping(value = {"view/valid/{connectId}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult<Boolean> validateView(@PathVariable("connectId") String connectId, @RequestBody Map<String, String> params) {
        ApiMetaResult<Boolean> result = new ApiMetaResult();
        try {
            boolean isValid = dbGenerateDao.validateViewSql(connectId, params.get("sql"));
            result.success().setData(isValid);
        } catch (SQLException e) {
            logger.error(e.getMessage());
            result.success().setData(false);
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dbGenerateDao.setDao(dao);
    }
}
