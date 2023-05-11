package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.lang.reflect.Field;
import java.util.*;


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

    public Map<String, Object> getQueryParameters(Class elementType, HttpServletRequest request) {
        Map<String, Object> queryParamsMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            Set<String> fieldNames = getClassFieldNames(elementType);
            if (fieldNames == null || fieldNames.contains(entry.getKey())) {
                List<String> values = List.of(entry.getValue());
                if (values.size() == 1) {
                    queryParamsMap.put(entry.getKey(), values.get(0));
                } else {
                    queryParamsMap.put(entry.getKey(), values.toArray(new String[values.size()]));
                }
            }
        }

        return queryParamsMap;
    }

    private Set<String> getClassFieldNames(Class elementType) {
        Set<String> fieldNameList = new HashSet<>();
        List<Field> fieldsList = getClassFields(elementType);
        for (Field field : fieldsList) {
            fieldNameList.add(field.getName());
        }
        return fieldNameList;
    }

    private List<Field> getClassFields(Class elementType) {
        List<Field> fieldsList = new ArrayList<>();
        while (elementType != null) {
            Field[] declaredFields = elementType.getDeclaredFields();
            fieldsList.addAll(Arrays.asList(declaredFields));
            elementType = elementType.getSuperclass();
        }

        return fieldsList;
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
