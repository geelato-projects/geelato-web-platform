package org.geelato.web.platform.m.excel.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.excel.service.ExportExcelService;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/2 15:23
 */
@Controller
@RequestMapping(value = "/api/export/file")
public class ExportExcelController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(ExportExcelController.class);

    @Autowired
    private AttachService attachService;
    @Autowired
    private ExportExcelService exportExcelService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(Attach.class, req);

            List<Attach> pageQueryList = attachService.pageQueryModel(Attach.class, pageNum, pageSize, params);
            List<Attach> queryList = attachService.queryModel(Attach.class, params);

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

    /**
     * 导出excel
     *
     * @param request
     * @param response
     * @param dataType   数据来源，mql、data
     * @param templateId 模板id
     * @param fileName   导出文件名称
     */
    @RequestMapping(value = "/{dataType}/{templateId}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult exportWps(HttpServletRequest request, HttpServletResponse response, @PathVariable String dataType, @PathVariable String templateId,
                               String fileName, String markText, String markKey, boolean readonly) {
        ApiResult result = new ApiResult();
        try {
            String jsonText = exportExcelService.getGql(request);
            List<Map> valueMapList = new ArrayList<>();
            Map valueMap = new HashMap();
            // List<BatchGetFormDataByIdListResponseBody.BatchGetFormDataByIdListResponseBodyResult> resultList = null;
            if ("mql".equals(dataType)) {
                // todo 查询接口
            } else if ("data".equals(dataType) && Strings.isNotBlank(jsonText)) {
                JSONObject jo = JSON.parseObject(jsonText);
                valueMapList = (List<Map>) jo.get("valueMapList");
                valueMap = (Map) jo.get("valueMap");
            } else {
                throw new RuntimeException("暂不支持解析该数据类型！");
            }

            result = exportExcelService.exportWps(templateId, fileName, valueMapList, valueMap, markText, markKey, readonly);
        } catch (Exception e) {
            logger.error("表单信息导出Excel出错。", e);
            result.error().setMsg(e.getMessage());
        }

        return result;
    }
}
