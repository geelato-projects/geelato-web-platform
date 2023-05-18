package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.ErrorMsg;
import org.geelato.web.platform.m.security.entity.Org;
import org.geelato.web.platform.m.security.service.OrgService;
import org.geelato.web.platform.m.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/security/org")
public class OrgRestController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(OrgRestController.class);
    @Autowired
    private OrgService orgService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(Org.class, req);

            List<Org> pageQueryList = orgService.pageQueryModel(Org.class, pageNum, pageSize, params);
            List<Org> queryList = orgService.queryModel(Org.class, params);

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
            Map<String, Object> params = this.getQueryParameters(Org.class, req);
            return result.setData(orgService.queryModel(Org.class, params));
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
            return result.setData(orgService.getModel(Org.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody Org form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (form.getId() != null && form.getId() > 0) {
                // 存在，方可更新
                if (orgService.isExist(Org.class, form.getId())) {
                    result.setData(orgService.updateModel(form));
                } else {
                    result.error().setMsg(ErrorMsg.IS_NULL);
                }
            } else {
                result.setData(orgService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult create(@RequestBody Org form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            form.setId(null);
            return result.setData(orgService.createModel(form));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.CREATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult update(@RequestBody Org form) {
        ApiResult result = new ApiResult();
        try {
            if (orgService.isExist(Org.class, form.getId())) {
                result.setData(orgService.updateModel(form));
            } else {
                result.error().setMsg(ErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.UPDATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult delete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            orgService.deleteModel(Org.class, id);
            result.success();
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.DELETE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            Org mResult = orgService.getModel(Org.class, id);
            if (mResult != null) {
                orgService.isDeleteModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}
