package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;


/**
 * @author geemeta
 */
@ControllerAdvice
public class BaseController {

    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;

    @Autowired
    protected RuleService ruleService;
    /**
     * 创建session、Request、Response等对象
     */
    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected HttpSession session;

    /**
     * 在每个子类方法调用之前先调用
     * 设置request,response,session这三个对象
     *
     * @param request
     * @param response
     */
    @ModelAttribute
    public void setReqAndRes(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.session = request.getSession(true);

        //可以在此处拿到当前登录的用户
    }

//    /**
//     * 获取application中的属性值
//     * @param key
//     * @param defaultValue
//     * @return
//     */
//    protected String getProperty(String key, String defaultValue) {
//        String value = applicationContext.getEnvironment().getProperty(key);
//        return value == null ? defaultValue : value;
//    }
//  implements InitializingBean
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        ruleService.setDao(dao);
//    }
}
