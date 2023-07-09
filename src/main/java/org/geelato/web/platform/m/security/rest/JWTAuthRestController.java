package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiResult;
import org.geelato.core.meta.annotation.IgnoreJWTVerify;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.LoginParams;
import org.geelato.web.platform.m.security.entity.LoginResult;
import org.geelato.web.platform.m.security.entity.LoginRoleInfo;
import org.geelato.web.platform.m.security.entity.User;
import org.geelato.web.platform.m.security.service.AccountService;
import org.geelato.web.platform.m.security.service.JWTUtil;
import org.geelato.web.platform.m.security.service.SecurityHelper;
import org.geelato.web.platform.m.security.service.ShiroDbRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hongxq on 2022/5/1.
 */
@Controller
@RequestMapping(value = {"/api/sys/jwtauth", "/basic-api", "/api/user"})
public class JWTAuthRestController extends BaseController {

    @Autowired
    protected AccountService accountService;

    private Logger logger = LoggerFactory.getLogger(JWTAuthRestController.class);


    @IgnoreJWTVerify
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public ApiResult login(@RequestBody LoginParams loginParams, HttpServletRequest req) {
        ApiResult apiResult = new ApiResult();
        try {
            //用户登录校验
            User loginUser = dao.queryForObject(User.class, "loginName", loginParams.getUsername());
            Boolean checkPsdRst = CheckPsd(loginUser, loginParams);
            if (loginUser != null && checkPsdRst) {
                apiResult.success();
                apiResult.setMsg("认证成功！");
                apiResult.setCode(20000);

                String userId = loginUser.getId().toString();

                Map<String, String> payload = new HashMap<>(2);
                payload.put("id", userId);
                payload.put("loginName", loginUser.getLoginName());
                String token = JWTUtil.getToken(payload);

                LoginResult loginResult = new LoginResult();
                loginResult.setUserId(userId);
                loginResult.setRealName(loginUser.getName());
                loginResult.setUsername(loginUser.getLoginName());

                loginResult.setToken(token);
                loginResult.setAvatar(getAvatar(userId));
                loginResult.setRoles(getRoles(userId));

                //TODO 将token 写入域名下的cookies

                apiResult.setData(loginResult);
            } else {
                apiResult.error();
                apiResult.setMsg("账号或密码不正确");
                return apiResult;
            }

        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
            apiResult.error();
            apiResult.setMsg(exception.getMessage());
        }
        return apiResult;
    }

    private Boolean CheckPsd(User loginUser, LoginParams loginParams) {
        return loginUser.getPassword().equals(accountService.entryptPassword(loginParams.getPassword(), loginUser.getSalt()));
    }

    private ArrayList<LoginRoleInfo> getRoles(String id) {
        ArrayList<LoginRoleInfo> roles = new ArrayList<>();
        roles.add(new LoginRoleInfo("Super Admin", "super"));
        return roles;
    }

    private String getAvatar(String id) {
        return "https://q1.qlogo.cn/g?b=qq&nk=339449197&s=640";
    }

    @RequestMapping(value = "/getUserInfo", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getUserInfo(HttpServletRequest req) {
        try {
            User user = this.getUserByToken(req);
            LoginResult loginResult = new LoginResult();
            loginResult.setUserId(user.getId().toString());
            loginResult.setAvatar(user.getAvatar());
            loginResult.setRoles(null);
            loginResult.setToken(this.getToken(req));
            loginResult.setUsername(user.getLoginName());
            loginResult.setRealName(user.getName());
            loginResult.setHomePath("");
            loginResult.setDesc("");

            return new ApiResult().success().setData(loginResult);
        } catch (Exception e) {
            logger.error("getUserInfo", e);
            return new ApiResult().error().setMsg(e.getMessage());
        }
    }

    @RequestMapping(value = "/getPermCode")
    @ResponseBody
    public ApiResult getPermissionCode(HttpServletRequest req) {
        try {
            User user = this.getUserByToken(req);
            // TODO 改从数据库中获取
            String[] permissionCodes = new String[]{"1000", "3000", "5000"};

            return new ApiResult().success().setData(permissionCodes);
        } catch (Exception e) {
            return new ApiResult().error();
        }
    }

    @RequestMapping(value = "/isLogged")
    @ResponseBody
    public ApiResult isLogged(HttpServletRequest req) {
        ApiResult result = new ApiResult();
        try {
            User user = this.getUserByToken(req);
            user.setSalt("");
            user.setPassword("");
            user.setPlainPassword("");
            return result.setData(accountService.wrapUser(user));
        } catch (Exception e) {
            return result.error();
        }
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult logout(HttpServletRequest req) {
        try {
            User user = this.getUserByToken(req);
            logger.debug("User [" + user.getLoginName() + "] logout.");
            return new ApiResult();
        } catch (Exception e) {
            logger.error("退出失败", e);
            return new ApiResult().error();
        }
    }

    /**
     * 获取当前用户的菜单
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/getMenuList", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult getCurrentUserMenu(HttpServletRequest req) throws Exception {
        ApiResult result = new ApiResult();
        // User user = this.getUserByToken(req);
        List<Map<String, Object>> menuItemList = new ArrayList<>();
        String appId = req.getParameter("appId");
        String tenantCode = req.getParameter("tenantCode");
        if (Strings.isNotBlank(appId) && Strings.isNotBlank(tenantCode)) {
            // 菜单
            Map map = new HashMap<>();
            // map.put("userId", user.getId());
            map.put("flag", req.getParameter("flag"));
            map.put("appId", req.getParameter("appId"));
            map.put("tenantCode", req.getParameter("tenantCode"));
            menuItemList = dao.queryForMapList("select_platform_tree_node_app_page", map);
        }

        return new ApiResult().setData(menuItemList);
    }

    /**
     * 用于管理员重置密码
     *
     * @param passwordLength 默认为8位，最长为32位
     * @return
     */
    @RequestMapping(value = "/resetPassword", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult resetPassword(HttpServletRequest req, @RequestParam(defaultValue = "8", required = false) int passwordLength) throws Exception {
        User user = this.getUserByToken(req);
        String plainPassword = RandomStringUtils.randomAlphanumeric(passwordLength > 32 ? 32 : passwordLength);
        user.setPlainPassword(plainPassword);
        accountService.entryptPassword(user);
        dao.save(user);
        return new ApiResult().setData(plainPassword);
    }


    /**
     * 通过token获取用户信息
     *
     * @param req
     * @return
     * @throws Exception
     */
    private User getUserByToken(HttpServletRequest req) throws Exception {
        return dao.queryForObject(User.class, "loginName", req.getAttribute("loginName"));
    }

    private String getToken(HttpServletRequest req) {
        return req.getHeader("authorization");
    }


}
