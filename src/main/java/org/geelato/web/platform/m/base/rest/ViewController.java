package org.geelato.web.platform.m.base.rest;

import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.web.platform.m.base.service.ViewService;
import org.geelato.web.platform.m.security.service.SecurityHelper;
import org.geelato.web.platform.m.security.service.ShiroDbRealm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/api/view/")
public class ViewController {
    @Autowired
    private ViewService viewService;

    @RequestMapping(value = {"pageQuery/{entity}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiPagedResult pageQuery(@PathVariable("entity") String entity) {
        ApiPagedResult result = new ApiPagedResult();
        return result;
    }

    @RequestMapping(value = {"export/{view_name}"}, method = {RequestMethod.POST}, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult export(@PathVariable("entity") String entity) {
        ApiMetaResult result = new ApiMetaResult();
        return result;
    }
}
