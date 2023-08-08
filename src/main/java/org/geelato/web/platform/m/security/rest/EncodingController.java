package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.geelato.web.platform.m.security.entity.Encoding;
import org.geelato.web.platform.m.security.service.EncodingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/8/2 11:01
 */
@Controller
@RequestMapping(value = "/api/encoding")
public class EncodingController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(EncodingController.class);
    @Autowired
    private EncodingService encodingService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(Encoding.class, req);

            List<Encoding> pageQueryList = encodingService.pageQueryModel(Encoding.class, pageNum, pageSize, params);
            List<Encoding> queryList = encodingService.queryModel(Encoding.class, params);

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
    public ApiResult query(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            Map<String, Object> params = this.getQueryParameters(Encoding.class, req);
            return result.setData(encodingService.queryModel(Encoding.class, params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(encodingService.getModel(Encoding.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody Encoding form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (encodingService.isExist(Encoding.class, form.getId())) {
                    result.setData(encodingService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(encodingService.createModel(form));
            }
            if (result.isSuccess()) {
                encodingService.redisTemplateEncodingUpdate(form);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/isDelete/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult isDelete(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            Encoding mResult = encodingService.getModel(Encoding.class, id);
            if (mResult != null) {
                encodingService.isDeleteModel(mResult);
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

    @RequestMapping(value = "/generate/{id}", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult generate(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(id)) {
                Encoding encoding = encodingService.getModel(Encoding.class, id);
                Assert.notNull(encoding, ApiErrorMsg.IS_NULL);
                result.success().setData(encodingService.generate(encoding));
            } else {
                result.error().setMsg(ApiErrorMsg.ID_IS_NULL);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(e.getMessage());
        }

        return result;
    }
}
