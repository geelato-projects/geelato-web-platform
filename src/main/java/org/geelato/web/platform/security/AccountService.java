package org.geelato.web.platform.security;

import org.geelato.core.orm.Dao;
import org.geelato.web.platform.entity.security.User;
import org.geelato.utils.Digests;
import org.geelato.utils.Encodes;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by hongxueqian on 14-4-12.
 */
@Component
public class AccountService {
    public static final String HASH_ALGORITHM = "SHA-1";
    public static final int HASH_INTERATIONS = 1024;
    private static final int SALT_SIZE = 8;
    @Autowired
    private Dao dao;

    public User findUserByLoginName(String loginName) {
        return dao.queryForObject(User.class, "loginName", loginName);
    }

    public void registerUser(User user) {
        entryptPassword(user);
        if (StringUtils.isBlank(user.getName())) user.setName(user.getLoginName());
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
    private void entryptPassword(User user) {
        byte[] salt = Digests.generateSalt(SALT_SIZE);
        user.setSalt(Encodes.encodeHex(salt));

        byte[] hashPassword = Digests.sha1(user.getPlainPassword().getBytes(), salt, HASH_INTERATIONS);
        user.setPassword(Encodes.encodeHex(hashPassword));
    }
}
