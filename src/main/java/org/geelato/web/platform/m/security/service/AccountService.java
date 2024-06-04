package org.geelato.web.platform.m.security.service;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.J2Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.geelato.core.api.ApiResult;
import org.geelato.core.orm.Dao;
import org.geelato.utils.Digests;
import org.geelato.utils.Encodes;
import org.geelato.web.platform.m.base.service.RuleService;
import org.geelato.web.platform.m.security.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by hongxueqian on 14-4-12.
 */
@Component
public class AccountService {
    public static final String HASH_ALGORITHM = "SHA-1";
    public static final int HASH_INTERATIONS = 1024;
    private static final int SALT_SIZE = 8;


    private Dao dao;
    @Autowired
    protected RuleService ruleService;
//    private static AccountService ref;

    private CacheChannel cache = J2Cache.getChannel();


    public User findUserByLoginName(String loginName) {
        return dao.queryForObject(User.class, "loginName", loginName);
    }

    public void setDao(Dao dao) {
        this.dao = dao;
    }

    public void registerUser(User user) {
        entryptPassword(user);
        if (StringUtils.isBlank(user.getName())) {
            user.setName(user.getLoginName());
        }
        dao.save(user);

        //注册之后自动登录
        UsernamePasswordToken token = new UsernamePasswordToken();
        token.setUsername(user.getLoginName());
        token.setPassword(user.getPlainPassword().toCharArray());
        SecurityUtils.getSubject().login(token);

        //更新Shiro中当前用户的用户名.
        SecurityHelper.getCurrentUser().name = user.getName();
    }

    /**
     * 设定安全的密码，生成随机的salt并经过1024次 sha-1 hash
     */
    public User entryptPassword(User user) {
        byte[] salt = Digests.generateSalt(SALT_SIZE);
        user.setSalt(Encodes.encodeHex(salt));

        byte[] hashPassword = Digests.sha1(user.getPlainPassword().getBytes(), salt, HASH_INTERATIONS);
        user.setPassword(Encodes.encodeHex(hashPassword));
        return user;
    }

    public String entryptPassword(String plainPassword, String salt) {
        byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), Encodes.decodeHex(salt), HASH_INTERATIONS);
        return Encodes.encodeHex(hashPassword);
    }

    public Map wrapUser(User user) {
        HashMap map = new HashMap(3);
        map.put("user", user);
        CacheObject userConfigCacheObject = cache.get("config", user.getId().toString(), null);
        HashMap userConfig = new HashMap();
        if (userConfigCacheObject.getValue() != null) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) userConfigCacheObject.getValue();
            list.forEach((item) -> {
                userConfig.put(item.get("code"), item);
            });
        }
        map.put("userConfig", userConfig);

        CacheObject commonConfigCacheObject = cache.get("config", user.getId().toString(), null);
        HashMap commonConfig = new HashMap();
        if (userConfigCacheObject.getValue() != null) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) commonConfigCacheObject.getValue();
            list.forEach((item) -> {
                commonConfig.put(item.get("code"), item);
            });
        }
        map.put("commonConfig", commonConfig);

        List<Map<String, Object>> moduleList = dao.queryForMapList(Module.class);
        for (Map module : moduleList) {
            long id = Long.parseLong(module.get("id").toString());
            ApiResult<List<Map>> result = ruleService.queryForTree("platform_menu_item", id, "items");
            List<Map> menuItemList = result.getData();
            module.put("tree", menuItemList);
        }
        map.put("modules", moduleList);

        return map;
    }

}
