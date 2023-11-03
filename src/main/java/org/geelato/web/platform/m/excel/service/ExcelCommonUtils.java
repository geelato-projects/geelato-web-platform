package org.geelato.web.platform.m.excel.service;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.exception.file.FileException;
import org.geelato.web.platform.m.base.entity.Dict;
import org.geelato.web.platform.m.base.entity.DictItem;
import org.geelato.web.platform.m.base.service.RuleService;
import org.geelato.web.platform.m.excel.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/10/27 9:53
 */
@Component
public class ExcelCommonUtils {
    private static final int REDIS_TIME_OUT = 60;
    private static final int GGL_QUERY_TOTAL = 10000;
    private final FilterGroup filterGroup = new FilterGroup().addFilter(ColumnDefault.DEL_STATUS_FIELD, String.valueOf(DeleteStatusEnum.NO.getCode()));
    private final Logger logger = LoggerFactory.getLogger(ExcelCommonUtils.class);
    private final MetaManager metaManager = MetaManager.singleInstance();
    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;
    @Autowired
    @Qualifier("secondaryDao")
    protected Dao dao2;
    @Autowired
    protected RuleService ruleService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 为空，抛出异常
     *
     * @param object
     * @param fileException
     * @param <T>
     * @throws T
     */
    public static <T extends FileException> void notNull(@Nullable Object object, T fileException) throws T {
        if (object == null) {
            throw fileException;
        }
    }

    /**
     * 获取表格默认字段
     *
     * @return
     */
    public List<String> getDefaultColumns() {
        List<String> columnNames = new ArrayList<>();
        List<ColumnMeta> columnMetaList = metaManager.getDefaultColumn();
        if (columnMetaList != null && columnMetaList.size() > 0) {
            for (ColumnMeta columnMeta : columnMetaList) {
                if (!columnNames.contains(columnMeta.getName())) {
                    columnNames.add(columnMeta.getName());
                }
            }
        }

        return columnNames;
    }


    /**
     * 解析业务数据类型，规则
     *
     * @param rules
     * @return
     */
    public Set<BusinessTypeRuleData> readBusinessTypeRuleData(String rules) {
        Set<BusinessTypeRuleData> typeRuleDataSet = new LinkedHashSet<>();
        if (Strings.isNotBlank(rules)) {
            List<BusinessTypeRuleData> typeRuleDataList = com.alibaba.fastjson.JSON.parseArray(rules, BusinessTypeRuleData.class);
            if (typeRuleDataList != null && typeRuleDataList.size() > 0) {
                typeRuleDataList.sort(new Comparator<BusinessTypeRuleData>() {
                    @Override
                    public int compare(BusinessTypeRuleData o1, BusinessTypeRuleData o2) {
                        return o1.getOrder() - o2.getOrder();
                    }
                });
                for (BusinessTypeRuleData ruleData : typeRuleDataList) {
                    typeRuleDataSet.add(ruleData);
                }
            }
        }

        return typeRuleDataSet;
    }

