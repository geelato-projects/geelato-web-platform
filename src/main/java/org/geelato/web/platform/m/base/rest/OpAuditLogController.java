package org.geelato.web.platform.m.base.rest;

import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.MediaTypes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/api/oplog")
public class OpAuditLogController  extends BaseController {

    @RequestMapping(value = {"query/{bizId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiResult queryLog(@PathVariable("bizId") String bizId){
        return null;
    }

    @RequestMapping(value = {"pagequery/{bizId}"}, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiPagedResult pageQueryLog(@PathVariable("bizId") String bizId){
        return null;
    }
}
