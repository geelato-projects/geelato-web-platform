package org.geelato.web.platform.m.security.service;

import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.enums.AuthCodeAction;
import org.geelato.web.platform.enums.ValidTypeEnum;
import org.geelato.web.platform.m.security.entity.AuthCodeParams;
import org.geelato.web.platform.m.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/17 9:10
 */
@Component
public class AuthCodeService {
    private static final String NUMBERS = "0123456789";
    private static final int LENGTH = 6;
    private static final int CODE_EXPIRATION_TIME = 5;
    private final Logger logger = LoggerFactory.getLogger(AuthCodeService.class);
    @Autowired
    @Qualifier("primaryDao")
    public Dao dao;
    @Autowired
    private EmailService emailService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成六位纯数字，验证码
     *
     * @return
     */
    public static String generateCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        Random random = new Random();
        for (int i = 0; i < LENGTH; i++) {
            int index = random.nextInt(NUMBERS.length());
            char randomChar = NUMBERS.charAt(index);
            sb.append(randomChar);
        }
        return sb.toString();
    }

    /**
     * 验证码，生成
     *
     * @param form
     * @return
     */
    public boolean generate(AuthCodeParams form) throws NoSuchFieldException, IllegalAccessException {
        String redisKey = form.getRedisKey();
        if (Strings.isBlank(redisKey)) {
            return false;
        }
        // 用户
        User user = dao.queryForObject(User.class, form.getUserId());
        Assert.notNull(user, ApiErrorMsg.IS_NULL);
        // 需要查询用户信息的情况
        String[] needUser = {AuthCodeAction.VALIDATEUSER.name()};
        if (Arrays.binarySearch(needUser, form.getAction().toUpperCase(Locale.ENGLISH)) > -1) {
            form.setPrefix(user.getMobilePrefix());
            String label = ValidTypeEnum.getLabel(form.getValidType());
            if (Strings.isNotBlank(label)) {
                Class<?> clazz = user.getClass();
                Field labelField = clazz.getDeclaredField(label);
                labelField.setAccessible(true);
                form.setValidBox((String) labelField.get(user));
            }
        }
        // 验证方式不能为空
        if (Strings.isBlank(form.getValidBox())) {
            return false;
        }
        // 验证码
        String authCode = AuthCodeService.generateCode();
        form.setAuthCode(authCode);
        logger.info("authCode：" + authCode);
        // 发送信息
        boolean sendAuthCode = action(form);
        if (!sendAuthCode) {
            return false;
        }
        // 加密
        String saltCode = form.getRedisValue(authCode);
        if (Strings.isBlank(saltCode)) {
            return false;
        }
        // 存入缓存中
        redisTemplate.opsForValue().set(redisKey, saltCode, CODE_EXPIRATION_TIME, TimeUnit.MINUTES);

        return true;
    }

    public boolean action(AuthCodeParams form) {
        String action = form.getAction().toUpperCase(Locale.ENGLISH);
        if (ValidTypeEnum.MOBILE.getValue().equals(form.getValidType())) {
            return sendMobile();
        } else if (ValidTypeEnum.MAIL.getValue().equals(form.getValidType())) {
            sendEmail(form.getValidBox(), "Geelato Admin AuthCode", form.getAuthCode());
        }

        return true;
    }

    public void sendEmail(String to, String subject, String authCode) {
        String text = String.format("本次验证码是 %s，请在 %d 分钟内输入验证码进行下一步操作。", authCode, CODE_EXPIRATION_TIME);
        emailService.sendHtmlMail(to, subject, text);
    }

    public boolean sendMobile() {
        return true;
    }

    /**
     * 验证码，验证
     *
     * @param form
     * @return
     */
    public boolean validate(AuthCodeParams form) {
        String redisKey = form.getRedisKey();
        if (Strings.isBlank(redisKey) || Strings.isBlank(form.getAuthCode())) {
            return false;
        }
        // 加密
        String saltCode = form.getRedisValue();
        // 获取缓存
        String redisCode = (String) redisTemplate.opsForValue().get(redisKey);
        logger.info("redisKey-redisCode: " + redisKey + "[" + redisCode + "]");
        return saltCode.equals(redisCode);
    }


}
