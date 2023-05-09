package org.geelato.web.platform.m.security.rest;

import org.geelato.web.platform.m.base.rest.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/*
    created by chengx
 */
@Controller
@RequestMapping(value = "/api/security/org")
public class OrgRestController extends BaseController {

    @RequestMapping(value = "/query", method = RequestMethod.GET)
    @ResponseBody
    public String queryOrg  () {
        return "queryOrg";
    }


}
