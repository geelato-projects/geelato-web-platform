package org.geelato.web.platform.m.base.rest;

import org.geelato.core.Ctx;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.env.entity.User;
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

/**
 * @author itechgee@126.com
 * @date 2017/7/7.
 */
@Controller
@RequestMapping(value = "/api/page/")
public class PageController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

//    @RequestMapping(value = {"{pageCode}", "{pageCode}/*"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
//    @ResponseBody
//    public ApiMetaResult pageConfig(@PathVariable String pageCode) {
//        ApiMetaResult apiMetaResult = new ApiMetaResult();
//        try {
//            apiMetaResult.setData(dao.queryForObject(AppPage.class, "code", pageCode));
//            if (apiMetaResult.getData() == null) {
//                apiMetaResult.error();
//                apiMetaResult.setMsg("未能获取到pageCode(" + pageCode + ")对应配置信息。");
//            }
//        } catch (Exception e) {
//            apiMetaResult.error();
//            apiMetaResult.setMsg("未能获取到pageCode(" + pageCode + ")对应配置信息。");
//            logger.error("pageCode：" + pageCode, e);
//        }
//        return apiMetaResult;
//    }


    /**
     * 基于页面id或页面的扩展id（树节点id）获取页面定义及页面自定义信息
     *
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
                page = dao.queryForObject(AppPage.class, "id", id);
            } else if ("extendId".equals(idType)) {
                page = dao.queryForObject(AppPage.class, "extendId", id);
            } else {
                // 不支持的id类型
                apiResult.error();
                apiResult.setMsg("不支持的id类型" + idType);
                return apiResult;
            }

            HashMap pageMap = new HashMap(6);
            if (page != null) {
                pageMap.put("id", page.getId());
                pageMap.put("type", page.getId());
                pageMap.put("appId", page.getId());
                pageMap.put("code", page.getCode());
                pageMap.put("releaseContent", page.getReleaseContent());
            }
            User user = Ctx.getCurrentUser();
            ApiPagedResult apiPagedResult = ruleService.queryForMapList("{\"platform_my_page_custom\":{\"@fs\":\"id,cfg,pageId\",\"creator|eq\":\"" + user.getUserId() + "\",\"delStatus|eq\":0,\"@p\":\"1,1\"}}", false);
            if (apiPagedResult.getDataSize() > 0) {
                pageMap.put("pageCustom", ((List) apiPagedResult.getData()).get(0));
            } else {
                pageMap.put("pageCustom", null);
            }
            apiResult.setData(pageMap);

        } catch (Exception e) {
            logger.error("获取页面配置信息出错！", e);
            apiResult.error(e);
        }
        return apiResult;
    }

}
