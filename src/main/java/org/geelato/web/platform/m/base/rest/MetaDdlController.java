package org.geelato.web.platform.m.base.rest;


import org.apache.logging.log4j.util.Strings;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.model.entity.TableMeta;
import org.geelato.core.orm.DbGenerateDao;
import org.geelato.web.platform.m.model.service.DevTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author itechgee@126.com
 * @date 2017/6/3.
 */
@Controller
@RequestMapping(value = "/api/meta/ddl/")
public class MetaDdlController extends BaseController {

    @Autowired
    protected DbGenerateDao dbGenerateDao;
    @Autowired
    private DevTableService devTableService;

    private static final Logger logger = LoggerFactory.getLogger(MetaDdlController.class);


    /**
     * 新建或更新表，不删除表字段
     *
     * @param entity 实体名称
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

    @RequestMapping(value = {"tables/{appId}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult recreates(@PathVariable("appId") String appId) {
        ApiMetaResult result = new ApiMetaResult();
        Map<String, Boolean> tableResult = new LinkedHashMap<>();
        try {
            if (Strings.isNotBlank(appId)) {
                FilterGroup filterGroup = new FilterGroup();
                filterGroup.addFilter("appId", appId);
                filterGroup.addFilter("tenantCode", Ctx.getCurrentTenantCode());
                filterGroup.addFilter("enableStatus", String.valueOf(EnableStatusEnum.ENABLED.getCode()));
                List<TableMeta> tableMetas = devTableService.queryModel(TableMeta.class, filterGroup);
                if (tableMetas != null) {
                    tableMetas.sort(new Comparator<TableMeta>() {
                        @Override
                        public int compare(TableMeta o1, TableMeta o2) {
                            return o1.getEntityName().compareToIgnoreCase(o2.getEntityName());
                        }
                    });
                    for (TableMeta meta : tableMetas) {
                        tableResult.put(meta.getEntityName(), false);
                    }
                    for (int i = 0; i < tableMetas.size(); i++) {
                        dbGenerateDao.createOrUpdateOneTable(tableMetas.get(i).getEntityName(), false);
                        logger.info(String.format("成功插入第 %s 个。表名：%s", (i + 1), tableMetas.get(i).getEntityName()));
                        tableResult.put(tableMetas.get(i).getEntityName(), true);
                    }
                }
            }
            result.setData(tableResult);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error().setMsg(ex.getCause().getMessage()).setData(tableResult);
        }
        return result;
    }

    /**
     * 新建更新视图
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

}
