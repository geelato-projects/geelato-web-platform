package org.geelato.web.platform.m.excel.service;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.xssf.usermodel.*;
import org.geelato.web.platform.exception.file.FileContentIsEmptyException;
import org.geelato.web.platform.exception.file.FileContentReadFailedException;
import org.geelato.web.platform.m.excel.entity.BusinessColumnMeta;
import org.geelato.web.platform.m.excel.entity.BusinessData;
import org.geelato.web.platform.m.excel.entity.BusinessMeta;
import org.geelato.web.platform.m.excel.entity.BusinessTypeData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: XSSFSheet, xlsx
 * @date 2023/10/16 9:43
 */
@Component
public class ExcelXSSFReader {
    private final Logger logger = LoggerFactory.getLogger(ExcelXSSFReader.class);
    @Lazy
    @Autowired
    private ExcelCommonUtils excelCommonUtils;

    /**
     * 元数据
     *
     * @param sheet
     * @return
     */
    public Map<String, List<BusinessMeta>> readBusinessMeta(XSSFSheet sheet) {
        Map<String, List<BusinessMeta>> tableMaps = new HashMap<>();
        int lastRowIndex = sheet.getLastRowNum();
        logger.info("BusinessMeta = " + lastRowIndex);
        List<BusinessMeta> columns = new ArrayList<>();
        List<String> tables = new ArrayList<>();
        // 跳过第一行，标题行
        for (int i = 1; i <= lastRowIndex; i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
                break;
            }
            try {
                BusinessMeta meta = new BusinessMeta();
                meta.setTableName(row.getCell(0).getStringCellValue());
                meta.setColumnName(row.getCell(1).getStringCellValue());
                meta.setEvaluation(row.getCell(2).getStringCellValue());
                meta.setConstValue(row.getCell(3).getStringCellValue());
                meta.setVariableValue(row.getCell(4).getStringCellValue());
                meta.setExpression(row.getCell(5).getStringCellValue());
                meta.setDictCode(row.getCell(6).getStringCellValue());
                meta.setPrimaryValue(row.getCell(7).getStringCellValue());
                meta.setRemark(row.getCell(8).getStringCellValue());
                columns.add(meta);
                // 表格
                if (!tables.contains(meta.getTableName())) {
                    tables.add(meta.getTableName());
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new FileContentReadFailedException("Business Meta, Read Failed In (" + i + ").");
            }
        }
        // 按表格分类
        for (String tableName : tables) {
            List<BusinessMeta> metas = new ArrayList<>();
            for (BusinessMeta meta : columns) {
                if (tableName.equalsIgnoreCase(meta.getTableName())) {
                    metas.add(meta);
                }
            }
            tableMaps.put(tableName, metas);
        }

        return tableMaps;
    }

    /**
     * 业务数据类型
     *
     * @param sheet
     * @return
     */
    public Map<String, BusinessTypeData> readBusinessTypeData(XSSFSheet sheet) {
        int lastRowIndex = sheet.getLastRowNum();
        logger.info("BusinessTypeData = " + lastRowIndex);
        Map<String, BusinessTypeData> metaMap = new HashMap<String, BusinessTypeData>(lastRowIndex);
        // 跳过第一行，标题行
        for (int i = 1; i <= lastRowIndex; i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
                break;
            }
            try {
                BusinessTypeData meta = new BusinessTypeData();
                meta.setName(row.getCell(0).getStringCellValue());
                meta.setType(row.getCell(1).getStringCellValue());
                meta.setFormat(row.getCell(2).getStringCellValue());
                // 多值分解，全局规则
                meta.setMultiSeparator(row.getCell(3).getStringCellValue());
                meta.setMultiScene(row.getCell(4).getStringCellValue());
                // 规则，单格规则
                String rules = row.getCell(5).getStringCellValue();
                meta.setTypeRuleData(excelCommonUtils.readBusinessTypeRuleData(rules));
                meta.setRemark(row.getCell(6).getStringCellValue());
                metaMap.put(meta.getName(), meta);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new FileContentReadFailedException("Business Data Type, Read Failed In (" + i + ").");
            }
        }

