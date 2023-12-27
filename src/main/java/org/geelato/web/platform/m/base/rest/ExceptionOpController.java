package org.geelato.web.platform.m.base.rest;

import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.web.platform.DemoPractiseException;
import org.geelato.web.platform.m.base.entity.AppPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author itechgee@126.com
 * @date 2017/7/7.
 */
@Controller
@RequestMapping(value = "/api/exception/")
public class ExceptionOpController extends BaseController {
    private static Logger logger = LoggerFactory.getLogger(ExceptionOpController.class);

    @RequestMapping(value = {"/test", }, method = RequestMethod.GET, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult pageConfig() {
        throw new DemoPractiseException("fuck this exception");
    }

}
