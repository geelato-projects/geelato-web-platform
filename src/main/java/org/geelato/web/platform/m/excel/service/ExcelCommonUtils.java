package org.geelato.web.platform.m.excel.service;

import com.alibaba.fastjson2.JSON;
import org.apache.logging.log4j.util.Strings;
import org.geelato.web.platform.m.excel.entity.BusinessData;
import org.geelato.web.platform.m.excel.entity.BusinessTypeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/10/27 9:53
 */
public class ExcelCommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExcelCommonUtils.class);

    /**
     * 导入数据，多值数据处理
     *
     * @param businessDataMapList
     * @return
     */
    public static List<Map<String, BusinessData>> handleBusinessDataMultiScene(List<Map<String, BusinessData>> businessDataMapList) {
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
        logger.info(JSON.toJSONString(multiLoggers));

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
    private static List<Map<String, BusinessData>> mergeBusinessData(Map<String, BusinessData> singleData, List<Map<String, BusinessData>> multiMapList, List<Map<String, BusinessData>> symMapList) {
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
    private static List<Map<String, BusinessData>> cartesianProduct(Map<String, BusinessData> multiData) {
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
    private static Set<String[]> cartesianProductHelper(String[][] arrays) {
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
    private static void cartesianProductHelper(String[][] arrays, int index, String[] current, Set<String[]> result) {
        if (index == arrays.length) {
            result.add(current.clone());
            return;
        }

        for (String element : arrays[index]) {
            current[index] = element;
            cartesianProductHelper(arrays, index + 1, current, result);
        }
    }
}
