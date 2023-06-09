package org.geelato.web.platform.m.designer.rest;

import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.constants.MediaTypes;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.designer.entity.AppPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping(value = "/api/page/")
public class PageController {
    @Autowired
    protected Dao dao;

    private static Logger logger = LoggerFactory.getLogger(PageController.class);

    @RequestMapping(value = {"{pageCode}", "{pageCode}/*"}, method = RequestMethod.POST, produces = MediaTypes.JSON_UTF_8)
    @ResponseBody
    public ApiMetaResult pageConfig(@PathVariable String pageCode) {
        ApiMetaResult apiMetaResult = new ApiMetaResult();
        try {
            apiMetaResult.setData(dao.queryForObject(AppPage.class, "code", pageCode));
            if (apiMetaResult.getData() == null) {
                apiMetaResult.error();
                apiMetaResult.setMsg("未能获取到pageCode(" + pageCode + ")对应配置信息。");
            }
        } catch (Exception e) {
            apiMetaResult.error();
            apiMetaResult.setMsg("未能获取到pageCode(" + pageCode + ")对应配置信息。");
            logger.error("pageCode：" + pageCode, e);
        }
        return apiMetaResult;
    }

}
