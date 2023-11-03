package org.geelato.web.platform.m.excel.rest;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.MysqlDataTypeEnum;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.meta.model.field.FieldMeta;
import org.geelato.core.script.js.JsProvider;
import org.geelato.utils.UIDGenerator;
import org.geelato.web.platform.enums.ExcelColumnTypeEnum;
import org.geelato.web.platform.exception.file.FileNotFoundException;
import org.geelato.web.platform.exception.file.*;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.BusinessData;
import org.geelato.web.platform.m.excel.entity.BusinessMeta;
import org.geelato.web.platform.m.excel.entity.BusinessTypeData;
import org.geelato.web.platform.m.excel.entity.ExportTemplate;
import org.geelato.web.platform.m.excel.service.ExcelCommonUtils;
import org.geelato.web.platform.m.excel.service.ExcelReader;
import org.geelato.web.platform.m.excel.service.ExcelXSSFReader;
import org.geelato.web.platform.m.excel.service.ExportTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/10/12 14:09
 */
@Controller
@RequestMapping(value = "/api/import")
public class ImportExcelController extends BaseController {
    private static final String EXCEL_XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String EXCEL_XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ROOT_DIRECTORY = "upload";
    private static final String REQUEST_FILE_PART = "file";
    private static final String IMPORT_ERROR_FILE_GENRE = "importErrorFile";
    private final Logger logger = LoggerFactory.getLogger(ImportExcelController.class);
    private final MetaManager metaManager = MetaManager.singleInstance();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private ExportTemplateService exportTemplateService;
    @Autowired
    private ExcelReader excelReader;
    @Autowired
    private ExcelXSSFReader excelXSSFReader;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private AttachService attachService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ExcelCommonUtils excelCommonUtils;

