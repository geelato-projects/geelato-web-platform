package org.geelato.web.platform.rest;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.J2Cache;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.subject.Subject;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.entity.security.User;
import org.geelato.web.platform.entity.settings.CommonConfig;
import org.geelato.web.platform.entity.settings.UserConfig;
import org.geelato.web.platform.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by hongxq on 2014/5/10.
 */
@Controller
@RequestMapping(value = "/api/sys/auth")
public class AuthRestController {

    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    private Function commonConfigLoader = (p) -> dao.queryForMapList(CommonConfig.class);
    private Function userConfigLoader = (userId) -> dao.queryForMapList(UserConfig.class, "id", userId);
    private CacheChannel cache = J2Cache.getChannel();

//    @Autowired
//    private ShiroDbRealm shiroDbRealm;

    private Logger logger = LoggerFactory.getLogger(AuthRestController.class);

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Map login(@RequestBody User user, HttpServletRequest req) {
        Subject currentUser = SecurityUtils.getSubject();
        if (!currentUser.isAuthenticated()) {
            //collect user principals and credentials in a gui specific manner
            //such as username/password html form, X509 certificate, OpenID, etc.
            //We'll use the username/password example here since it is the most common.
            //(do you know what movie this is from? ;)
            UsernamePasswordToken token = new UsernamePasswordToken(user.getLoginName(), user.getPassword());
            //this is all you have to do to support 'remember me' (no config - built in!):
            boolean rememberMe = Boolean.parseBoolean(req.getParameter("remember"));
            token.setRememberMe(rememberMe);
            try {
                if (logger.isDebugEnabled())
                    logger.debug("User [" + token.getUsername() + "] logging in ... ");
                currentUser.login(token);
                //if no exception, that's it, we're done!
                if (logger.isDebugEnabled())
                    logger.debug("User [" + currentUser.getPrincipal() + "] login successfully.");
            } catch (UnknownAccountException uae) {
                //username wasn't in the system, show them an error message?
                throw new RestException(HttpStatus.BAD_REQUEST, "无效的用户名！");
            } catch (IncorrectCredentialsException ice) {
                //password didn't match, try again?
                throw new RestException(HttpStatus.BAD_REQUEST, "无效的密码！");
            } catch (LockedAccountException lae) {
                //account for that username is locked - can't login.  Show them a message?
                throw new RestException(HttpStatus.BAD_REQUEST, "用户账号已被锁！");
            } catch (AuthenticationException ae) {
                //unexpected condition - error?
                throw new RestException(HttpStatus.BAD_REQUEST, "登录失败！[" + ae.getMessage() + "]");
            }
        }
        user = dao.queryForObject(User.class, "loginName", user.getLoginName());
        user.setSalt("");
        user.setPassword("");
        user.setPlainPassword("");
        return wrap(user);
    }

    @RequestMapping(value = "/isLogged")
    @ResponseBody
    public Map isLogged() {
        if (SecurityHelper.isAuthenticatedForCurrentUser()) {
            User user = dao.queryForObject(User.class, SecurityHelper.getCurrentUserId());
            user.setSalt("");
            user.setPassword("");
            user.setPlainPassword("");
            return wrap(user);
        }
        return null;
    }

    @RequestMapping(value = "/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        if (!SecurityHelper.isAuthenticatedForCurrentUser()) {
            logger.debug("No User to logout.");
        } else {
            String name = SecurityHelper.getCurrentUser().getName();
            Subject currentUser = SecurityUtils.getSubject();
            currentUser.logout();
            logger.debug("User [" + name + "] logout successfully.");
        }
    }

    private Map wrap(User user) {
        HashMap map = new HashMap(3);
        map.put("user", user);
        //user config
//        map.put("userConfig", cache.get("config", user.getId().toString(), userConfigLoader));
        map.put("userConfig",dao.queryForMapList(UserConfig.class, "creator", 1));
        map.put("commonConfig", cache.get("config", "commonConfig", commonConfigLoader));
        return map;
    }


}
