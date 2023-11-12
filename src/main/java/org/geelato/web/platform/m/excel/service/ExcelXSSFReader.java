package org.geelato.web.platform.m.excel.service;

import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.geelato.core.meta.model.field.ColumnMeta;
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
                                cellValue = String.valueOf(BigDecimal.valueOf(cell.getNumericCellValue()));
                            } else if (CellType.STRING.equals(cell.getCellType())) {
                                cellValue = cell.getStringCellValue();
                            }
                        } else if (data.isColumnTypeNumber()) {
                            if (CellType.NUMERIC.equals(cell.getCellType())) {
                                String value = String.valueOf(BigDecimal.valueOf(cell.getNumericCellValue()));
                                cellValue = ExcelCommonUtils.stringToNumber(value);
                            } else if (CellType.STRING.equals(cell.getCellType())) {
                                cellValue = ExcelCommonUtils.stringToNumber(cell.getStringCellValue());
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
                }
                businessDataMap.put(data.getName(), businessData);
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
        // 清理 批注
        cleanSheetStyle(sheet);
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
                    if (cell == null) {
                        cell = row.createCell(businessData.getXIndex());
                    }
                    cell.setCellStyle(style);
                    ClientAnchor anchor = new XSSFClientAnchor();
                    anchor.setCol1(cell.getColumnIndex());
                    anchor.setRow1(cell.getRowIndex());
                    anchor.setCol2(cell.getColumnIndex() + 10);
                    anchor.setRow2(cell.getRowIndex() + 10);
                    Drawing drawing = sheet.createDrawingPatriarch();
                    XSSFComment comment = (XSSFComment) drawing.createCellComment(anchor);
                    comment.setString(new XSSFRichTextString(String.join("；\r\n", businessData.getErrorMsg())));
                    cell.setCellComment(comment);
                }
            }
        }

    }

    /**
     * 插入工作表，唯一性约束校验失败
     *
     * @param workbook
     * @param repeatedData
     */
    public void writeRepeatedData(XSSFWorkbook workbook, Map<ColumnMeta, Map<Object, Long>> repeatedData) {
        if (repeatedData != null && repeatedData.size() > 0) {
            XSSFSheet sheet = workbook.createSheet("唯一约束，导入数据重复值及数量"); // 创建新的工作表
            int x = 0;
            for (Map.Entry<ColumnMeta, Map<Object, Long>> columnMetaMapEntry : repeatedData.entrySet()) {
                // 表头
                ColumnMeta meta = columnMetaMapEntry.getKey();
                createRowCell(sheet, 0, x, String.format("%s(%s:%s)", meta.getTitle(), meta.getTableName(), meta.getFieldName()));
                CellRangeAddress rangea = new CellRangeAddress(0, 0, x, x + 1); // A1到D1的范围
                sheet.addMergedRegion(rangea);
                createRowCell(sheet, 1, x, "值");
                createRowCell(sheet, 1, x + 1, "值数量");
                // 内容
                int y = 2;
                Map<Object, Long> objectLongMap = columnMetaMapEntry.getValue();
                for (Map.Entry<Object, Long> mapEntry : objectLongMap.entrySet()) {
                    createRowCell(sheet, y, x, mapEntry.getKey());
                    createRowCell(sheet, y, x + 1, mapEntry.getValue());
                    y = y + 1;
                }
                x = x + 2;
            }
        }
    }

    /**
     * 创建表格
     *
     * @param sheet
     * @param y
     * @param x
     * @param value
     */
    private void createRowCell(XSSFSheet sheet, int y, int x, Object value) {
        XSSFRow row = sheet.getRow(y);
        if (row == null) {
            row = sheet.createRow(y);
        }
        XSSFCell cell = row.getCell(x);
        if (cell == null) {
            cell = row.createCell(x);
        }
        cell.setCellValue(String.valueOf(value));
    }

    /**
     * 清理 批注，
     *
     * @param sheet
     */
    private void cleanSheetStyle(XSSFSheet sheet) {
        int yCount = sheet.getLastRowNum();
        for (int y = 1; y <= yCount; y++) {
            XSSFRow row = sheet.getRow(y);
            if (row != null) {
                int xCount = row.getLastCellNum();
                for (int x = 0; x < xCount; x++) {
                    XSSFCell cell = row.getCell(x);
                    if (cell != null) {
                        // 单元格注释
                        XSSFComment comment = cell.getCellComment();
                        if (comment != null) {
                            cell.removeCellComment();
                        }
                        // 清理格式
                        /*XSSFCellStyle style = cell.getCellStyle();
                        if (style != null) {
                            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                            cell.setCellStyle(style);
                        }*/
                    }
                }
            }
        }
    }
}
