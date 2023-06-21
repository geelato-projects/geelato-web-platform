package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.web.platform.m.base.entity.Dict;
import org.geelato.web.platform.m.base.entity.DictItem;
import org.geelato.web.platform.m.base.service.DictItemService;
import org.geelato.web.platform.m.base.service.DictService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/dict/item")
public class DictItemController extends BaseController {
    private static final String DICT_CODE = "dictCode";
    private static final String DICT_ID = "dictId";
    private final Logger logger = LoggerFactory.getLogger(DictItemController.class);
    @Autowired
    private DictService dictService;
    @Autowired
    private DictItemService dictItemService;

    @RequestMapping(value = "/pageQuery", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(DictItem.class, req);

            List<DictItem> pageQueryList = dictItemService.pageQueryModel(DictItem.class, pageNum, pageSize, params);
            List<DictItem> queryList = dictItemService.queryModel(DictItem.class, params);

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
            Map<String, Object> params = this.getQueryParameters(DictItem.class, req);
            return result.setData(dictItemService.queryModel(DictItem.class, params));
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
            return result.setData(dictItemService.getModel(DictItem.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/createOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult createOrUpdate(@RequestBody DictItem form) {
        ApiResult result = new ApiResult();
        try {
            // ID为空方可插入
            if (Strings.isNotBlank(form.getId())) {
                // 存在，方可更新
                if (dictItemService.isExist(DictItem.class, form.getId())) {
                    result.setData(dictItemService.updateModel(form));
                } else {
                    result.error().setMsg(ApiErrorMsg.IS_NULL);
                }
            } else {
                result.setData(dictItemService.createModel(form));
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
            DictItem mResult = dictItemService.getModel(DictItem.class, id);
            if (mResult != null) {
                dictItemService.isDeleteModel(mResult);
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

    @RequestMapping(value = "/queryItemByDicCode/{dictCode}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryItemByDicCode(@PathVariable(required = true) String dictCode) {
        ApiResult result = new ApiResult();
        List<DictItem> iResult = new ArrayList<>();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(DICT_CODE, dictCode);
            // 字典
            List<Dict> dResult = dictService.queryModel(Dict.class, params);
            // 字典项
            if (dResult != null && !dResult.isEmpty()) {
                params.remove(DICT_CODE);
                params.put(DICT_ID, dResult.get(0).getId());
                params.put(ColumnDefault.ENABLE_STATUS_FIELD, ColumnDefault.ENABLE_STATUS_VALUE);
                iResult = dictItemService.queryModel(DictItem.class, params);
            }
            result.success().setData(iResult);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
        }

        return result;
    }
}