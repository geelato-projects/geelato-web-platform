package org.geelato.web.platform.m.security.service;

import jakarta.annotation.Resource;
import org.apache.commons.collections4.map.HashedMap;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.orm.Dao;
import org.geelato.utils.StringUtils;
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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author diabl
 */
@Component
public class AuthCodeService {
    private static final String NUMBERS = "0123456789";
    private static final int LENGTH = 6;
    private static final int CODE_EXPIRATION_TIME = 5;
    private static final String CONFIG_KEY_TEMPLATE_CODE = "mobileTemplateAutoCode";
    private final Logger logger = LoggerFactory.getLogger(AuthCodeService.class);
    @Autowired
    @Qualifier("primaryDao")
    public Dao dao;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AliMobileService aliMobileService;
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
        if (StringUtils.isBlank(redisKey)) {
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
            if (StringUtils.isNotBlank(label)) {
                Class<?> clazz = user.getClass();
                Field labelField = clazz.getDeclaredField(label);
                labelField.setAccessible(true);
                form.setValidBox((String) labelField.get(user));
            }
        }
        // 验证方式不能为空
        if (StringUtils.isBlank(form.getValidBox())) {
            return false;
        }
        // 验证码
        String authCode = AuthCodeService.generateCode();
        form.setAuthCode(authCode);
        logger.info("authCode：" + authCode);
        // 加密
        String saltCode = form.getRedisValue(authCode);
        if (StringUtils.isBlank(saltCode)) {
            return false;
        }
        // 发送信息
        boolean sendAuthCode = action(form);
        if (!sendAuthCode) {
            return false;
        }
        // 存入缓存中
        redisTemplate.opsForValue().set(redisKey, saltCode, CODE_EXPIRATION_TIME, TimeUnit.MINUTES);

        return true;
    }

    public boolean action(AuthCodeParams form) {
        String action = form.getAction().toUpperCase(Locale.ENGLISH);
        if (ValidTypeEnum.MOBILE.getValue().equals(form.getValidType())) {
            return sendMobile(form.getPrefix(), form.getValidBox(), form.getAuthCode());
        } else if (ValidTypeEnum.MAIL.getValue().equals(form.getValidType())) {
            return sendEmail(form.getValidBox(), "Geelato Admin AuthCode", form.getAuthCode());
        }
        return true;
    }

    public boolean sendEmail(String to, String subject, String authCode) {
        String text = String.format("本次验证码是 %s，请在 %d 分钟内输入验证码进行下一步操作。", authCode, CODE_EXPIRATION_TIME);
        return emailService.sendHtmlMail(to, subject, text);
    }

    public boolean sendMobile(String mobilePrefix, String mobilePhone, String authCode) {
        String phoneNumbers = mobilePhone;
        if (StringUtils.isNotBlank(mobilePrefix) && !"+86".equals(mobilePrefix)) {
            phoneNumbers = mobilePrefix + phoneNumbers;
        }
        try {
            Map<String, Object> params = new HashedMap<>();
            params.put("code", authCode);
            return aliMobileService.sendMobile(CONFIG_KEY_TEMPLATE_CODE, phoneNumbers, params);
        } catch (Exception e) {
            logger.error("发送短信时发生异常", e);
        }
        return false;
    }

    /**
     * 验证码，验证
     *
     * @param form
     * @return
     */
    public boolean validate(AuthCodeParams form) {
        String redisKey = form.getRedisKey();
        if (StringUtils.isBlank(redisKey) || StringUtils.isBlank(form.getAuthCode())) {
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