    /**
     * 导入数据，多值数据处理
     *
     * @param businessDataMapList
     * @return
     */
    public List<Map<String, BusinessData>> handleBusinessDataMultiScene(List<Map<String, BusinessData>> businessDataMapList) {
        List<Map<String, BusinessData>> handleDataMapList = new ArrayList<>();
        Set<Map<String, Object>> multiLoggers = new LinkedHashSet<>();
        if (businessDataMapList != null && businessDataMapList.size() > 0) {
            for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
                // 分类
                Map<String, BusinessData> singleData = new HashMap<>();
                Map<String, BusinessData> multiData = new LinkedHashMap<>();
                Map<String, BusinessData> symData = new HashMap<>();
                int maxLength = 0;
                for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                    BusinessData businessData = businessDataEntry.getValue();
                    BusinessTypeData typeData = businessData.getBusinessTypeData();
                    boolean isMulti = false;
                    if (typeData.isMulti()) {
                        if (businessData.getValue() != null) {
                            try {
                                String[] multiValue = String.valueOf(businessData.getValue()).split(typeData.getMultiSeparator());
                                Map<String, Object> multiLogger = new LinkedHashMap<>();
                                multiLogger.put("index", String.format("y.%s,x.%s", businessData.getYIndex(), businessData.getXIndex()));
                                multiLogger.put("separator", typeData.getMultiSeparator());
                                multiLogger.put("scene", typeData.getMultiScene());
                                multiLogger.put("cellValue", businessData.getValue());
                                multiLogger.put("formatValue", multiValue);
                                multiLoggers.add(multiLogger);
                                if (multiValue != null && multiValue.length > 0) {
                                    for (int i = 0; i < multiValue.length; i++) {
                                        multiValue[i] = Strings.isNotBlank(multiValue[i]) ? multiValue[i].trim() : "";
                                    }
                                    businessData.setMultiValue(multiValue);
                                    if (typeData.isSceneTypeMulti()) {
                                        isMulti = true;
                                        multiData.put(businessDataEntry.getKey(), businessData);
                                    } else if (typeData.isSceneTypeSym()) {
                                        isMulti = true;
                                        symData.put(businessDataEntry.getKey(), businessData);
                                        maxLength = maxLength > multiValue.length ? maxLength : multiValue.length;
                                    }
                                }
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                            }
                        }
                    }
                    if (!isMulti) {
                        singleData.put(businessDataEntry.getKey(), businessData);
                    }
                }
                // 对乘值处理
                List<Map<String, BusinessData>> multiMapList = cartesianProduct(multiData);
                // 对称值处理
                List<Map<String, BusinessData>> symMapList = new ArrayList<>();
                if (!symData.isEmpty()) {
                    for (int i = 0; i < maxLength; i++) {
                        Map<String, BusinessData> symMap = new HashMap<>();
                        for (Map.Entry<String, BusinessData> businessDataEntry : symData.entrySet()) {
                            BusinessData businessData = businessDataEntry.getValue();
                            BusinessData data = new BusinessData();
                            data.setXIndex(businessData.getXIndex());
                            data.setYIndex(businessData.getYIndex());
                            String[] multiValue = businessData.getMultiValue();
                            data.setValue(multiValue[i < multiValue.length ? i : multiValue.length - 1]);
                            data.setPrimevalValue(data.getValue());
                            data.setBusinessTypeData(businessData.getBusinessTypeData());
                            data.setErrorMsgs(businessData.getErrorMsg());
                            symMap.put(businessDataEntry.getKey(), data);
                        }
                        symMapList.add(symMap);
                    }
                }

                List<Map<String, BusinessData>> mergeData = mergeBusinessData(singleData, multiMapList, symMapList);
                handleDataMapList.addAll(mergeData);
            }
        }
        // logger.info(JSON.toJSONString(multiLoggers));

        return handleDataMapList;
    }

    /**
     * 导入数据，多值数据处理
     *
     * @param singleData   单值数据
     * @param multiMapList 多值数据，相乘
     * @param symMapList   多值数据，对称
     * @return
     */
    private List<Map<String, BusinessData>> mergeBusinessData(Map<String, BusinessData> singleData, List<Map<String, BusinessData>> multiMapList, List<Map<String, BusinessData>> symMapList) {
        List<Map<String, BusinessData>> mergeData = new ArrayList<>();
        if (multiMapList != null && multiMapList.size() > 0) {
            if (symMapList != null && symMapList.size() > 0) {
                for (Map<String, BusinessData> multiMap : multiMapList) {
                    for (Map<String, BusinessData> symMap : symMapList) {
                        Map<String, BusinessData> map = new HashMap<>();
                        map.putAll(multiMap);
                        map.putAll(symMap);
                        map.putAll(singleData);
                        mergeData.add(map);
                    }
                }
            } else {
                for (Map<String, BusinessData> map : multiMapList) {
                    map.putAll(singleData);
                    mergeData.add(map);
                }
            }
        } else {
            if (symMapList != null && symMapList.size() > 0) {
                for (Map<String, BusinessData> map : symMapList) {
                    map.putAll(singleData);
                    mergeData.add(map);
                }
            } else {
                mergeData.add(singleData);
            }
        }

        return mergeData;
    }

    /**
     * 导入数据，多值数据处理
     *
     * @param multiData 多值数据，相乘
     * @return
     */
    private List<Map<String, BusinessData>> cartesianProduct(Map<String, BusinessData> multiData) {
        List<Map<String, BusinessData>> mapList = new ArrayList<>();
        Set<String[]> valueSet = new LinkedHashSet<>();
        Set<String> keySet = new LinkedHashSet<>();
        for (Map.Entry<String, BusinessData> map : multiData.entrySet()) {
            keySet.add(map.getKey());
            valueSet.add(map.getValue().getMultiValue());
        }
        Set<String[]> result = cartesianProductHelper(valueSet.toArray(new String[][]{}));
        for (String[] arr : result) {
            Map<String, BusinessData> map = new HashMap<>();
            Iterator<String> iterator = keySet.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                String key = iterator.next();
                BusinessData businessData = multiData.get(key);
                BusinessData data = new BusinessData();
                data.setXIndex(businessData.getXIndex());
                data.setYIndex(businessData.getYIndex());
                data.setValue(arr[count++]);
                data.setPrimevalValue(data.getValue());
                data.setBusinessTypeData(businessData.getBusinessTypeData());
                data.setErrorMsgs(businessData.getErrorMsg());
                map.put(key, data);
            }
            mapList.add(map);
        }

        return mapList;
    }

    /**
     * 递归
     *
     * @param arrays
     * @return
     */
    private Set<String[]> cartesianProductHelper(String[][] arrays) {
        Set<String[]> result = new LinkedHashSet<>();
        cartesianProductHelper(arrays, 0, new String[arrays.length], result);
        return result;
    }

    /**
     * 递归，计算组合
     *
     * @param arrays
     * @param index
     * @param current
     * @param result
     */
    private void cartesianProductHelper(String[][] arrays, int index, String[] current, Set<String[]> result) {
        if (index == arrays.length) {
            result.add(current.clone());
            return;
        }

        for (String element : arrays[index]) {
            current[index] = element;
            cartesianProductHelper(arrays, index + 1, current, result);
        }
    }

    /**
     * 数据处理
     *
     * @param currentUUID
     * @param businessDataMapList
     * @param priorityMulti       是否优先于多值处理
     * @return
     */
    public List<Map<String, BusinessData>> handleBusinessDataRule(String currentUUID, List<Map<String, BusinessData>> businessDataMapList, boolean priorityMulti) {
        if (businessDataMapList != null && businessDataMapList.size() > 0) {
            // 设置缓存
            List<String> cacheKeys = setCache(currentUUID, businessDataMapList, priorityMulti);
            // 数据处理
            for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
                for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                    BusinessData businessData = businessDataEntry.getValue();
                    if (businessData.getValue() == null || Strings.isBlank(String.valueOf(businessData.getValue()))) {
                        continue;
                    }
                    BusinessTypeData typeData = businessData.getBusinessTypeData();
                    Set<BusinessTypeRuleData> typeRuleDataSet = typeData.getTypeRuleData();
                    if (typeRuleDataSet != null && typeRuleDataSet.size() > 0) {
                        for (BusinessTypeRuleData ruleData : typeRuleDataSet) {
                            // 执行优先多值 + 优先多值的规则
                            if (priorityMulti != ruleData.isPriority()) {
                                continue;
                            }
                            try {
                                Object newValue = null;
                                String oldValue = String.valueOf(businessData.getValue());
                                if (ruleData.isRuleTypeDeletes()) {
                                    if (Strings.isNotBlank(ruleData.getRule())) {
                                        newValue = oldValue.replaceAll(ruleData.getRule(), "");
                                    } else {
                                        businessData.setErrorMsg("Rule resolution failure。[Deletes] Rule is empty！");
                                    }
                                } else if (ruleData.isRuleTypeReplace()) {
                                    if (Strings.isNotBlank(ruleData.getRule()) && Strings.isNotBlank(ruleData.getGoal())) {
                                        newValue = oldValue.replaceAll(ruleData.getRule(), ruleData.getGoal());
                                    } else {
                                        businessData.setErrorMsg("Rule resolution failure。[" + ruleData.getType() + "] Rule or Goal is empty！");
                                    }
                                } else if (ruleData.isRuleTypeDictionary()) {
                                    if (Strings.isNotBlank(ruleData.getRule())) {
                                        Map<String, String> redisValues = (Map<String, String>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, ruleData.getRule()));
                                        if (redisValues != null && redisValues.size() > 0) {
                                            newValue = redisValues.get(oldValue);
                                        }
                                    } else {
                                        businessData.setErrorMsg("Rule resolution failure。[" + ruleData.getType() + "] Rule is empty！");
                                    }
                                } else if (ruleData.isRuleTypeQueryGoal()) {
                                    String tableName = ruleData.getQueryRuleTable();
                                    List<String> columnNames = ruleData.getQueryRuleColumn();
                                    if (Strings.isNotBlank(tableName) && columnNames != null && columnNames.size() > 0 && Strings.isNotBlank(ruleData.getGoal())) {
                                        Map<String, Object> redisValues = (Map<String, Object>) redisTemplate.opsForValue().get(String.format("%s:%s,%s", currentUUID, ruleData.getRule(), ruleData.getGoal()));
                                        if (redisValues != null && redisValues.size() > 0) {
                                            newValue = redisValues.get(oldValue);
                                        }
                                    } else {
                                        businessData.setErrorMsg("Rule resolution failure。[" + ruleData.getType() + "] Rule is Error Or Goal is Empty！");
                                    }
                                } else if (ruleData.isRuleTypeQueryRule()) {
                                    String tableName = ruleData.getQueryRuleTable();
                                    List<String> columnNames = ruleData.getQueryRuleColumn();
                                    if (Strings.isNotBlank(tableName) && columnNames != null && columnNames.size() > 0 && Strings.isNotBlank(ruleData.getGoal())) {
                                        Map<String, Object> redisValues = (Map<String, Object>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, ruleData.getRule()));
                                        if (redisValues != null && redisValues.size() > 0) {
                                            newValue = redisValues.get(oldValue);
                                        }
                                    } else {
                                        businessData.setErrorMsg("Rule resolution failure。[" + ruleData.getType() + "] Rule is Error Or Goal is Empty！");
                                    }
                                } else {
                                    newValue = businessData.getValue();
                                }
                                if (newValue != null) {
                                    logger.info(String.format("数据清洗[Y.%s,X.%s]，%s => %s", businessData.getYIndex(), businessData.getXIndex(), businessData.getValue(), newValue));
                                }
                                businessData.setValue(newValue);
                            } catch (Exception ex) {
                                businessData.setErrorMsg("Rule resolution failure。" + JSON.toJSONString(ruleData));
                            }
                        }
                    }
                }
            }
            // 清理缓存
            redisTemplate.delete(cacheKeys);
        }

        return businessDataMapList;
    }

    private List<String> setCache(String currentUUID, List<Map<String, BusinessData>> businessDataMapList, boolean priorityMulti) {
        List<String> cacheList = new ArrayList<>();
        // 类型
        Map<String, BusinessTypeRuleData> ruleDataDict = new HashMap<>();
        Map<String, BusinessTypeRuleData> ruleDataGoal = new HashMap<>();
        Map<String, BusinessTypeRuleData> ruleDataRule = new HashMap<>();
        // 数据解析
        if (businessDataMapList != null && businessDataMapList.size() > 0) {
            for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
                for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                    BusinessData businessData = businessDataEntry.getValue();
                    BusinessTypeData typeData = businessData.getBusinessTypeData();
                    Set<BusinessTypeRuleData> typeRuleDataSet = typeData.getTypeRuleData();
                    if (typeRuleDataSet != null && typeRuleDataSet.size() > 0) {
                        for (BusinessTypeRuleData ruleData : typeRuleDataSet) {
                            // 执行优先多值 + 优先多值的规则
                            if (priorityMulti != ruleData.isPriority()) {
                                continue;
                            }
                            if (ruleData.isRuleTypeDictionary()) {
                                if (Strings.isNotBlank(ruleData.getRule())) {
                                    String key = String.format("%s:%s", currentUUID, ruleData.getRule());
                                    if (!ruleDataDict.containsKey(key)) {
                                        ruleDataDict.put(key, ruleData);
                                    }
                                }
                            } else if (ruleData.isRuleTypeQueryGoal()) {
                                String tableName = ruleData.getQueryRuleTable();
                                List<String> columnNames = ruleData.getQueryRuleColumn();
                                if (Strings.isNotBlank(ruleData.getGoal()) && Strings.isNotBlank(tableName) && columnNames != null && columnNames.size() > 0) {
                                    String key = String.format("%s:%s,%s", currentUUID, ruleData.getRule(), ruleData.getGoal());
                                    if (!ruleDataGoal.containsKey(key)) {
                                        ruleDataGoal.put(key, ruleData);
                                    }
                                }
                            } else if (ruleData.isRuleTypeQueryRule()) {
                                String tableName = ruleData.getQueryRuleTable();
                                List<String> columnNames = ruleData.getQueryRuleColumn();
                                if (Strings.isNotBlank(ruleData.getGoal()) && Strings.isNotBlank(tableName) && columnNames != null && columnNames.size() > 0) {
                                    String key = String.format("%s:%s", currentUUID, ruleData.getRule());
                                    if (!ruleDataRule.containsKey(key)) {
                                        ruleDataRule.put(key, ruleData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // 字典查询
        List<String> dictKeys = setDictRuleRedis(currentUUID, ruleDataDict);
        cacheList.addAll(dictKeys);
        // 目标字段查询
        List<String> goalRedis = setQueryRuleRedis(ruleDataGoal);
        cacheList.addAll(goalRedis);
        // 规则字段查询
        List<String> ruleRedis = setQueryRuleRedis(ruleDataRule);
        cacheList.addAll(ruleRedis);

        return cacheList;
    }

    /**
     * 数据字典缓存
     *
     * @param currentUUID
     * @param ruleDataMap
     * @return
     */
    private List<String> setDictRuleRedis(String currentUUID, Map<String, BusinessTypeRuleData> ruleDataMap) {
        List<String> dictKeys = new ArrayList<>();
        if (ruleDataMap != null && ruleDataMap.size() > 0) {
            // 所有字典编码
            List<String> dictCodes = new ArrayList<>();
            for (Map.Entry<String, BusinessTypeRuleData> ruleDataEntry : ruleDataMap.entrySet()) {
                dictCodes.add(ruleDataEntry.getValue().getRule());
            }

            List<Dict> dictList = new ArrayList<>();
            List<DictItem> dictItemList = new ArrayList<>();
            // 查询
            FilterGroup filter = new FilterGroup();
            filter.addFilter("dictCode", FilterGroup.Operator.in, String.join(",", dictCodes));
            dictList = dao.queryList(Dict.class, filter, "");
            if (dictList != null && dictList.size() > 0) {
                List<String> dictIds = new ArrayList<>();
                for (Dict dict : dictList) {
                    dictIds.add(dict.getId());
                }
                FilterGroup filter1 = new FilterGroup();
                filter1.addFilter("dictId", FilterGroup.Operator.in, String.join(",", dictIds));
                dictItemList = dao.queryList(DictItem.class, filter1, "");
                // 存入缓存
                for (Dict dict : dictList) {
                    String dictKey = String.format("%s:%s", currentUUID, dict.getDictCode());
                    Map<String, String> dictItems = new HashMap<>();
                    if (dictItemList != null && dictItemList.size() > 0) {
                        for (DictItem dictItem : dictItemList) {
                            if (dict.getId().equalsIgnoreCase(dictItem.getDictId())) {
                                dictItems.put(dictItem.getItemName(), dictItem.getItemCode());
                            }
                        }
                        logger.info(dictKey + " - " + dictItems.size());
                        redisTemplate.opsForValue().set(dictKey, dictItems, REDIS_TIME_OUT, TimeUnit.MINUTES);
                        dictKeys.add(dictKey);
                    }
                }
            }
        }

        return dictKeys;
    }

    /**
     * 主键查询，缓存
     *
     * @param ruleDataMap
     * @return
     */
    private List<String> setQueryRuleRedis(Map<String, BusinessTypeRuleData> ruleDataMap) {
        List<String> primaryKeys = new ArrayList<>();
        String gglFormat = "{\"%s\": {\"@fs\": \"%s\"}}";
        try {
            if (ruleDataMap != null && ruleDataMap.size() > 0) {
                for (Map.Entry<String, BusinessTypeRuleData> ruleDataEntry : ruleDataMap.entrySet()) {
                    String key = ruleDataEntry.getKey();
                    BusinessTypeRuleData ruleData = ruleDataEntry.getValue();
                    if (ruleData != null) {
                        Set<String> columnNames = new LinkedHashSet<>();
                        columnNames.add(ruleData.getGoal());
                        columnNames.addAll(ruleData.getQueryRuleColumn());
                        String ggl = String.format(gglFormat, ruleData.getQueryRuleTable(), String.join(",", columnNames));
                        logger.info(key + " - " + ggl);
                        ApiPagedResult page = ruleService.queryForMapList(ggl, false);
                        Map<String, Object> redisValue = pagedResultToMap(page, ruleData.getGoal(), ruleData.getQueryRuleColumn());
                        logger.info(String.format("%s - %s => %s", key, page.getTotal(), (redisValue != null ? redisValue.size() : 0)));
                        redisTemplate.opsForValue().set(key, redisValue, REDIS_TIME_OUT, TimeUnit.MINUTES);
                        primaryKeys.add(key);
                    }
                }
            }
        } catch (Exception ex) {
            logger.info(ex.getMessage(), ex);
        }

        return primaryKeys;
    }

    /**
     * 设置 缓存，数据字典、主键
     *
     * @param currentUUID 当前主键
     * @param tableMeta   元数据
     * @param data        业务数据
     * @return
     */
    public List<String> setCache(String currentUUID, Map<String, List<BusinessMeta>> tableMeta, List<Map<String, BusinessData>> data) {
        List<String> cacheList = new ArrayList<>();
        // 元数据
        Map<String, ConditionMeta> dictMetas = new HashMap<>();
        Map<String, ConditionMeta> primaryMetas = new HashMap<>();
        for (Map.Entry<String, List<BusinessMeta>> metaMap : tableMeta.entrySet()) {
            if (metaMap.getValue() != null && metaMap.getValue().size() > 0) {
                for (BusinessMeta meta : metaMap.getValue()) {
                    ConditionMeta conditionMeta = null;
                    if (meta.isEvaluationTypeDictionary() && Strings.isNotBlank(meta.getDictCode())) {
                        conditionMeta = new ConditionMeta();
                        conditionMeta.setVariable(meta.getVariableValue());
                        conditionMeta.setDictCode(meta.getDictCode());
                    } else if (meta.isEvaluationTypePrimaryKey() && Strings.isNotBlank(meta.getPrimaryValue())) {
                        conditionMeta = new ConditionMeta();
                        conditionMeta.setVariable(meta.getVariableValue());
                        conditionMeta.setTableName(meta.getPrimaryKeyTable());
                        conditionMeta.setColumnNames(meta.getPrimaryKeyColumns());
                        conditionMeta.setGoalName(meta.getPrimaryKeyGoal());
                    }
                    if (conditionMeta != null) {
                        List<String> values = new ArrayList<>();
                        for (Map<String, BusinessData> map : data) {
                            BusinessData businessData = map.get(meta.getVariableValue());
                            if (businessData != null) {
                                values.add(String.valueOf(businessData.getValue()));
                            }
                        }
                        conditionMeta.setValues(values);
                        if (meta.isEvaluationTypeDictionary()) {
                            String key = String.format("%s:%s", currentUUID, meta.getDictCode());
                            if (!dictMetas.containsKey(key)) {
                                dictMetas.put(key, conditionMeta);
                            }
                        } else if (meta.isEvaluationTypePrimaryKey()) {
                            String key = String.format("%s:%s", currentUUID, meta.getPrimaryValue());
                            if (!primaryMetas.containsKey(key)) {
                                primaryMetas.put(key, conditionMeta);
                            }
                        }
                    }
                }
            }
        }
        dao.setDefaultFilter(true, filterGroup);
        // 数据字典
        List<String> dictKeys = setDictRedis(currentUUID, dictMetas);
        cacheList.addAll(dictKeys);
        // 主键
        List<String> primaryKeys = setPrimaryRedis(primaryMetas);
        cacheList.addAll(primaryKeys);

        return cacheList;
    }

    /**
     * 数据字典缓存
     *
     * @param currentUUID
     * @param dictMetas
     * @return
     */
    private List<String> setDictRedis(String currentUUID, Map<String, ConditionMeta> dictMetas) {
        List<String> dictKeys = new ArrayList<>();
        if (dictMetas != null && dictMetas.size() > 0) {
            Set<String> dictCodes = new LinkedHashSet<>();
            Set<String> dictItemNames = new LinkedHashSet<>();
            for (Map.Entry<String, ConditionMeta> metaEntry : dictMetas.entrySet()) {
                if (metaEntry.getValue() != null) {
                    dictCodes.add(metaEntry.getValue().getDictCode());
                    dictItemNames.addAll(metaEntry.getValue().getValues());
                }
            }

            List<Dict> dictList = new ArrayList<>();
            List<DictItem> dictItemList = new ArrayList<>();
            // 查询
            FilterGroup filter = new FilterGroup();
            filter.addFilter("dictCode", FilterGroup.Operator.in, String.join(",", dictCodes));
            dictList = dao.queryList(Dict.class, filter, "");
            if (dictList != null && dictList.size() > 0) {
                List<String> dictIds = new ArrayList<>();
                for (Dict dict : dictList) {
                    dictIds.add(dict.getId());
                }
                FilterGroup filter1 = new FilterGroup();
                filter1.addFilter("dictId", FilterGroup.Operator.in, String.join(",", dictIds));
                // filter1.addFilter("itemName", FilterGroup.Operator.in, String.join(",", dictItemNames));
                dictItemList = dao.queryList(DictItem.class, filter1, "");
                // 存入缓存
                for (Dict dict : dictList) {
                    String dictKey = String.format("%s:%s", currentUUID, dict.getDictCode());
                    Map<String, String> dictItems = new HashMap<>();
                    if (dictItemList != null && dictItemList.size() > 0) {
                        for (DictItem dictItem : dictItemList) {
                            if (dict.getId().equalsIgnoreCase(dictItem.getDictId())) {
                                dictItems.put(dictItem.getItemName(), dictItem.getItemCode());
                            }
                        }
                        logger.info(dictKey + " - " + dictItems.size());
                        redisTemplate.opsForValue().set(dictKey, dictItems, REDIS_TIME_OUT, TimeUnit.MINUTES);
                        dictKeys.add(dictKey);
                    }
                }
            }
        }

        return dictKeys;
    }

    /**
     * 主键查询，缓存
     *
     * @param primaryMetas
     * @return
     */
    private List<String> setPrimaryRedis(Map<String, ConditionMeta> primaryMetas) {
        List<String> primaryKeys = new ArrayList<>();
        String gglFormat = "{\"%s\": {\"@fs\": \"%s\"}}";
        try {
            if (primaryMetas != null && primaryMetas.size() > 0) {
                for (Map.Entry<String, ConditionMeta> metaEntry : primaryMetas.entrySet()) {
                    String key = metaEntry.getKey();
                    ConditionMeta meta = metaEntry.getValue();
                    if (meta != null) {
                        Set<String> columnNames = new LinkedHashSet<>();
                        columnNames.add(meta.getGoalName());
                        columnNames.addAll(meta.getColumnNames());
                        String ggl = String.format(gglFormat, meta.getTableName(), String.join(",", columnNames));
                        logger.info(key + " - " + ggl);
                        ApiPagedResult page = ruleService.queryForMapList(ggl, false);
                        logger.info(key + " - " + page.getTotal());
                        Map<String, Object> redisValue = pagedResultToMap(page, meta.getGoalName(), meta.getColumnNames());
                        logger.info(String.format("%s - %s => %s", key, page.getTotal(), (redisValue != null ? redisValue.size() : 0)));
                        redisTemplate.opsForValue().set(key, redisValue, REDIS_TIME_OUT, TimeUnit.MINUTES);
                        primaryKeys.add(key);
                    }
                }
            }
        } catch (Exception ex) {
            logger.info(ex.getMessage(), ex);
        }

        return primaryKeys;
    }

    /**
     * 值转化
     *
     * @param page        查询结果
     * @param goalName    目标字段
     * @param columnNames 查询字段
     * @return
     */
    private Map<String, Object> pagedResultToMap(ApiPagedResult page, String goalName, List<String> columnNames) {
        Map<String, Object> redisMap = new HashMap<>();
        if (page != null && page.getData() != null && page.getTotal() > 0) {
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) page.getData();
            for (Map<String, Object> map : mapList) {
                Object goalValue = map.get(goalName);
                if (goalValue != null) {
                    for (String columnName : columnNames) {
                        String value = String.valueOf(map.get(columnName));
                        if (Strings.isNotBlank(value) && !redisMap.containsKey(value)) {
                            redisMap.put(value, goalName);
                        }
                    }
                }
            }
        }

        return redisMap;
    }
}
