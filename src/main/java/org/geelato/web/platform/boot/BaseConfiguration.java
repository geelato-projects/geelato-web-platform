package org.geelato.web.platform.boot;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 获取属性文件信息
 * @author geelato
 */
public class BaseConfiguration implements ApplicationContextAware {

    protected ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }

    public String getProperty(String key,String defaultValue){
        return applicationContext.getEnvironment().getProperty(key,defaultValue);
    }

}
