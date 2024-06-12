package org.geelato.web.platform.m.base.rest;

import org.geelato.core.Ctx;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.env.entity.User;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.web.platform.m.base.entity.AppPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author itechgee@126.com
 * @date 2017/7/7.
 */
@Controller
@RequestMapping(value = "/api/page/")
public class PageController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);



    /**
     * 基于页面id或页面的扩展id（树节点id）获取页面定义及页面自定义信息
     * 排除已删除的记录
     * @param idType “pageId”或“extendId”
     * @param id     id值
     * @return {id,type,appId,code,releaseContent,pageCustom}，其中pageCustom为不同用户对该页面的自定义信息
     */
    @RequestMapping(value = {"getPageAndCustom/{idType}/{id}", "getPageAndCustom/{idType}/{id}/*"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult getPageAndCustom(@PathVariable String idType, @PathVariable String id) {
        ApiResult apiResult = new ApiResult();
        try {
            AppPage page = null;
            if ("pageId".equals(idType)) {
                page = dao.queryForObject(AppPage.class, "id", id,"delStatus","0");
            } else if ("extendId".equals(idType)) {
                page = dao.queryForObject(AppPage.class, "extendId", id,"delStatus","0");
            } else {
                // 不支持的id类型
                apiResult.error();
                apiResult.setMsg("不支持的id类型" + idType);
                return apiResult;
            }

            HashMap pageMap = new HashMap(6);
            if (page != null) {
                pageMap.put("id", page.getId());
                pageMap.put("type", page.getType());
                pageMap.put("appId", page.getAppId());
                pageMap.put("code", page.getCode());
                pageMap.put("releaseContent", page.getReleaseContent());
            }
            User user = Ctx.getCurrentUser();
            // 用户自定义信息
            ApiPagedResult apiPagedResult = ruleService.queryForMapList("{\"platform_my_page_custom\":{\"@fs\":\"id,cfg,pageId\",\"creator|eq\":\"" + user.getUserId() + "\",\"pageId|eq\":\""+page.getId()+"\",\"delStatus|eq\":0,\"@p\":\"1,1\"}}", false);
            if (apiPagedResult.getDataSize() > 0) {
                pageMap.put("pageCustom", ((List) apiPagedResult.getData()).get(0));
            }else{
                pageMap.put("pageCustom", null);
            }
            // 用户对该页面的操作权限
            Map params = new HashMap<String, String>(1);
            params.put("userId", user.getUserId());
            params.put("object", page.getId());
            params.put("appId", page.getAppId());
            params.put("type", "ep");
            List<Map<String, Object>> permsList = dao.queryForMapList("query_permission_code_and_rule_by_role_user", params);
            if (permsList != null && permsList.size() > 0) {
                pageMap.put("pagePerms", permsList);
            }else{
                pageMap.put("pagePerms", null);
            }

            apiResult.setData(pageMap);
        } catch (Exception e) {
            logger.error("获取页面配置信息出错！", e);
            apiResult.error(e);
        }
        return apiResult;
    }

}
