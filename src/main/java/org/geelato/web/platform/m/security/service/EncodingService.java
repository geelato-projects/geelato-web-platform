package org.geelato.web.platform.m.security.service;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.core.util.UUIDUtils;
import org.geelato.web.platform.enums.EncodingItemTypeEnum;
import org.geelato.web.platform.enums.EncodingSerialTypeEnum;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.security.entity.Encoding;
import org.geelato.web.platform.m.security.entity.EncodingItem;
import org.geelato.web.platform.m.security.entity.EncodingLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/8/2 11:02
 */
@Component
public class EncodingService extends BaseService {
    private static final String ENCODING_LOCK_PREFIX = "ENCODING_LOCK";
    private static final String ENCODING_LIST_PREFIX = "ENCODING_LIST";
    private static final String ENCODING_ITEM_PREFIX = "ENCODING_ITEM_";
    private final Logger logger = LoggerFactory.getLogger(EncodingService.class);
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void redisTemplateEncodingUpdate(Encoding encoding) {
        encoding.afterSet();
        String redisItemKey = ENCODING_ITEM_PREFIX + encoding.getId();
        List<Object> redisItemKeys = redisTemplate.opsForList().range(ENCODING_LIST_PREFIX, 0, -1);
        // 设置缓存
        if (redisItemKeys == null || redisItemKeys.isEmpty()) {
            redisItemKeys = redisTemplateEncoding();
        }
        // 清理
        if (redisItemKeys.contains(redisItemKey)) {
            redisTemplate.delete(redisItemKey);
            redisTemplate.opsForList().remove(ENCODING_LIST_PREFIX, 1, redisItemKey);
        }
        // 重新获取
        redisTemplateEncodingItem(encoding);
    }

    private void redisTemplateEncodingItem(Encoding encoding) {
        String redisItemKey = ENCODING_ITEM_PREFIX + encoding.getId();
        List<Object> serials = querySerialsByEncodingLog(encoding);
        logger.info(redisItemKey + " 流水号：" + JSON.toJSONString(serials));
        redisTemplateListRightPush(redisItemKey, serials);
        redisTemplate.expire(redisItemKey, timeInterval(encoding.getDateType()), TimeUnit.SECONDS);
    }

    public List<Object> redisTemplateEncoding() {
        List<Object> redisItemKeys = redisTemplate.opsForList().range(ENCODING_LIST_PREFIX, 0, -1);
        if (redisItemKeys == null || redisItemKeys.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            params.put("enableStatus", EnableStatusEnum.ENABLED.getCode());
            List<Encoding> encodingList = queryModel(Encoding.class, params);
            for (Encoding encoding : encodingList) {
                encoding.afterSet();
                String redisItemKey = ENCODING_ITEM_PREFIX + encoding.getId();
                redisTemplateEncodingItem(encoding);
                redisItemKeys.add(redisItemKey);
            }
            if (!redisItemKeys.isEmpty()) {
                logger.info("编码模板：" + JSON.toJSONString(redisItemKeys));
                redisTemplateListRightPush(ENCODING_LIST_PREFIX, redisItemKeys);
                redisTemplate.expire(ENCODING_LIST_PREFIX, 1, TimeUnit.DAYS);
            }
        }

        return redisItemKeys;
    }

    /**
     * 编码记录，查询流水号
     *
     * @param encoding
     * @return
     */
    public List<Object> querySerialsByEncodingLog(Encoding encoding) {
        List<Object> serials = new ArrayList<>();
        // 查询日志
        Map<String, Object> logParams = new HashMap<>();
        logParams.put("encodingId", encoding.getId());
        logParams.put("template", encoding.getFormatExample());
        logParams.put("enableStatus", EnableStatusEnum.ENABLED.getCode());
        if (Strings.isNotBlank(encoding.getDateType())) {
            logParams.put("exampleDate", new SimpleDateFormat(encoding.getDateType()).format(new Date()));
        }
        logger.info("logParams：" + JSON.toJSONString(logParams));
        List<EncodingLog> encodingLogList = queryModel(EncodingLog.class, logParams);
        if (encodingLogList != null && !encodingLogList.isEmpty()) {
            for (EncodingLog log : encodingLogList) {
                if (Strings.isNotBlank(log.getExampleSerial())) {
                    serials.add(log.getExampleSerial());
                }
            }
            serials = formatSerialList(serials);
        }
        return serials;
    }

