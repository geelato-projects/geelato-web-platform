package org.geelato.web.platform.m.base.rest;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.service.AttachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/5 10:15
 */
@Controller
@RequestMapping(value = "/api/attach")
public class AttachController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(AttachController.class);
    @Autowired
    private AttachService attachService;


    @RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult get(@PathVariable(required = true) String id) {
        ApiResult result = new ApiResult();
        try {
            return result.setData(attachService.getModel(Attach.class, id));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult list(@RequestBody Map<String, Object> requestMap) {
        ApiResult result = new ApiResult();
        try {
            List<Attach> attachList = new ArrayList<>();
            String ids = (String) requestMap.get("ids");
            if (Strings.isNotBlank(ids)) {
                String[] idArr = ids.split(",");
                if (idArr.length > 0) {
                    for (int i = 0; i < idArr.length; i++) {
                        attachList.add(attachService.getModel(Attach.class, idArr[i]));
                    }
                }
            }
            return result.setData(attachList);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/remove/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ApiResult remove(@PathVariable(required = true) String id, Boolean isRemoved) {
        ApiResult result = new ApiResult<>();
        try {
            Attach model = attachService.getModel(Attach.class, id);
            if (model != null) {
                attachService.isDeleteModel(model);
                if (isRemoved) {
                    boolean delFile = attachService.deleteFile(model);
                    if (delFile) {
                        result.success();
                    } else {
                        result.error().setMsg(ApiErrorMsg.DELETE_FAIL);
                    }
                } else {
                    result.success();
                }
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
