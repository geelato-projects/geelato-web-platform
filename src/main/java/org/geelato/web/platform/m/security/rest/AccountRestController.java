package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping(value = "/api/sys/account")
public class AccountRestController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(AccountRestController.class);
    @Autowired
    protected AccountService accountService;

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public ApiResult create(@RequestBody User user, HttpServletRequest req) {
        accountService.registerUser(user);
        return new ApiResult();
    }
}