    /**
     * 编码实例生成
     *
     * @param form
     * @return
     */
    public String generate(Encoding form) {
        redisTemplateEncoding();
        if (Strings.isBlank(form.getTemplate())) {
            return null;
        }
        List<EncodingItem> itemList = JSON.parseArray(form.getTemplate(), EncodingItem.class);
        if (itemList == null || itemList.isEmpty()) {
            return null;
        }
        form.afterSet();
        String redisItemKey = ENCODING_ITEM_PREFIX + form.getId();
        // 记录
        EncodingLog encodingLog = new EncodingLog();
        encodingLog.setEncodingId(form.getId());
        encodingLog.setEnableStatus(EnableStatusEnum.ENABLED.getCode());
        encodingLog.setTemplate(form.getFormatExample());
        // 编码实例
        List<String> examples = new ArrayList<>();
        for (EncodingItem item : itemList) {
            if (EncodingItemTypeEnum.CONSTANT.getValue().equals(item.getItemType())) {
                // 常量
                if (Strings.isNotBlank(item.getConstantValue())) {
                    examples.add(item.getConstantValue());
                }
            } else if (EncodingItemTypeEnum.SERIAL.getValue().equals(item.getItemType())) {
                // 序列号
                String serial = getSerialByRedisLock(redisItemKey, item);
                if (Strings.isBlank(serial)) {
                    throw new RuntimeException(ApiErrorMsg.SERIAL_USE_UP);
                }
                encodingLog.setExampleSerial(serial);
                examples.add(serial);
            } else if (EncodingItemTypeEnum.DATE.getValue().equals(item.getItemType())) {
                // 日期
                if (Strings.isNotBlank(item.getDateType())) {
                    try {
                        String date = new SimpleDateFormat(item.getDateType()).format(new Date());
                        examples.add(date);
                        encodingLog.setExampleDate(date);
                    } catch (Exception ex) {
                        logger.error("日期解析失败", ex);
                    }
                }
            }
        }
        String separator = Strings.isNotBlank(form.getSeparators()) ? form.getSeparators() : "";
        encodingLog.setExample(String.join(separator, examples));
        logger.info(redisItemKey + " 记录：" + JSON.toJSONString(encodingLog));
        dao.save(encodingLog);
        if (Strings.isNotBlank(encodingLog.getExampleSerial())) {
            redisTemplate.opsForList().rightPush(redisItemKey, encodingLog.getExampleSerial());
        }

        return encodingLog.getExample();
    }

    private String getSerialByRedisLock(String redisItemKey, EncodingItem item) {
        //获取锁 加上uuid防止误删除锁
        String uuid = System.currentTimeMillis() + UUID.randomUUID().toString().replaceAll("-", "");
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(ENCODING_LOCK_PREFIX, uuid, 10, TimeUnit.SECONDS);
        //如果获取到锁执行步骤 最后释放锁
        String serial = null;
        if (lock) {
            if (EncodingSerialTypeEnum.ORDER.getValue().equals(item.getSerialType())) {
                // 顺序
                serial = getOrderSerial(redisItemKey, item.getSerialDigit());
            } else if (EncodingSerialTypeEnum.RANDOM.getValue().equals(item.getSerialType())) {
                // 随机
                serial = getRandomSerial(redisItemKey, item.getSerialDigit());
            }
            //在极端情况下仍然会误删除锁
            //因此使用lua脚本的方式来防止误删除
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                    "then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            DefaultRedisScript defaultRedisScript = new DefaultRedisScript();
            defaultRedisScript.setScriptText(script);
            defaultRedisScript.setResultType(Long.class);
            redisTemplate.execute(defaultRedisScript, List.of(ENCODING_LOCK_PREFIX), uuid);
        } else {
            //如果没有获取到锁 重试
            try {
                Thread.sleep(100);
                serial = getSerialByRedisLock(redisItemKey, item);
            } catch (InterruptedException e) {
                logger.error("redisLockError", e);
            }
        }

        return serial;
    }

