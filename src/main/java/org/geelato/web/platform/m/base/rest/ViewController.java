package org.geelato.web.platform.m.base.rest;

import com.alibaba.fastjson2.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.ViewManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.meta.model.view.ViewMeta;
import org.geelato.web.platform.m.base.service.ViewService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping(value = "/api/view/")
public class ViewController  extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(ViewController.class);
    @Autowired
    private ViewService viewService;


    @RequestMapping(value = {"pageQuery/{view_name}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiPagedResult pageQuery(@PathVariable("view_name") String viewName, HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            ViewMeta viewMeta= ViewManager.singleInstance().getByViewName(viewName);
            EntityMeta entityMeta= MetaManager.singleInstance().get(viewMeta.getSubjectEntity());
            Map<String, Object> params = this.getQueryParameters(entityMeta.getClass(), req);
            List<Map<String,Object>> pageQueryList = viewService.pageQueryModel(entityMeta.getEntityName(),viewName, pageNum, pageSize, params);

            result.setTotal(999);
            result.setData(new DataItems(pageQueryList, result.getTotal()));
            result.setPage(pageNum);
            result.setSize(pageSize);
            result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = {"defined/{view_name}"}, method = {RequestMethod.GET}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult export(@PathVariable("view_name") String viewName) {
        ApiMetaResult result = new ApiMetaResult();
        ViewMeta viewMeta= ViewManager.singleInstance().getByViewName(viewName);
        String viewColumnJson=viewMeta.getViewColumn();
        if(!StringUtils.isBlank(viewColumnJson)){
            JSONArray jsonArray = JSONArray.parse(viewMeta.getViewColumn());
            result.setMeta(jsonArray);
        }else{
            result.setMeta(viewColumnJson);
        }
        return result;
    }
}
