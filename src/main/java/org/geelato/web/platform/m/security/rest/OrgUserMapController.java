package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.OrgUserMap;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.OrgUserMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/security/org/user")
public class OrgUserMapController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(OrgUserMapController.class);
    @Autowired
    private OrgUserMapService orgUserMapService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(OrgUserMap.class, req);

            List<OrgUserMap> pageQueryList = orgUserMapService.pageQueryModel(OrgUserMap.class, pageNum, pageSize, params);
            List<OrgUserMap> queryList = orgUserMapService.queryModel(OrgUserMap.class, params);

            result.setTotal(queryList != null ? queryList.size() : 0);
            result.setData(new DataItems(pageQueryList, result.getTotal()));
            result.setPage(pageNum);
            result.setSize(pageSize);
            result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult query(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = this.getQueryParameters(OrgUserMap.class, req);
            return result.setData(orgUserMapService.queryModel(OrgUserMap.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(orgUserMapService.getModel(OrgUserMap.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/insert", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult insert(@RequestBody OrgUserMap form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("userId", form.getUserId());
            params.put("orgId", form.getOrgId());
            List<OrgUserMap> oList = orgUserMapService.queryModel(OrgUserMap.class, params);
            if (oList != null && !oList.isEmpty()) {
                result.error().setMsg(ErrorMsg.IS_EXIST);
            } else {
                orgUserMapService.insertModel(form);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            if (!orgUserMapService.isExist(User.class, "orgId", id)) {
                OrgUserMap mResult = orgUserMapService.getModel(OrgUserMap.class, id);
                if (mResult != null) {
                    orgUserMapService.isDeleteModel(mResult);
                    result.success();
                } else {
                    result.error().setMsg(ErrorMsg.IS_NULL);
                }
            } else {
                result.error().setMsg(ErrorMsg.FOR_FAIL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}
