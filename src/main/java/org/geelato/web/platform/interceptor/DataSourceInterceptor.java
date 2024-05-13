package org.geelato.web.platform.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.geelato.core.env.EnvManager;
import org.geelato.core.meta.annotation.IgnoreJWTVerify;
import org.geelato.web.platform.boot.DynamicDataSource;
import org.geelato.web.platform.boot.DynamicDatasourceHolder;
import org.geelato.web.platform.m.security.service.JWTUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

public class DataSourceInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String tenant=request.getHeader("tenant");
        String app=request.getHeader("app");
        DynamicDatasourceHolder.setDataSource(app);
        return true;
    }
}