    /**
     * 下载模板
     *
     * @param request
     * @param response
     * @param templateId
     * @return
     */
    @RequestMapping(value = "/template/{templateId}", method = RequestMethod.GET)
    @ResponseBody
    public ApiResult getTemplate(HttpServletRequest request, HttpServletResponse response, @PathVariable String templateId) {
        ApiResult result = new ApiResult();
        try {
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            result.success().setData(exportTemplate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    /**
     * @param request
     * @param response
     * @param importType part:可以部分导入；all:需要全部导入，错误即中断并回滚。
     * @param templateId 模板文件id
     * @param attachId   业务数据文件id
     * @return
     */
    @RequestMapping(value = "/attach/{importType}/{templateId}/{attachId}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult importAttach(HttpServletRequest request, HttpServletResponse response, @PathVariable String importType, @PathVariable String templateId, @PathVariable String attachId) {
        ApiResult result = new ApiResult();
        try {
            result = importExcel(request, response, importType, templateId, attachId);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error(ex);
        }
        return result;
    }

    /**
     * excel导入
     *
     * @param request
     * @param response
     * @param importType part:可以部分导入；all:需要全部导入，错误即中断并回滚。
     * @param templateId 模板文件id
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/file/{importType}/{templateId}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult importFile(HttpServletRequest request, HttpServletResponse response, @PathVariable String importType, @PathVariable String templateId) {
        ApiResult result = new ApiResult();
        try {
            result = importExcel(request, response, importType, templateId, null);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error(ex);
        }
        return result;
    }

    public ApiResult importExcel(HttpServletRequest request, HttpServletResponse response, String importType, String templateId, String attachId) {
        ApiResult result = new ApiResult();
        String currentUUID = String.valueOf(UIDGenerator.generate());
        try {
            long importStart = System.currentTimeMillis();
            // 文件内容
            Map<String, List<BusinessMeta>> businessMetaListMap = new HashMap<>();// 元数据
            Map<String, BusinessTypeData> businessTypeDataMap = new HashMap<>();// 数据类型
            List<Map<String, BusinessData>> businessDataMapList = new ArrayList<>();// 业务数据
            // 事务模板查询
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            ExcelCommonUtils.notNull(exportTemplate, new FileNotFoundException("ExportTemplate Data Not Found"));
            logger.info(String.format("事务模板（%s[%s]）", exportTemplate.getTitle(), exportTemplate.getId()));
            //事务，模板元数据
            Attach templateRuleAttach = getFile(exportTemplate.getTemplateRule());
            ExcelCommonUtils.notNull(templateRuleAttach, new FileNotFoundException("Business Data Type And Meta File Not Found"));
            logger.info(String.format("数据类型+元数据（%s[%s]）%s", templateRuleAttach.getName(), templateRuleAttach.getId(), templateRuleAttach.getUrl()));
            businessMetaListMap = getBusinessMeta(new File(templateRuleAttach.getUrl()), 1);
            //事务，模板数据类型
            // Attach templateAttach = getFile(exportTemplate.getTemplate());
            businessTypeDataMap = getBusinessTypeData(new File(templateRuleAttach.getUrl()), 0);
            // 事务，业务数据
            Attach businessFile = null;
            if (Strings.isNotBlank(attachId)) {
                businessFile = getFile(attachId);
                ExcelCommonUtils.notNull(businessFile, new FileNotFoundException("Business Data File Not Found"));
                logger.info(String.format("业务数据（%s[%s]）[%s]", businessFile.getName(), businessFile.getId(), sdf.format(new Date())));
            }
            businessDataMapList = getBusinessData(businessFile, request, businessTypeDataMap, 0);
            // 需要转化的业务数据
            businessDataMapList = excelCommonUtils.handleBusinessDataRule(currentUUID, businessDataMapList, true);
            logger.info(String.format("BusinessData Handle Rule [TRUE] = %s [%s]", (businessDataMapList == null ? 0 : businessDataMapList.size()), sdf.format(new Date())));
            // 需要分割的业务数据，多值数据处理
            businessDataMapList = excelCommonUtils.handleBusinessDataMultiScene(businessDataMapList);
            logger.info(String.format("BusinessData Handle Multi Scene = %s [%s]", (businessDataMapList == null ? 0 : businessDataMapList.size()), sdf.format(new Date())));
            // 需要转化的业务数据
            businessDataMapList = excelCommonUtils.handleBusinessDataRule(currentUUID, businessDataMapList, false);
            logger.info(String.format("BusinessData Handle Rule [FALSE] = %s [%s]", (businessDataMapList == null ? 0 : businessDataMapList.size()), sdf.format(new Date())));
            // 设置缓存
            List<String> cacheKeys = excelCommonUtils.setCache(currentUUID, businessMetaListMap, businessDataMapList);
            logger.info(String.format("Redis Template [ADD] = %s [%s]", (cacheKeys == null ? 0 : cacheKeys.size()), sdf.format(new Date())));
            // 忽略默认字段
            List<String> columnNames = excelCommonUtils.getDefaultColumns();
            // 获取
            logger.info(String.format("业务数据解析-开始 [%s]", sdf.format(new Date())));
            long parseStart = System.currentTimeMillis();
            Map<String, List<Map<String, Object>>> tableData = new HashMap<>();
            for (Map.Entry<String, List<BusinessMeta>> metaMap : businessMetaListMap.entrySet()) {
                // 获取表格字段信息
                EntityMeta entityMeta = metaManager.getByEntityName(metaMap.getKey(), false);
                Assert.notNull(entityMeta, "Table Meta Is Null");
                // 当前字段
                List<Map<String, Object>> columnData = new ArrayList<>();
                long countCow = 0;
                for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
                    // 一行业务数据，键值对
                    Map<String, Object> valueMap = new HashMap<>();
                    for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                        valueMap.put(businessDataEntry.getKey(), businessDataEntry.getValue().getValue());
                    }
                    // 一行数据库数据
                    Map<String, Object> columnMap = new HashMap<>();
                    long start = System.currentTimeMillis();
                    for (BusinessMeta meta : metaMap.getValue()) {
                        FieldMeta fieldMeta = entityMeta.getFieldMeta(meta.getColumnName());
                        Assert.notNull(entityMeta, "Table FieldMeta Is Null");
                        Object value = null;
                        BusinessData businessData = businessDataMap.get(meta.getVariableValue());
                        if (businessData != null) {
                            try {
                                // 获取值
                                value = getValue(currentUUID, fieldMeta.getColumn(), meta, businessData, valueMap);
                                // 验证值
                                Set<String> errorMsg = validateValue(fieldMeta.getColumn(), businessData, value, columnNames);
                                businessData.setErrorMsgs(errorMsg);
                            } catch (Exception ex) {
                                businessData.setErrorMsg(ex.getMessage());
                            }
                        }
                        columnMap.put(meta.getColumnName(), value);
                    }
                    logger.info(String.format("表 %s 解析成功，第 %s 行。用时 %s ms。", metaMap.getKey(), (countCow += 1), (System.currentTimeMillis() - start)));
                    columnData.add(columnMap);
                }
                tableData.put(metaMap.getKey(), columnData);
            }
            logger.info(String.format("业务数据解析-结束 用时：%s ms", (System.currentTimeMillis() - parseStart)));
            // 释放缓存
            redisTemplate.delete(cacheKeys);
            logger.info(String.format("Redis Template [DELETE] [%s]", sdf.format(new Date())));
            // 业务数据校验
            if (!validBusinessData(businessDataMapList)) {
                Map<String, Object> errorAttach = writeBusinessData(businessFile, request, response, businessDataMapList, 0);
                logger.info(String.format("业务数据校验-错误 [%s]", sdf.format(new Date())));
                return result.error(new FileContentValidFailedException("For more information, see the error file.")).setData(errorAttach);
            }
            // 插入数据 "@biz": "myBizCode",
            logger.info(String.format("插入业务数据-开始 [%s]", sdf.format(new Date())));
            long insertStart = System.currentTimeMillis();
            if (!tableData.isEmpty()) {
                Map<String, Object> insertMap = new HashMap<>();
                insertMap.put("@biz", "myBizCode");
                for (Map.Entry<String, List<Map<String, Object>>> table : tableData.entrySet()) {
                    insertMap.put(table.getKey(), table.getValue());
                }
                ruleService.batchSave(JSON.toJSONString(insertMap), true);
            } else {
                throw new FileContentIsEmptyException("Business Import Data Is Empty");
            }
            logger.info(String.format("插入业务数据-结束 用时：%s ms", (System.currentTimeMillis() - insertStart)));
            logger.info(String.format("导入业务数据-结束 用时：%s ms", (System.currentTimeMillis() - importStart)));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error(ex);
        }

        return result;
    }

    /**
     * 业务数据类型与元数据类型校验
     *
     * @param columnMeta   元数据
     * @param businessData 业务数据类型
     * @param value        值
     * @return
     */
    private Set<String> validateValue(ColumnMeta columnMeta, BusinessData businessData, Object value, List<String> columnNames) {
        Set<String> errorMsg = new LinkedHashSet<>();
        BusinessTypeData typeData = businessData.getBusinessTypeData();

        if (MysqlDataTypeEnum.getBooleans().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.BOOLEAN.name().equalsIgnoreCase(typeData.getType())) {

        } else if (MysqlDataTypeEnum.getStrings().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.STRING.name().equalsIgnoreCase(typeData.getType())) {
            if (value != null && String.valueOf(value).length() > columnMeta.getCharMaxLength()) {
                errorMsg.add(String.format("当前长度：%s；已超出字段最大长度：%s。", String.valueOf(value).length(), columnMeta.getCharMaxLength()));
            }
        } else if (MysqlDataTypeEnum.getNumbers().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.NUMBER.name().equalsIgnoreCase(typeData.getType())) {
            if (value != null && String.valueOf(value).length() > (columnMeta.getNumericPrecision() + columnMeta.getNumericScale())) {
                errorMsg.add(String.format("当前长度：%s；已超出字段数值位数限制：%s。", String.valueOf(value).length(), (columnMeta.getNumericPrecision() + columnMeta.getNumericScale())));
            }
        } else if (MysqlDataTypeEnum.getDates().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.DATETIME.name().equalsIgnoreCase(typeData.getType())) {

        } else {
            errorMsg.add(String.format("业务数据格式：%s；而数据库存储格式为：%s。", typeData.getType(), columnMeta.getDataType()));
        }
        if (value == null && !columnMeta.isNullable() && !columnNames.contains(columnMeta.getName())) {
            errorMsg.add(String.format("原始数据[%s]，对应字段值不能为空。", businessData.getPrimevalValue()));
        }

        return errorMsg;
    }

    /**
     * 元数据对应的值
     *
     * @param currentUUID
     * @param columnMeta   元数据
     * @param meta         元数据
     * @param businessData 业务数据
     * @param valueMap     一行业务数据
     * @return
     */
    private Object getValue(String currentUUID, ColumnMeta columnMeta, BusinessMeta meta, BusinessData businessData, Map<String, Object> valueMap) {
        Object value = null;
        if (meta.isEvaluationTypeConst()) {
            value = meta.getConstValue();
        } else if (meta.isEvaluationTypeVariable()) {
            value = businessData.getValue();
        } else if (meta.isEvaluationTypeJsExpression()) {
            value = JsProvider.executeExpression(meta.getExpression(), valueMap);
        } else if (meta.isEvaluationTypePrimaryKey()) {
            if (businessData.getValue() != null) {
                Map<String, Object> redisValues = (Map<String, Object>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, meta.getPrimaryValue()));
                if (redisValues != null && redisValues.size() > 0) {
                    value = redisValues.get(String.valueOf(businessData.getValue()));
                }
            }
        } else if (meta.isEvaluationTypeDictionary()) {
            if (businessData.getValue() != null) {
                Map<String, String> redisValues = (Map<String, String>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, meta.getDictCode()));
                if (redisValues != null && redisValues.size() > 0) {
                    value = redisValues.get(String.valueOf(businessData.getValue()));
                }
            }
        } else if (meta.isEvaluationTypeSerialNumber()) {
            value = currentUUID;
        }
        if (columnMeta.getDataType().equalsIgnoreCase("year")) {
            value = new SimpleDateFormat("yyyy").format(value);
        } else if (columnMeta.getDataType().equalsIgnoreCase("date")) {
            value = new SimpleDateFormat("yyyy-MM-dd").format(value);
        } else if (columnMeta.getDataType().equalsIgnoreCase("time")) {
            value = new SimpleDateFormat("HH:mm:ss").format(value);
        } else if (columnMeta.getDataType().equalsIgnoreCase("dateTime")) {
            value = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
        }

        return value;
    }

    /**
     * 获取元数据
     *
     * @param file       文件
     * @param sheetIndex 工作表次序
     * @return
     * @throws IOException
     */
    private Map<String, List<BusinessMeta>> getBusinessMeta(File file, int sheetIndex) throws IOException {
        Map<String, List<BusinessMeta>> businessMetaListMap = new HashMap<>();
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            // excel文件类型
            String contentType = Files.probeContentType(file.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessMetaListMap = excelReader.readBusinessMeta(sheet);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook workbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessMetaListMap = excelXSSFReader.readBusinessMeta(sheet);
            } else {
                throw new FileTypeNotSupportedException("Business Meta, Excel Type: " + contentType);
            }
        } catch (IOException ex) {
            throw new FileException("Business Meta, Excel Sheet(" + sheetIndex + ") Reader Failed! " + ex.getMessage());
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        return businessMetaListMap;
    }

    /**
     * 获取业务数据类型
     *
     * @param file       文件
     * @param sheetIndex 工作表次序
     * @return
     * @throws IOException
     */
    private Map<String, BusinessTypeData> getBusinessTypeData(File file, int sheetIndex) throws IOException {
        Map<String, BusinessTypeData> businessTypeDataMap = new HashMap<>();
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            // excel文件类型
            String contentType = Files.probeContentType(file.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessTypeDataMap = excelReader.readBusinessTypeData(sheet);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook workbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessTypeDataMap = excelXSSFReader.readBusinessTypeData(sheet);
            } else {
                throw new FileTypeNotSupportedException("Business Data Type, Excel Type: " + contentType);
            }
        } catch (IOException ex) {
            throw new FileException("Business Data Type, Excel Sheet(" + sheetIndex + ") Reader Failed! " + ex.getMessage());
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        return businessTypeDataMap;
    }

    /**
     * 获取业务数据
     *
     * @param request
     * @param businessTypeDataMap 数据类型
     * @param sheetIndex          工作表次序
     * @return
     * @throws IOException
     */
    private List<Map<String, BusinessData>> getBusinessData(Attach businessFile, HttpServletRequest request, Map<String, BusinessTypeData> businessTypeDataMap, int sheetIndex) throws IOException {
        List<Map<String, BusinessData>> businessDataMapList = new ArrayList<>();
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            String contentType = null;
            if (businessFile != null) {
                File file = new File(businessFile.getUrl());
                contentType = Files.probeContentType(file.toPath());
                fileInputStream = new FileInputStream(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
            } else {
                Part filePart = request.getPart(REQUEST_FILE_PART);
                contentType = filePart.getContentType();
                inputStream = filePart.getInputStream();
                bufferedInputStream = new BufferedInputStream(inputStream);
            }
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessDataMapList = excelReader.readBusinessData(sheet, businessTypeDataMap);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook workbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                businessDataMapList = excelXSSFReader.readBusinessData(sheet, businessTypeDataMap);
            } else {
                throw new FileTypeNotSupportedException("Business Data, Excel Type: " + contentType);
            }
        } catch (Exception ex) {
            throw new FileException("Business Data, Excel Sheet(" + sheetIndex + ") Reader Failed! " + ex.getMessage());
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        return businessDataMapList;
    }

    /**
     * 业务数据校验
     *
     * @param businessDataMapList 业务数据
     * @return
     */
    private boolean validBusinessData(List<Map<String, BusinessData>> businessDataMapList) {
        boolean isValid = true;
        for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
            for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                BusinessData businessData = businessDataEntry.getValue();
                if (!businessData.isValidate()) {
                    isValid = false;
                    break;
                }
            }
            if (!isValid) {
                break;
            }
        }

