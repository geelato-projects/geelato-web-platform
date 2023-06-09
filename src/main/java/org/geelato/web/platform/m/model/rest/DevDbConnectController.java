package org.geelato.web.platform.m.model.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.meta.model.connect.ConnectMeta;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.model.service.DevDbConnectService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.core.constants.ApiErrorMsg;
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
@RequestMapping(value = "/api/model/connect")
public class DevDbConnectController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(DevDbConnectController.class);
    @Autowired
    private DevDbConnectService devDbConnectService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult<DataItems> pageQuery(HttpServletRequest req) {
        ApiPagedResult<DataItems> result = new ApiPagedResult<>();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(ConnectMeta.class, req);

            List<ConnectMeta> pageQueryList = devDbConnectService.pageQueryModel(ConnectMeta.class, pageNum, pageSize, params);
            List<ConnectMeta> queryList = devDbConnectService.queryModel(ConnectMeta.class, params);

            result.setTotal(queryList != null ? queryList.size() : 0);
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

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<List<ConnectMeta>> query(HttpServletRequest req) {
        ApiResult<List<ConnectMeta>> result = new ApiResult<>();
        try {
            Map<String, Object> params = this.getQueryParameters(ConnectMeta.class, req);
            return result.setData(devDbConnectService.queryModel(ConnectMeta.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult<ConnectMeta> get(@PathVariable(required = true) long id) {
        ApiResult<ConnectMeta> result = new ApiResult<>();
        try {
            return result.setData(devDbConnectService.getModel(ConnectMeta.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult<Map> createOrUpdate(@RequestBody ConnectMeta form) {
        ApiResult<Map> result = new ApiResult<>();
        try {
            // ID为空方可插入
            if (form.getId() != null && form.getId() > 0) {
                // 存在，方可更新
                if (devDbConnectService.isExist(ConnectMeta.class, form.getId())) {
                    form.afterSet();
                    result.setData(devDbConnectService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(devDbConnectService.createModel(form));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) long id) {
        ApiResult result = new ApiResult();
        try {
            ConnectMeta mResult = devDbConnectService.getModel(ConnectMeta.class, id);
            if (mResult != null) {
                devDbConnectService.isDeleteModel(mResult);
                result.success();
            } else {
                result.error().setMsg(ApiErrorMsg.IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}
