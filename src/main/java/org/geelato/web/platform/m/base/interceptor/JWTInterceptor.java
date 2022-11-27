package org.geelato.web.platform.m.base.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.geelato.core.meta.annotation.IgnoreJWTVerify;
import org.geelato.web.platform.m.security.service.JWTUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


import java.lang.reflect.Method;

/**
 * @author geemeta
 */
public class JWTInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {


        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        // 检查是否有IgnoreToken注释，有则跳过认证
        if (method.isAnnotationPresent(IgnoreJWTVerify.class)) {
            return true;
        }

        // 从请求头内获取token
        String token = request.getHeader("authorization");
        // 执行认证
        if (token == null) {
            throw new Exception("无效的token");
        }

        // 验证令牌
        JWTUtil.verify(token);

        // 获取载荷内容
        DecodedJWT verify = JWTUtil.verify(token);
        String loginName = verify.getClaim("loginName").asString();
        String id = verify.getClaim("id").asString();

        // 放入attribute以便后面调用
        request.setAttribute("loginName", loginName);
        request.setAttribute("id", id);

        return true;
    }

}