        return isValid;
    }

    /**
     * 有错误业务数据值，将批注写入对应表格，并生成新文件
     *
     * @param request
     * @param businessDataMapList 业务数据
     * @param sheetIndex          工作表次序
     * @return
     * @throws IOException
     */
    private Map<String, Object> writeBusinessData(Attach businessFile, HttpServletRequest request, HttpServletResponse response, List<Map<String, BusinessData>> businessDataMapList, int sheetIndex) throws IOException {
        Map<String, Object> attachMap = new HashMap<>();
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        OutputStream outputStream = null;
        OutputStream responseOut = null;
        FileInputStream responseIn = null;
        try {
            String contentType = null;
            String fileName = null;
            if (businessFile != null) {
                File file = new File(businessFile.getUrl());
                contentType = Files.probeContentType(file.toPath());
                fileName = businessFile.getName();
                // 输入流
                fileInputStream = new FileInputStream(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
            } else {
                // 业务文件
                Part filePart = request.getPart("file");
                contentType = filePart.getContentType();
                fileName = filePart.getSubmittedFileName();
                // 输入流
                inputStream = filePart.getInputStream();
                bufferedInputStream = new BufferedInputStream(inputStream);
            }
            String templateExt = fileName.substring(fileName.lastIndexOf("."));
            String templateName = fileName.substring(0, fileName.lastIndexOf("."));
            // 错误文件
            String errorFileName = String.format("%s：%s%s%s", templateName, "错误提示 ", dateTimeFormat.format(new Date()), templateExt);
            String directory = uploadService.getSavePath(ROOT_DIRECTORY, errorFileName, true);
            File errorFile = new File(directory);
            // 文件处理
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                // 创建CellStyle对象并设置填充颜色
                HSSFCellStyle style = workbook.createCellStyle();
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                // 写入工作表
                excelReader.writeBusinessData(sheet, style, businessDataMapList);
                // 写入文件
                outputStream = new FileOutputStream(errorFile);
                workbook.write(outputStream);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook workbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = workbook.getSheetAt(sheetIndex);
                // 创建CellStyle对象并设置填充颜色
                XSSFCellStyle style = workbook.createCellStyle();
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                // 写入工作表
                excelXSSFReader.writeBusinessData(sheet, style, businessDataMapList);
                // 写入文件
                outputStream = new FileOutputStream(errorFile);
                workbook.write(outputStream);
            } else {
                throw new FileTypeNotSupportedException("Business Data, Excel Type: " + contentType);
            }
            // 保存文件信息
            BasicFileAttributes attributes = Files.readAttributes(errorFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            attach.setGenre(IMPORT_ERROR_FILE_GENRE);
            attach.setName(errorFileName);
            attach.setType(Files.probeContentType(errorFile.toPath()));
            attach.setSize(attributes.size());
            attach.setUrl(directory);
            attachMap = attachService.createModel(attach);
            // 可下载
            /*responseOut = response.getOutputStream();
            responseIn = new FileInputStream(errorFile);
            errorFileName = URLEncoder.encode(errorFileName, "UTF-8");
            String mineType = request.getServletContext().getMimeType(errorFileName);
            response.setContentType(mineType);
            response.setHeader("Content-disposition", "attachment; filename=" + errorFileName);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = responseIn.read(buffer)) > 0) {
                responseOut.write(buffer, 0, len);
            }*/
        } catch (Exception ex) {
            throw new FileException("Business Data Error Message, Excel Sheet(" + sheetIndex + ") Writer Failed! " + ex.getMessage());
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (responseOut != null) {
                responseOut.close();
            }
            if (responseIn != null) {
                responseIn.close();
            }
        }

        return attachMap;
    }

    /**
     * 获取文件
     *
     * @param attachId
     * @return
     */
    private Attach getFile(String attachId) {
        try {
            if (Strings.isNotBlank(attachId)) {
                Attach attach = attachService.getModel(Attach.class, attachId);
                File file = new File(attach.getUrl());
                if (file.exists()) {
                    return attach;
                }
            }
        } catch (Exception ex) {
            logger.info(ex.getMessage(), ex);
        }

        return null;
    }
}