    /**
     * 顺序流水号
     *
     * @param redisItemKey
     * @param serialDigit
     * @return
     */
    private String getOrderSerial(String redisItemKey, int serialDigit) {
        List<Object> redisSerials = redisTemplate.opsForList().range(redisItemKey, 0, -1);
        if (redisSerials == null || redisSerials.isEmpty()) {
            return String.format("%0" + serialDigit + "d", 1);
        }
        redisSerials = formatSerialList(redisSerials);
        long max = Long.parseLong(String.valueOf(redisSerials.get(redisSerials.size() - 1)));
        long radius = Long.parseLong(UUIDUtils.generateFixation(serialDigit, 9));
        return radius > max ? String.format("%0" + serialDigit + "d", max + 1) : null;
    }

    /**
     * 随机流水号
     *
     * @param redisItemKey
     * @param serialDigit
     * @return
     */
    private String getRandomSerial(String redisItemKey, int serialDigit) {
        List<Object> redisSerials = redisTemplate.opsForList().range(redisItemKey, 0, -1);
        String serial = null;
        long radius = Long.parseLong(UUIDUtils.generateFixation(serialDigit, 9));
        for (int i = 0; i < radius; i++) {
            serial = UUIDUtils.generateRandom(serialDigit);
            if (Strings.isNotBlank(serial) && !redisSerials.contains(serial)) {
                break;
            }
            serial = null;
        }
        return serial;
    }

    /**
     * 时间间隔
     *
     * @param dateType（年、月、日）
     * @return
     */
    private long timeInterval(String dateType) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0); // 将秒数设置为0
        calendar.set(Calendar.MILLISECOND, 0); // 将毫秒数设置为0
        if (Arrays.asList(new String[]{"yyyy", "yy"}).contains(dateType)) {
            calendar.add(Calendar.YEAR, 1); // 将当前时间加上一年
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 将天数设置为1,表示下个月的第一天
            calendar.set(Calendar.HOUR_OF_DAY, 0); // 将小时数设置为0,表示当天的零点
            calendar.set(Calendar.MINUTE, 0); // 将分钟数设置为0
        } else if (Arrays.asList(new String[]{"yyyyMM", "yyMM"}).contains(dateType)) {
            calendar.add(Calendar.MONTH, 1); // 将当前时间加上一个月
            calendar.set(Calendar.DAY_OF_MONTH, 1); // 将天数设置为1,表示下个月的第一天
            calendar.set(Calendar.HOUR_OF_DAY, 0); // 将小时数设置为0,表示当天的零点
            calendar.set(Calendar.MINUTE, 0); // 将分钟数设置为0
        } else if (Arrays.asList(new String[]{"yyyyMMdd", "yyMMdd"}).contains(dateType)) {
            calendar.add(Calendar.HOUR_OF_DAY, 24); // 将当前时间加上一天
        } else {
            return -1;
        }

        Date tonight = calendar.getTime(); // 获取今天晚上的时间
        long diff = tonight.getTime() - System.currentTimeMillis(); // 计算时间差(毫秒)
        return diff / 1000;
    }

    /**
     * 删除redis，再重新批量添加
     *
     * @param key
     * @param list
     */
    private void redisTemplateListRightPush(String key, List<Object> list) {
        redisTemplate.delete(key);
        if (Strings.isNotBlank(key) && list != null) {
            for (Object item : list) {
                redisTemplate.opsForList().rightPush(key, item);
            }
        }
    }

    /**
     * 集合格式化
     *
     * @param list
     * @return
     */
    private <T> List<T> formatList(List<T> list) {
        List<T> nList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                T obj = list.get(i);
                if (obj != null && Strings.isNotBlank(obj.toString())) {
                    if (!nList.contains(obj)) {
                        nList.add(obj);
                    }
                }
            }
        }

        return nList;
    }

    /**
     * 流水号集合排序
     *
     * @param list
     * @return
     */
    private List<Object> formatSerialList(List<Object> list) {
        if (list != null && !list.isEmpty()) {
            // 去重、去空
            list = formatList(list);
            // 排序
            list.sort(new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    return Long.parseLong(String.valueOf(o1)) > Long.parseLong(String.valueOf(o2)) ? 1 : 0;
                }
            });
        }

        return list;
    }
}
