package org.geelato.web.platform.rest;


import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiMultiPagedResult;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.mvc.MediaTypes;
import org.geelato.core.orm.Dao;
import org.geelato.core.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * getBizRuleScriptManager:
 *
 * @author itechgee@126.com
 * @date 2017/6/3.
 */
@Controller
@RequestMapping(value = "/api/meta/")
public class MetaController implements InitializingBean {

    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;

    @Autowired
    protected RuleService ruleService;

    private MetaManager metaManager = MetaManager.singleInstance();

    private static Logger logger = LoggerFactory.getLogger(MetaController.class);

    /**
     * e.g.:http://localhost:8080/api/meta/list/
     *
     * @param request
     * @return
     */
    @RequestMapping(value = {"list", "list/*"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiPagedResult list(@RequestParam(value = "withMeta", defaultValue = "true") boolean withMeta, HttpServletRequest request) {
        String gql = getGql(request);
        ApiPagedResult page = ruleService.queryForMapList(gql, withMeta);
        return page;
    }

    /**
     * 多列表查询，一次查询返回多个列表
     *
     * @param request
     * @return
     */
    @RequestMapping(value = {"multiList", "multiList/*"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMultiPagedResult multiList(@RequestParam(value = "withMeta", defaultValue = "true") boolean withMeta, HttpServletRequest request) {
        String gql = getGql(request);
        return ruleService.queryForMultiMapList(gql, withMeta);
    }

    /**
     * e.g.:http://localhost:8080/api/meta/list/
     *
     * @param biz     业务代码
     * @param request HttpServletRequest
     * @return SaveResult
     */
    @RequestMapping(value = {"save/{biz}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult save(@PathVariable("biz") String biz, HttpServletRequest request) {
        String gql = getGql(request);
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.save(biz, gql));
        return result;
    }


    @RequestMapping(value = {"delete/{biz}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult delete(@PathVariable("biz") String biz, HttpServletRequest request) {
        String gql = getGql(request);
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.delete(biz, gql));
        return result;
    }


    /**
     * e.g.:http://localhost:8080/api/meta/defined/
     * 获取数据定义信息，即元数据信息
     *
     * @param entityOrQueryKey 实体名称或查询键
     * @return
     */
    @RequestMapping(value = {"defined/{entityOrQueryKey}"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult defined(@PathVariable("entityOrQueryKey") String entityOrQueryKey) {
        ApiMetaResult result = new ApiMetaResult();
        if (metaManager.containsEntity(entityOrQueryKey)) {
            result.setMeta(metaManager.getByEntityName(entityOrQueryKey).getAllSimpleFieldMetas());
        } else {
            // TODO
        }
        return result;
    }

    /**
     * e.g.:http://localhost:8080/api/meta/entityNames/
     * 获取实体名称列表
     *
     * @return
     */
    @RequestMapping(value = {"entityNames"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult entityNames() {
        ApiMetaResult result = new ApiMetaResult();
        result.setData(metaManager.getAllEntityNames());
        return result;
    }


    /**
     * 获取通用树数据（platform_tree_node）
     * e.g.:http://localhost:8080/api/meta/tree/
     *
     * @param biz     业务代码
     * @param request HttpServletRequest
     * @return ApiResult
     */
    @RequestMapping(value = {"tree/{biz}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult treeNodeList(@PathVariable("biz") String biz, @RequestParam String entity, @RequestParam Long treeId, HttpServletRequest request) {
        return ruleService.queryForTreeNodeList(entity, treeId);
    }

    private String getGql(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = request.getReader();
        } catch (IOException e) {
            logger.error("未能从httpServletRequest中获取gql的内容", e);
        }

        String str;
        try {
            while ((str = br.readLine()) != null) {
                stringBuilder.append(str);
            }
        } catch (IOException e) {
            logger.error("未能从httpServletRequest中获取gql的内容", e);
        }
        return stringBuilder.toString();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ruleService.setDao(dao);
    }
}
