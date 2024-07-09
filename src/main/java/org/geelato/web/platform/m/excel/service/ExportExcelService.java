package org.geelato.web.platform.m.excel.service;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.geelato.core.Ctx;
import org.geelato.core.api.ApiResult;
import org.geelato.web.platform.enums.AttachmentSourceEnum;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.entity.Base64Info;
import org.geelato.web.platform.m.base.entity.SysConfig;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.SysConfigService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.ExportColumn;
import org.geelato.web.platform.m.excel.entity.ExportTemplate;
import org.geelato.web.platform.m.excel.entity.PlaceholderMeta;
import org.geelato.web.platform.m.excel.entity.WordWaterMarkMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author diabl
 * @date 2024/3/12 14:39
 */
@Component
public class ExportExcelService {
    private static final String WORD_DOC_CONTENT_TYPE = "application/msword";
    private static final String EXCEL_XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String EXCEL_XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String WORD_DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9]{1,5}$");
    private final Logger logger = LoggerFactory.getLogger(ExportExcelService.class);
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    @Autowired
    private ExportTemplateService exportTemplateService;
    @Autowired
    private ExcelWriter excelWriter;
    @Autowired
    private ExcelXSSFWriter excelXSSFWriter;
    @Autowired
    private WordXWPFWriter wordXWPFWriter;
    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private AttachService attachService;

    /**
     * 导出文件
     *
     * @param templateId   导出模板id
     * @param fileName     导出文件名称
     * @param valueMapList 集合数据
     * @param valueMap     对象数据
     * @param markText     水印文本，默认样式
     * @param markKey      配置的水印样式
     * @param readonly     是否只读
     * @return
     */
    public ApiResult exportWps(String templateId, String fileName, List<Map> valueMapList, Map valueMap, String markText, String markKey, boolean readonly) {
        ApiResult result = new ApiResult();
        try {
            // 水印
            WordWaterMarkMeta markMeta = setWaterMark(markText, markKey);
            // 模型
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            Assert.notNull(exportTemplate, "导出模板不存在");
            // 模板
            Base64Info templateAttach = getTemplate(exportTemplate.getTemplate());
            Assert.notNull(templateAttach, "导出模板文件不存在");
            // 模板源数据
            Map<String, PlaceholderMeta> metaMap = null;
            Base64Info templateRuleAttach = getTemplate(exportTemplate.getTemplateRule());
            if (templateRuleAttach != null) {
                // 读取，模板源数据
                metaMap = getPlaceholderMeta(templateRuleAttach.getFile());
            } else if (Strings.isNotBlank(exportTemplate.getBusinessMetaData())) {
                metaMap = getPlaceholderMeta(exportTemplate.getBusinessMetaData());
            }
            if (metaMap == null || metaMap.isEmpty()) {
                throw new RuntimeException("导出模板源数据不存在！");
            }
            // 实体文件名称
            String templateExt = templateAttach.getName().substring(templateAttach.getName().lastIndexOf("."));
            String templateName = templateAttach.getName().substring(0, templateAttach.getName().lastIndexOf("."));
            if (Strings.isNotBlank(fileName)) {
                if (pattern.matcher(fileName).matches()) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
                fileName = fileName + templateExt;
            } else {
                fileName = String.format("%s_%s%s", templateName, sdf.format(new Date()), templateExt);
            }
            // 实体文件 upload/存放表/租户编码/应用Id
            String directory = UploadService.getSavePath(UploadService.ROOT_DIRECTORY, AttachmentSourceEnum.PLATFORM_ATTACH.getValue(), exportTemplate.getTenantCode(), exportTemplate.getAppId(), fileName, true);
            File exportFile = new File(directory);
            // 生成实体文件
            generateEntityFile(templateAttach.getFile(), exportFile, metaMap, valueMapList, valueMap, markMeta, readonly);
            // 保存文件信息
            BasicFileAttributes attributes = Files.readAttributes(exportFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            // attach.setObjectId(templateId);
            attach.setAppId(exportTemplate.getAppId());
            attach.setGenre("exportFile");
            attach.setName(fileName);
            attach.setType(Files.probeContentType(exportFile.toPath()));
            attach.setSize(attributes.size());
            attach.setPath(directory);
            Attach attachMap = attachService.createModel(attach);
            result.setData(attachMap);
        } catch (Exception e) {
            logger.error("表单信息导出Excel出错。", e);
            result.error().setMsg(e.getMessage());
        }

        return result;
    }


    public ApiResult exportExcelByColumnMeta(String appId, String fileName, List<Map> valueMapList, Map valueMap, List<ExportColumn> exportColumns, List<PlaceholderMeta> placeholderMetas, String markText, String markKey, boolean readonly) {
        ApiResult result = new ApiResult();
        try {
            String tenantCode = Ctx.getCurrentTenantCode();
            // 水印
            WordWaterMarkMeta markMeta = setWaterMark(markText, markKey);
            // 实体文件名称
            String templateExt = ".xlsx";
            if (Strings.isNotBlank(fileName)) {
                if (pattern.matcher(fileName).matches()) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
            } else {
                fileName = "exportExcelByColumnMeta";
            }
            // 模板源数据
            Map<String, PlaceholderMeta> metaMap = getPlaceholderMeta(placeholderMetas);
            logger.info("数据定义读取成功");
            // 生成导出模板
            String templateName = String.format("%s_%s%s", fileName, "导出模板", templateExt);
            Base64Info templateAttach = getTemplate(tenantCode, appId, templateName, exportColumns);
            Assert.notNull(templateAttach, "导出模板创建失败！");
            logger.info("导出模板生成成功！" + templateAttach.getFile().getAbsolutePath());
            // 实体文件 upload/存放表/租户编码/应用Id
            String exportFileName = String.format("%s_%s%s", fileName, sdf.format(new Date()), templateExt);
            String directory = UploadService.getSavePath(UploadService.ROOT_DIRECTORY, AttachmentSourceEnum.PLATFORM_ATTACH.getValue(), tenantCode, appId, exportFileName, true);
            File exportFile = new File(directory);
            // 生成实体文件
            generateEntityFile(templateAttach.getFile(), exportFile, metaMap, valueMapList, valueMap, markMeta, readonly);
            // 保存文件信息
            BasicFileAttributes attributes = Files.readAttributes(exportFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            attach.setAppId(appId);
            attach.setGenre("exportFile");
            attach.setName(exportFileName);
            attach.setType(Files.probeContentType(exportFile.toPath()));
            attach.setSize(attributes.size());
            attach.setPath(directory);
            Attach attachMap = attachService.createModel(attach);
            result.setData(attachMap);
        } catch (Exception e) {
            logger.error("表单信息导出Excel出错。", e);
            result.error().setMsg(e.getMessage());
        }

        return result;
    }

    /**
     * 水印处理
     *
     * @param markText
     * @param markKey
     * @return
     */
    private WordWaterMarkMeta setWaterMark(String markText, String markKey) {
        WordWaterMarkMeta meta = null;
        if (Strings.isNotBlank(markKey)) {
            Map<String, Object> params = new HashMap<>();
            params.put("configKey", markKey);
            List<SysConfig> list = sysConfigService.queryModel(SysConfig.class, params);
            if (list != null && list.size() > 0 && list.get(0) != null) {
                SysConfig config = list.get(0);
                config.afterSet();
                try {
                    if (config.isEncrypted()) {
                        SysConfigService.decrypt(config);
                    }
                    meta = JSON.parseObject(config.getConfigValue(), WordWaterMarkMeta.class);
                    Assert.notNull(meta, "水印功能，系统配置值解析为空。");
                    meta.setDefaultText(markText);
                } catch (Exception e) {
                    throw new RuntimeException("水印功能，系统配置值解析失败");
                }
            } else {
                throw new RuntimeException("水印功能，配置值查询失败");
            }
        } else if (Strings.isNotBlank(markText)) {
            meta = WordWaterMarkMeta.defaultWaterMarkMeta();
            meta.setDefaultText(markText);
        }

        return meta;
    }

    /**
     * 占位符元数据
     *
     * @param file
     * @return
     * @throws IOException
     */
    private Map<String, PlaceholderMeta> getPlaceholderMeta(File file) throws IOException {
        Map<String, PlaceholderMeta> metaMap = new HashMap<>();
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        Workbook workbook = null;
        try {
            // excel文件类型
            String contentType = Files.probeContentType(file.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = (HSSFSheet) workbook.getSheetAt(0);
                metaMap = excelWriter.readPlaceholderMeta(sheet);
                workbook.close();
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                workbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
                metaMap = excelXSSFWriter.readPlaceholderMeta(sheet);
                workbook.close();
            } else {
                throw new RuntimeException("暂不支持导出该格式文件！");
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
            if (workbook != null) {
                workbook.close();
            }
        }

        return metaMap;
    }

    /**
     * 模板数据解析
     *
     * @param jsonText
     * @return
     */
    private Map<String, PlaceholderMeta> getPlaceholderMeta(String jsonText) {
        Map<String, PlaceholderMeta> metaMap = new HashMap<>();
        try {
            List<PlaceholderMeta> metas = JSON.parseArray(jsonText, PlaceholderMeta.class);
            metaMap = getPlaceholderMeta(metas);
        } catch (Exception e) {
            metaMap = null;
        }

        return metaMap;
    }

    private Map<String, PlaceholderMeta> getPlaceholderMeta(List<PlaceholderMeta> metas) {
        Map<String, PlaceholderMeta> metaMap = new HashMap<>();
        if (metas != null && !metas.isEmpty()) {
            for (PlaceholderMeta meta : metas) {
                if (excelXSSFWriter.validatePlaceholderMeta(meta)) {
                    metaMap.put(meta.getPlaceholder(), meta);
                }
            }
        }

        return metaMap;
    }

    /**
     * 生成实体文件
     *
     * @param templateFile 模板文件
     * @param exportFile   实体文件（路径）
     * @param metaMap      占位符
     * @param valueMapList 集合数据
     * @param valueMap     单个数据
     * @throws IOException
     */
    private void generateEntityFile(File templateFile, File exportFile, Map<String, PlaceholderMeta> metaMap, List<Map> valueMapList, Map valueMap, WordWaterMarkMeta markMeta, Boolean readonly) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        OutputStream outputStream = null;
        Workbook workbook = null;
        try {
            // excel文件类型
            String contentType = Files.probeContentType(templateFile.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(templateFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                workbook = new HSSFWorkbook(fileSystem);
                // 替换占位符
                HSSFSheet sheet = (HSSFSheet) workbook.getSheetAt(0);
                excelWriter.writeSheet(sheet, metaMap, valueMapList, valueMap);
                sheet.setForceFormulaRecalculation(true);
                // 水印
                // 只读
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                workbook.write(outputStream);
                workbook.close();
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                workbook = new XSSFWorkbook(bufferedInputStream);
                // 替换占位符
                XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
                excelXSSFWriter.writeSheet(sheet, metaMap, valueMapList, valueMap);
                sheet.setForceFormulaRecalculation(true);
                // 水印
                // 只读
                if (readonly) {
                    ((XSSFWorkbook) workbook).lockWindows();
                }
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                workbook.write(outputStream);
                outputStream.flush();
                workbook.close();
            } else if (WORD_DOC_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HWPFDocument document = new HWPFDocument(fileSystem);
                // 替换占位符
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                document.write(outputStream);
                outputStream.flush();
                document.close();
            } else if (WORD_DOCX_CONTENT_TYPE.equals(contentType)) {
                XWPFDocument document = new XWPFDocument(bufferedInputStream);
                document.getParagraphs();
                // 替换占位符
                wordXWPFWriter.writeDocument(document, metaMap, valueMapList, valueMap);
                // 写入水印
                DocxWaterMarkUtils.setXWPFDocumentWaterMark(document, markMeta);
                // 只读
                if (readonly) {
                    document.enforceReadonlyProtection();
                }
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                document.write(outputStream);
                outputStream.flush();
                document.close();
            } else {
                throw new RuntimeException("暂不支持导出该格式文件！");
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (workbook != null) {
                workbook.close();
            }
        }
    }


    private Base64Info getTemplate(String tenantCode, String appId, String fileName, List<ExportColumn> exportColumns) throws IOException {
        Base64Info info = null;
        OutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        FileInputStream fileInputStream = null;
        try {
            // 创建文件
            String exportPath = UploadService.getSavePath(UploadService.ROOT_DIRECTORY, AttachmentSourceEnum.PLATFORM_ATTACH.getValue(), tenantCode, appId, fileName, true);
            // 读取文件，
            workbook = new XSSFWorkbook();
            excelXSSFWriter.generateTemplateFile(workbook, "list", exportColumns);
            // 输出数据到文件
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(exportPath)));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.close();
            // 保存附件
            Attach attach = new Attach(new File(exportPath));
            attach.setName(fileName);
            attach.setPath(exportPath);
            attach.setGenre("exportTemplate");
            attach.setAppId(appId);
            attach = attachService.createModel(attach);
            // 数据转换
            info = Base64Info.getBase64InfoByAttach(attach);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (workbook != null) {
                workbook.close();
            }
        }

        return info;
    }

    /**
     * 将base64转为file
     *
     * @param template
     * @return
     */
    private Base64Info getTemplate(String template) {
        Base64Info info = null;
        if (Strings.isNotBlank(template)) {
            if (template.length() > 64) {
                try {
                    Base64Info bi = JSON.parseObject(template, Base64Info.class);
                    if (bi != null && Strings.isNotBlank(bi.getName()) && Strings.isNotBlank(bi.getBase64())) {
                        // 解码Base64字符串为字节数组
                        byte[] decodedBytes = Base64.getDecoder().decode(bi.getBase64());
                        // 创建临时文件
                        String fileExt = bi.getName().substring(bi.getName().lastIndexOf("."));
                        File tempFile = File.createTempFile("temp_base64_export", fileExt);
                        tempFile.deleteOnExit();
                        // 将字节数组写入临时文件
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            fos.write(decodedBytes);
                        }
                        // 将临时文件吸入file
                        info = bi;
                        info.setFile(tempFile);
                        logger.info(String.format("base64Name：%s；tempFilePath：%s", bi.getName(), info.getFile().getAbsolutePath()));
                    }
                } catch (Exception ex) {
                    logger.info(ex.getMessage(), ex);
                }
            } else {
                Attach attach = getFile(template);
                if (attach != null) {
                    info = Base64Info.getBase64InfoByAttach(attach);
                    logger.info(String.format("AttachName：%s；tempFilePath：%s", attach.getName(), info.getFile().getAbsolutePath()));
                }
            }
        }

        return info;
    }

    /**
     * 获取文件
     *
     * @param attachId
     * @return
     */
    private Attach getFile(String attachId) {
        if (Strings.isNotBlank(attachId)) {
            Attach attach = attachService.getModel(attachId);
            File file = new File(attach.getPath());
            if (file.exists()) {
                return attach;
            }
        }

        return null;
    }

    /**
     * 获取查询sql
     *
     * @param request
     * @return
     */
    public String getGql(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = request.getReader();
        } catch (IOException e) {
            logger.error("未能从httpServletRequest中获取gql的内容", e);
        }
        String str;
        try {
            while ((str = br.readLine()) != null) {
                stringBuilder.append(str);
            }
        } catch (IOException e) {
            logger.error("未能从httpServletRequest中获取gql的内容", e);
        }

        return stringBuilder.toString();
    }

}
