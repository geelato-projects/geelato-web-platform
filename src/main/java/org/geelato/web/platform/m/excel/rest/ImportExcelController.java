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
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.constants.ColumnDefault;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.enums.MysqlDataTypeEnum;
import org.geelato.core.gql.parser.FilterGroup;
import org.geelato.core.meta.MetaManager;
import org.geelato.core.meta.model.entity.EntityMeta;
import org.geelato.core.meta.model.field.ColumnMeta;
import org.geelato.core.meta.model.field.FieldMeta;
import org.geelato.core.script.js.JsProvider;
import org.geelato.utils.UIDGenerator;
import org.geelato.web.platform.enums.ExcelColumnTypeEnum;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.entity.Dict;
import org.geelato.web.platform.m.base.entity.DictItem;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.*;
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
import java.util.concurrent.TimeUnit;

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
    private static final int REDIS_TIME_OUT = 60;
    private static final int GGL_QUERY_TOTAL = 10000;
    private static final String ROOT_DIRECTORY = "upload";
    private static final String REQUEST_FILE_PART = "file";
    private final Logger logger = LoggerFactory.getLogger(ImportExcelController.class);
    private final FilterGroup filterGroup = new FilterGroup().addFilter(ColumnDefault.DEL_STATUS_FIELD, String.valueOf(DeleteStatusEnum.NO.getCode()));
    private final MetaManager metaManager = MetaManager.singleInstance();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");
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
            result.error().setMsg(ex.getMessage());
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
            result.error().setMsg(ex.getMessage());
        }
        return result;
    }

    public ApiResult importExcel(HttpServletRequest request, HttpServletResponse response, String importType, String templateId, String attachId) {
        ApiResult result = new ApiResult();
        String currentUUID = String.valueOf(UIDGenerator.generate());
        try {
            // 文件内容
            Map<String, List<BusinessMeta>> businessMetaListMap = new HashMap<>();// 元数据
            Map<String, BusinessTypeData> businessTypeDataMap = new HashMap<>();// 数据类型
            List<Map<String, BusinessData>> businessDataMapList = new ArrayList<>();// 业务数据
            // 事务模板查询
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            Assert.notNull(exportTemplate, "导入事务不存在");
            //事务，模板元数据
            Attach templateRuleAttach = getFile(exportTemplate.getTemplateRule());
            Assert.notNull(templateRuleAttach, "导入模板元数据不存在");
            businessMetaListMap = getBusinessMeta(new File(templateRuleAttach.getUrl()), 1);
            //事务，模板数据类型
            // Attach templateAttach = getFile(exportTemplate.getTemplate());
            // Assert.notNull(templateAttach, "导入模板不存在");
            businessTypeDataMap = getBusinessTypeData(new File(templateRuleAttach.getUrl()), 0);
            // 事务，业务数据
            File businessFile = null;
            if (Strings.isNotBlank(attachId)) {
                Attach uploadFile = getFile(attachId);
                Assert.notNull(uploadFile, "导入业务数据不存在");
                businessFile = new File(uploadFile.getUrl());
            }
            businessDataMapList = getBusinessData(businessFile, request, businessTypeDataMap, 0);
            // 设置缓存
            List<String> cacheKeys = setCache(currentUUID, businessMetaListMap, businessDataMapList);
            // 获取
            Map<String, List<Map<String, Object>>> tableData = new HashMap<>();
            for (Map.Entry<String, List<BusinessMeta>> metaMap : businessMetaListMap.entrySet()) {
                // 获取表格字段信息
                EntityMeta entityMeta = metaManager.getByEntityName(metaMap.getKey(), false);
                // 当前字段
                List<Map<String, Object>> columnData = new ArrayList<>();
                for (Map<String, BusinessData> businessDataMap : businessDataMapList) {
                    // 一行业务数据，键值对
                    Map<String, Object> valueMap = new HashMap<>();
                    for (Map.Entry<String, BusinessData> businessDataEntry : businessDataMap.entrySet()) {
                        valueMap.put(businessDataEntry.getKey(), businessDataEntry.getValue().getValue());
                    }
                    // 一行数据库数据
                    Map<String, Object> columnMap = new HashMap<>();
                    for (BusinessMeta meta : metaMap.getValue()) {
                        FieldMeta fieldMeta = entityMeta.getFieldMeta(meta.getColumnName());
                        BusinessData businessData = businessDataMap.get(meta.getVariableValue());
                        Object value = null;
                        try {
                            // 获取值
                            value = getValue(currentUUID, fieldMeta.getColumn(), meta, businessData, valueMap);
                            // 验证值
                            List<String> errorMsg = validateValue(fieldMeta.getColumn(), businessData.getBusinessTypeData(), value);
                            businessData.setErrorMsgs(errorMsg);
                        } catch (Exception ex) {
                            businessData.setErrorMsg(ex.getMessage());
                        }
                        columnMap.put(meta.getColumnName(), value);
                    }
                    columnData.add(columnMap);
                }
                tableData.put(metaMap.getKey(), columnData);
            }
            // 释放缓存
            redisTemplate.delete(cacheKeys);
            // 业务数据校验
            if (!validBusinessData(businessDataMapList)) {
                Map<String, Object> errorAttach = writeBusinessData(businessFile, request, businessDataMapList, 0);
                return result.error().setData(errorAttach).setMsg("请下载错误文件，查看具体错误信息");
            }
            // 插入数据 "@biz": "myBizCode",
            if (!tableData.isEmpty()) {
                Map<String, Object> insertMap = new HashMap<>();
                insertMap.put("@biz", "myBizCode");
                for (Map.Entry<String, List<Map<String, Object>>> table : tableData.entrySet()) {
                    insertMap.put(table.getKey(), table.getValue());
                }
                ruleService.batchSave(JSON.toJSONString(insertMap));
            } else {
                result.error().setMsg("没有需要导入的数据");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            result.error().setMsg(ex.getMessage());
        }

        return result;
    }


    /**
     * 业务数据类型与元数据类型校验
     *
     * @param columnMeta 元数据
     * @param typeData   业务数据类型
     * @param value      值
     * @return
     */
    private List<String> validateValue(ColumnMeta columnMeta, BusinessTypeData typeData, Object value) {
        List<String> errorMsg = new ArrayList<>();
        if (MysqlDataTypeEnum.getBooleans().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.BOOLEAN.name().equalsIgnoreCase(typeData.getType())) {

        } else if (MysqlDataTypeEnum.getStrings().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.STRING.name().equalsIgnoreCase(typeData.getType())) {
            if (value != null && String.valueOf(value).length() > columnMeta.getCharMaxLength()) {
                errorMsg.add(String.valueOf(value).length() + " 已超出字段最大长度：" + columnMeta.getCharMaxLength());
            }
        } else if (MysqlDataTypeEnum.getNumbers().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.NUMBER.name().equalsIgnoreCase(typeData.getType())) {
            if (value != null && String.valueOf(value).length() > (columnMeta.getNumericPrecision() + columnMeta.getNumericScale())) {
                errorMsg.add(String.valueOf(value).length() + " 已超出字段数值位数限制：" + (columnMeta.getNumericPrecision() + columnMeta.getNumericScale()));
            }
        } else if (MysqlDataTypeEnum.getDates().contains(columnMeta.getDataType()) && ExcelColumnTypeEnum.DATETIME.name().equalsIgnoreCase(typeData.getType())) {

        } else {
            errorMsg.add("业务数据格式：" + typeData.getType() + "；而数据库存储格式为：" + columnMeta.getDataType());
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
            List<Map<String, Object>> mapList = (List<Map<String, Object>>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, meta.getPrimaryValue()));
            if (businessData.getValue() != null && mapList != null && mapList.size() > 0) {
                for (Map<String, Object> map : mapList) {
                    if (businessData.getValue().equals(map.get(meta.getColumnName()))) {
                        value = map.get("id");
                    }
                }
            }
        } else if (meta.isEvaluationTypeDictionary()) {
            List<DictItem> dictItems = (List<DictItem>) redisTemplate.opsForValue().get(String.format("%s:%s", currentUUID, meta.getDictCode()));
            if (dictItems != null && dictItems.size() > 0) {
                for (DictItem item : dictItems) {
                    if (item.getItemName().equalsIgnoreCase(String.valueOf(businessData.getValue()))) {
                        value = item.getItemCode();
                        break;
                    }
                }
            }
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
                throw new RuntimeException("暂不支持该格式的导入数据模板文件！");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
                throw new RuntimeException("暂不支持该格式的导入数据模板文件！");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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
    private List<Map<String, BusinessData>> getBusinessData(File file, HttpServletRequest request, Map<String, BusinessTypeData> businessTypeDataMap, int sheetIndex) throws IOException {
        List<Map<String, BusinessData>> businessDataMapList = new ArrayList<>();
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            String contentType = null;
            if (file != null) {
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
                throw new RuntimeException("暂不支持导入该格式文件！");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
    private Map<String, Object> writeBusinessData(File file, HttpServletRequest request, List<Map<String, BusinessData>> businessDataMapList, int sheetIndex) throws IOException {
        Map<String, Object> attachMap = new HashMap<>();
        InputStream inputStream = null;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        OutputStream outputStream = null;
        try {
            String contentType = null;
            String fileName = null;
            if (file != null) {
                contentType = Files.probeContentType(file.toPath());
                fileName = file.getName();
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
                throw new RuntimeException("暂不支持导入该格式文件！");
            }
            // 保存文件信息
            BasicFileAttributes attributes = Files.readAttributes(errorFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            attach.setGenre("importErrorFile");
            attach.setName(errorFileName);
            attach.setType(Files.probeContentType(errorFile.toPath()));
            attach.setSize(attributes.size());
            attach.setUrl(directory);
            attachMap = attachService.createModel(attach);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
        }

        return attachMap;
    }

    /**
     * 设置 缓存，数据字典、主键
     *
     * @param currentUUID 当前主键
     * @param tableMeta   元数据
     * @param data        业务数据
     * @return
     */
    private List<String> setCache(String currentUUID, Map<String, List<BusinessMeta>> tableMeta, List<Map<String, BusinessData>> data) {
        List<String> cacheList = new ArrayList<>();
        // 元数据
        List<ConditionMeta> dictMetas = new ArrayList<>();
        List<ConditionMeta> primaryMetas = new ArrayList<>();
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
                        conditionMeta.setColumnName(meta.getPrimaryKeyColumn());
                    }
                    if (conditionMeta != null) {
                        List<String> values = new ArrayList<>();
                        for (Map<String, BusinessData> map : data) {
                            BusinessData businessData = map.get(meta.getVariableValue());
                            values.add(String.valueOf(businessData.getValue()));
                        }
                        conditionMeta.setValues(values);
                        if (meta.isEvaluationTypeDictionary()) {
                            dictMetas.add(conditionMeta);
                        } else if (meta.isEvaluationTypePrimaryKey()) {
                            primaryMetas.add(conditionMeta);
                        }
                    }
                }
            }
        }
        dao.setDefaultFilter(true, filterGroup);
        // 数据字典
        List<String> dictKeys = setDictRedis(currentUUID, dictMetas);
        cacheList.containsAll(dictKeys);
        // 主键
        List<String> primaryKeys = setPrimaryRedis(currentUUID, primaryMetas);
        cacheList.containsAll(primaryKeys);

        return cacheList;
    }

    /**
     * 数据字典缓存
     *
     * @param currentUUID
     * @param dictMetas
     * @return
     */
    private List<String> setDictRedis(String currentUUID, List<ConditionMeta> dictMetas) {
        List<String> dictKeys = new ArrayList<>();
        if (dictMetas != null && dictMetas.size() > 0) {
            List<String> dictCodes = new ArrayList<>();
            List<String> dictItemNames = new ArrayList<>();
            for (ConditionMeta conditionMeta : dictMetas) {
                dictCodes.add(conditionMeta.getDictCode());
                dictItemNames.addAll(conditionMeta.getValues());
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
                filter1.addFilter("itemName", FilterGroup.Operator.in, String.join(",", dictItemNames));
                dictItemList = dao.queryList(DictItem.class, filter1, "");
                // 存入缓存
                for (Dict dict : dictList) {
                    String dictKey = String.format("%s:%s", currentUUID, dict.getDictCode());
                    List<DictItem> dictItems = new ArrayList<>();
                    if (dictItemList != null && dictItemList.size() > 0) {
                        for (DictItem dictItem : dictItemList) {
                            if (dict.getId().equalsIgnoreCase(dictItem.getDictId())) {
                                dictItems.add(dictItem);
                            }
                        }
                        logger.info(dictKey + " - " + JSON.toJSONString(dictItems));
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
     * @param currentUUID
     * @param primaryMetas
     * @return
     */
    private List<String> setPrimaryRedis(String currentUUID, List<ConditionMeta> primaryMetas) {
        List<String> primaryKeys = new ArrayList<>();
        String gglFormat = "{\"%s\": {\"@fs\": \"id,%s\", \"%s|eq\": \"%s\", \"@p\": \"1,%s\"}}";
        if (primaryMetas != null && primaryMetas.size() > 0) {
            for (ConditionMeta meta : primaryMetas) {
                if (meta != null && meta.getValues() != null && meta.getValues().size() > 0) {
                    String primaryKey = String.format("%s:%s:%s", currentUUID, meta.getTableName(), meta.getColumnName());
                    String limit = String.valueOf(GGL_QUERY_TOTAL * Math.ceil(meta.getValues().size() / GGL_QUERY_TOTAL));
                    String ggl = String.format(gglFormat, meta.getTableName(), meta.getColumnName(), meta.getColumnName(), String.join(",", meta.getValues()), limit);
                    ApiPagedResult page = ruleService.queryForMapList(ggl, false);
                    logger.info(primaryKey + " - " + page.getData());
                    redisTemplate.opsForValue().set(primaryKey, page.getData(), REDIS_TIME_OUT, TimeUnit.MINUTES);
                    primaryKeys.add(primaryKey);
                }
            }
        }

        return primaryKeys;
    }

    /**
     * 获取文件
     *
     * @param attachId
     * @return
     */
    private Attach getFile(String attachId) {
        if (Strings.isNotBlank(attachId)) {
            Attach attach = attachService.getModel(Attach.class, attachId);
            File file = new File(attach.getUrl());
            if (file.exists()) {
                return attach;
            }
        }

        return null;
    }

}
