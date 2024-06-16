package org.geelato.web.platform.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.geelato.core.env.EnvManager;
import org.geelato.core.env.entity.User;
import org.geelato.core.meta.annotation.IgnoreJWTVerify;
import org.geelato.web.platform.PlatformContext;
import org.geelato.web.platform.Tenant;
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
        String token = request.getHeader("Authorization");
        // 执行认证
        if (token == null) {
            throw new Exception("无效的token");
        }
        token = token.replace("Bearer ", "");
        // 验证令牌
        JWTUtil.verify(token);

        // 获取载荷内容
        DecodedJWT verify = JWTUtil.verify(token);
        String loginName = verify.getClaim("loginName").asString();
        String id = verify.getClaim("id").asString();
        String passWord = verify.getClaim("passWord").asString();
        //初始化Core中的当前用户
        User currentUser= EnvManager.singleInstance().InitCurrentUser(loginName);

        PlatformContext.setCurrentUser(currentUser);
        PlatformContext.setCurrentTenant(new Tenant("geelato"));

        UsernamePasswordToken userToken = new UsernamePasswordToken(loginName, passWord);
        Subject subject = SecurityUtils.getSubject();
        subject.login(userToken);
        return true;
    }

}