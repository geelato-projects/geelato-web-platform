package org.geelato.web.platform.m.base.rest;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiMultiPagedResult;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
public class MetaController extends BaseController implements InitializingBean {

    private MetaManager metaManager = MetaManager.singleInstance();

    private static Logger logger = LoggerFactory.getLogger(MetaController.class);

    /**
     * @param request
     * @return
     */
    @RequestMapping(value = {"list", "list/*"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
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
    /**
     * @param biz     业务代码
     * @param request HttpServletRequest
     * @return SaveResult
     */
    @RequestMapping(value = {"batchSave/{biz}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult batchSave(@PathVariable("biz") String biz, HttpServletRequest request) {
        String gql = getGql(request);
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.batchSave(biz, gql));
        return result;
    }
    @RequestMapping(value = {"multiSave"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult multiSave(HttpServletRequest request) {
        String gql = getGql(request);
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.multiSave(gql));
        return result;
    }
    @RequestMapping(value = {"delete/{biz}/{id}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult delete(@PathVariable("biz") String biz, @PathVariable("id") String id) {
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.delete(biz, id));
        return result;
    }

    @RequestMapping(value = {"delete2/{biz}"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult delete(@PathVariable("biz") String biz, HttpServletRequest request) {
        String gql = getGql(request);
        ApiMetaResult result = new ApiMetaResult();
        result.setData(ruleService.deleteByGql(biz, gql));
        return result;
    }

    /**
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
     * 获取实体名称列表
     *
     * @return
     */
    @RequestMapping(value = {"entityNames"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult entityNames(@RequestParam String appCode) {
        ApiResult result = new ApiResult();
        result.setData(metaManager.getAllEntityNames());
        return result;
    }

    /**
     * 获取指定应用下的精简版实体元数据信息列表
     *
     * @param appCode 应用编码
     * @return
     */
    @RequestMapping(value = {"entityLiteMetas"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult queryLiteEntities(@RequestParam String appCode) {
        ApiResult result = new ApiResult();
        result.setData(metaManager.getAllEntityLiteMetas());
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

    /**
     * 唯一性校验
     *
     * @param request
     * @return
     */
    @RequestMapping(value = {"uniqueness"}, method = {RequestMethod.POST, RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult uniqueness(HttpServletRequest request) {
        ApiResult result = new ApiResult();
        String gql = getGql(request);
        if (Strings.isNotBlank(gql)) {
            JSONObject jo = JSON.parseObject(gql);
            String key = jo.keySet().iterator().next();
            JSONObject value = jo.getJSONObject(key);
            if (!value.containsKey("@fs")) {
                jo.getJSONObject(key).put("@fs", "id");
            }
            if (!value.containsKey("@p")) {
                jo.getJSONObject(key).put("@p", "1,10");
            }
            gql = JSON.toJSONString(jo);
        }
        ApiPagedResult page = ruleService.queryForMapList(gql, false);
        result.setData(page.getTotal() == 0);
        return result;
    }
}
