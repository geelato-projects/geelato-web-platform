package org.geelato.web.platform.boot;

import org.geelato.web.platform.m.base.interceptor.JWTInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author geemeta
 */
@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JWTInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/api/user")
                .excludePathPatterns("/api/user/forgetValid")
                .excludePathPatterns("/api/user/forget")
                .excludePathPatterns("/api/user/menu")
                .excludePathPatterns("/api/code/generate")
                .excludePathPatterns("/swagger-ui/index.html")
                .excludePathPatterns("/v3/**")
                .excludePathPatterns("/resources/**");
    }
}
