package org.geelato.web.platform.m.security.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.meta.annotation.IgnoreJWTVerify;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.security.entity.*;
import org.geelato.web.platform.m.security.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hongxq on 2022/10/1
 */
@Controller
@RequestMapping(value = {"/api/sys"})
public class SystemRestController extends BaseController {

    @Autowired
    @Qualifier("primaryDao")
    private Dao dao;

    @Autowired
    protected AccountService accountService;

    private Logger logger = LoggerFactory.getLogger(SystemRestController.class);


    @IgnoreJWTVerify
    @RequestMapping(value = "/getRoleListByPage", method = RequestMethod.GET, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public ApiPagedResult getAccountList(HttpServletRequest req) {
        //初始化返回值
        ApiPagedResult apiPageResult = new ApiPagedResult<DataItems>();
        List mapList = dao.queryForMapList(Role.class);
        apiPageResult.setData(new DataItems(mapList, mapList.size()));
        apiPageResult.success();
        apiPageResult.setTotal(mapList.size());
        return apiPageResult;
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
        User user = this.getUserByToken(req);
        // 菜单
        Map map = new HashMap<>();
        map.put("userId", user.getId());
        List<Map<String, Object>> menuItemList = dao.queryForMapList("select_platform_menu", map);
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
//        String token = this.getToken(req);
        //验证令牌  如果令牌不正确会出现异常 被全局异常处理
//        DecodedJWT verify = JWTUtil.verify(token);
//        String loginName = verify.getClaim("loginName").asString();

        return dao.queryForObject(User.class, "loginName", req.getAttribute("loginName"));
    }

    private String getToken(HttpServletRequest req) {
        return req.getHeader("authorization");
    }

//    @Override
//    public void afterPropertiesSet() throws Exception {
//        super.afterPropertiesSet();
//        accountService.setDao(this.dao);
//    }

}