        return metaMap;
    }

    /**
     * 读取业务数据
     *
     * @param sheet
     * @param businessTypeDataMap 数据类型
     * @return
     */
    public List<Map<String, BusinessData>> readBusinessData(XSSFSheet sheet, Map<String, BusinessTypeData> businessTypeDataMap) {
        int lastRowIndex = sheet.getLastRowNum();
        int lastCellNum = 0;
        logger.info("BusinessData Rows = " + lastRowIndex);
        // 第一行
        List<BusinessColumnMeta> headers = new ArrayList<>();
        XSSFRow firstRow = sheet.getRow(0);
        if (firstRow != null) {
            lastCellNum = firstRow.getLastCellNum();
            logger.info("BusinessData Cells = " + lastCellNum);
            for (int i = 0; i < lastCellNum; i++) {
                XSSFCell cell = firstRow.getCell(i);
                if (cell != null) {
                    String cellValue = cell.getStringCellValue();
                    if (Strings.isNotBlank(cellValue)) {
                        BusinessColumnMeta busColMeta = new BusinessColumnMeta();
                        busColMeta.setIndex(i);
                        busColMeta.setBusinessTypeData(businessTypeDataMap.get(cellValue));
                        if (busColMeta.getBusinessTypeData() != null) {
                            headers.add(busColMeta);
                        }
                    }
                }
            }
        } else {
            throw new FileContentIsEmptyException("Business Data Table Header is Empty.");
        }
        // 实体数据
        List<Map<String, BusinessData>> businessDataMapList = new ArrayList<>();
        for (int i = 1; i <= lastRowIndex; i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
                break;
            }
            Map<String, BusinessData> businessDataMap = new HashMap<>();
            for (BusinessColumnMeta colMeta : headers) {
                BusinessTypeData data = colMeta.getBusinessTypeData();
                // 定位、格式
                BusinessData businessData = new BusinessData();
                businessData.setXIndex(colMeta.getIndex());
                businessData.setYIndex(i);
                businessData.setBusinessTypeData(data);
                Object cellValue = null;
                // 每个格子的数据
                XSSFCell cell = row.getCell(colMeta.getIndex());
                if (cell != null) {
                    try {
                        if (data.isColumnTypeString()) {
                            if (CellType.NUMERIC.equals(cell.getCellType())) {
                                cellValue = String.valueOf(cell.getNumericCellValue());
                            } else if (CellType.STRING.equals(cell.getCellType())) {
                                cellValue = cell.getStringCellValue();
                            }
                        } else if (data.isColumnTypeNumber()) {
                            if (CellType.NUMERIC.equals(cell.getCellType())) {
                                cellValue = cell.getNumericCellValue();
                            } else if (CellType.STRING.equals(cell.getCellType())) {
                                String value = cell.getStringCellValue();
                                if (Strings.isNotBlank(value)) {
                                    if (cell.getStringCellValue().indexOf(".") == -1) {
                                        cellValue = Long.parseLong(value);
                                        cell.setCellValue(Long.parseLong(value));
                                    } else {
                                        cellValue = new BigDecimal(value).doubleValue();
                                    }
                                }
                            }
                        } else if (data.isColumnTypeBoolean()) {
                            if (CellType.BOOLEAN.equals(cell.getCellType())) {
                                cellValue = cell.getBooleanCellValue();
                            } else if (CellType.STRING.equals(cell.getCellType()) && Strings.isNotBlank(data.getFormat())) {
                                cellValue = data.getFormat().equalsIgnoreCase(cell.getStringCellValue());
                            } else if (CellType.NUMERIC.equals(cell.getCellType()) && Strings.isNotBlank(data.getFormat())) {
                                cellValue = data.getFormat() == cell.getStringCellValue();
                            } else if (CellType.NUMERIC.equals(cell.getCellType())) {
                                cellValue = cell.getNumericCellValue() > 0;
                            } else {
                                cellValue = false;
                            }
                        } else if (data.isColumnTypeDateTime()) {
                            if (CellType.NUMERIC.equals(cell.getCellType())) {
                                cellValue = cell.getDateCellValue();
                            } else if (Strings.isNotBlank(data.getFormat())) {
                                cellValue = new SimpleDateFormat(data.getFormat()).parse(cell.getStringCellValue());
                            }
                        }
                        businessData.setValue(cellValue);
                        businessData.setPrimevalValue(cellValue);
                    } catch (Exception ex) {
                        businessData.setErrorMsg(ex.getMessage());
                    }
                    businessDataMap.put(data.getName(), businessData);
                }
            }
            if (!businessDataMap.isEmpty()) {
                businessDataMapList.add(businessDataMap);
            }
        }

        return businessDataMapList;
    }

    /**
     * 往业务数据中写入校验批注
     *
     * @param sheet
     * @param style               填充颜色
     * @param businessDataMapList 业务数据
     */
    public void writeBusinessData(XSSFSheet sheet, XSSFCellStyle style, List<Map<String, BusinessData>> businessDataMapList) {
        Map<String, BusinessData> mapSet = new HashMap<>();
        for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
            for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                BusinessData businessData = businessDataEntry.getValue();
                if (businessData != null && !businessData.isValidate()) {
                    String key = String.format("Y.%s;X.%s", businessData.getYIndex(), businessData.getXIndex());
                    BusinessData msgData = (mapSet.containsKey(key) && mapSet.get(key) != null) ? mapSet.get(key) : new BusinessData();
                    msgData.setYIndex(businessData.getYIndex());
                    msgData.setXIndex(businessData.getXIndex());
                    msgData.setErrorMsgs(businessData.getErrorMsg());
                    mapSet.put(key, msgData);
                }
            }
        }
        // 实体数据
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row == null) {
                break;
            }
            for (Map.Entry<String, BusinessData> businessDataEntry : mapSet.entrySet()) {
                BusinessData businessData = businessDataEntry.getValue();
                if (businessData != null && businessData.getYIndex() == i) {
                    XSSFCell cell = row.getCell(businessData.getXIndex());
                    if (cell != null) {
                        cell.setCellStyle(style);
                        ClientAnchor anchor = new XSSFClientAnchor();
                        anchor.setCol1(cell.getColumnIndex());
                        anchor.setRow1(cell.getRowIndex());
                        anchor.setCol2(cell.getColumnIndex() + 1);
                        anchor.setRow2(cell.getRowIndex() + 1);
                        Drawing drawing = sheet.createDrawingPatriarch();
                        XSSFComment comment = (XSSFComment) drawing.createCellComment(anchor);
                        comment.setString(new XSSFRichTextString(String.join("；\r\n", businessData.getErrorMsg())));
                        cell.setCellComment(comment);
                    }
                }
            }
        }
    }
}
