package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author diabl
 */
@Controller
@RequestMapping(value = "/api/dict/item")
public class DictItemController extends BaseController {
    private static final String DICT_CODE = "dictCode";
    private static final String DICT_ID = "dictId";
    private static final String ROOT_PARENT_ID = "";
    private static final String DEFAULT_ORDER_BY = "seq_no ASC";

    private static final Map<String, List<String>> OPERATORMAP = new LinkedHashMap<>();

    static {
        OPERATORMAP.put("contains", Arrays.asList("itemCode", "itemName", "itemRemark"));
        OPERATORMAP.put("intervals", Arrays.asList("createAt", "updateAt"));
    }

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
            FilterGroup filterGroup = this.getFilterGroup(params, OPERATORMAP);

            List<DictItem> pageQueryList = dictItemService.pageQueryModel(DictItem.class, pageNum, pageSize, DEFAULT_ORDER_BY, filterGroup);
            List<DictItem> queryList = dictItemService.queryModel(DictItem.class, filterGroup);

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

    @RequestMapping(value = "/batchCreateOrUpdate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult batchCreateOrUpdate(@RequestBody List<DictItem> forms, String dictId) {
        ApiResult result = new ApiResult();
        try {
            dictItemService.batchCreateOrUpdate(dictId, forms);
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

    @RequestMapping(value = "/queryItemByDictCode/{dictCode}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult queryItemByDictCode(@PathVariable(required = true) String dictCode) {
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
            result.success().setData(buildTree(iResult));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    public List<DictItem> buildTree(List<DictItem> pidList) {
        boolean isChild = false;
        if (pidList != null && !pidList.isEmpty()) {
            for (DictItem item : pidList) {
                if (Strings.isNotBlank(item.getPid())) {
                    isChild = true;
                } else {
                    item.setPid(ROOT_PARENT_ID);
                }
            }
        }
        if (!isChild) {
            return pidList;
        }
        Map<String, List<DictItem>> pidListMap = pidList.stream().collect(Collectors.groupingBy(DictItem::getPid));
        pidList.forEach(item -> item.setChildren(pidListMap.get(item.getId())));
        //返回结果也改为返回顶层节点的list
        return pidListMap.get(ROOT_PARENT_ID);
    }

    @RequestMapping(value = "/validate", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult validate(@RequestBody DictItem form) {
        ApiResult result = new ApiResult();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("item_code", form.getItemCode());
            params.put("dict_id", form.getDictId());
            params.put("del_status", String.valueOf(DeleteStatusEnum.NO.getCode()));
            params.put("tenant_code", form.getTenantCode());
            result.setData(dictService.validate("platform_dict_item", form.getId(), params));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.VALIDATE_FAIL);
        }

        return result;
    }
}