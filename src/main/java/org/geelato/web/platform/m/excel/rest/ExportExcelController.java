package org.geelato.web.platform.m.excel.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.ExportTemplate;
import org.geelato.web.platform.m.excel.entity.PlaceholderMeta;
import org.geelato.web.platform.m.excel.service.ExcelWriter;
import org.geelato.web.platform.m.excel.service.ExcelXSSFWriter;
import org.geelato.web.platform.m.excel.service.ExportTemplateService;
import org.geelato.web.platform.m.excel.service.WordXWPFWriter;
import org.geelato.web.platform.m.security.entity.DataItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.regex.Pattern;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/2 15:23
 */
@Controller
@RequestMapping(value = "/api/export/file")
public class ExportExcelController extends BaseController {
    private static final String ROOT_DIRECTORY = "upload";
    private static final String WORD_DOC_CONTENT_TYPE = "application/msword";
    private static final String EXCEL_XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    private static final String EXCEL_XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String WORD_DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Pattern pattern = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9]{1,5}$");
    private final Logger logger = LoggerFactory.getLogger(ExportExcelController.class);
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
    private UploadService uploadService;
    @Autowired
    private AttachService attachService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public ApiPagedResult pageQuery(HttpServletRequest req) {
        ApiPagedResult result = new ApiPagedResult();
        try {
            int pageNum = Strings.isNotBlank(req.getParameter("current")) ? Integer.parseInt(req.getParameter("current")) : -1;
            int pageSize = Strings.isNotBlank(req.getParameter("pageSize")) ? Integer.parseInt(req.getParameter("pageSize")) : -1;
            Map<String, Object> params = this.getQueryParameters(Attach.class, req);

            List<Attach> pageQueryList = attachService.pageQueryModel(Attach.class, pageNum, pageSize, params);
            List<Attach> queryList = attachService.queryModel(Attach.class, params);

            result.setTotal(queryList != null ? queryList.size() : 0);
            result.setData(new DataItems(pageQueryList, result.getTotal()));
            result.setPage(pageNum);
            result.setSize(pageSize);
            result.setDataSize(pageQueryList != null ? pageQueryList.size() : 0);
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.QUERY_FAIL);
        }

        return result;
    }

    /**
     * 导出excel
     *
     * @param request
     * @param response
     * @param dataType   数据来源，mql、data
     * @param templateId 模板id
     * @param fileName   导出文件名称
     */
    @RequestMapping(value = "/{dataType}/{templateId}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ApiResult exportWps(HttpServletRequest request, HttpServletResponse response, @PathVariable String dataType, @PathVariable String templateId, String fileName) {
        ApiResult result = new ApiResult();
        try {
            // 表单数据
            String jsonText = getGql(request);
            List<Map> valueMapList = new ArrayList<>();
            Map valueMap = new HashMap();
            // List<BatchGetFormDataByIdListResponseBody.BatchGetFormDataByIdListResponseBodyResult> resultList = null;
            if ("mql".equals(dataType)) {
                // todo 查询接口
            } else if ("data".equals(dataType)) {
                JSONObject jo = JSON.parseObject(jsonText);
                valueMapList = (List<Map>) jo.get("valueMapList");
                valueMap = (Map) jo.get("valueMap");
            } else {
                throw new RuntimeException("暂不支持解析该数据类型！");
            }
            // 模型
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            Assert.notNull(exportTemplate, "导出模板不存在");
            // 模板
            Attach templateAttach = getFile(exportTemplate.getTemplate());
            Assert.notNull(templateAttach, "导出模板不存在");
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
            // 实体文件
            String directory = uploadService.getSavePath(ROOT_DIRECTORY, fileName, true);
            File exportFile = new File(directory);
            // 模板源数据
            Attach templateRuleAttach = getFile(exportTemplate.getTemplateRule());
            Assert.notNull(templateRuleAttach, "导出模板源数据不存在");
            // 读取，模板源数据
            Map<String, PlaceholderMeta> metaMap = getPlaceholderMeta(new File(templateRuleAttach.getUrl()));
            // 生成实体文件
            generateEntityFile(new File(templateAttach.getUrl()), exportFile, metaMap, valueMapList, valueMap);

            // 保存文件信息
            BasicFileAttributes attributes = Files.readAttributes(exportFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            attach.setGenre("exportFile");
            attach.setName(fileName);
            attach.setType(Files.probeContentType(exportFile.toPath()));
            attach.setSize(attributes.size());
            attach.setUrl(directory);
            attachService.createModel(attach);
        } catch (Exception e) {
            logger.error("表单信息导出Excel出错。", e);
            result.error().setMsg(e.getMessage());
        }

        return result;
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
        try {
            // excel文件类型
            String contentType = Files.probeContentType(file.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                HSSFSheet sheet = workbook.getSheetAt(0);
                metaMap = excelWriter.readPlaceholderMeta(sheet);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook templateWorkbook = new XSSFWorkbook(bufferedInputStream);
                XSSFSheet sheet = templateWorkbook.getSheetAt(0);
                metaMap = excelXSSFWriter.readPlaceholderMeta(sheet);
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
    private void generateEntityFile(File templateFile, File exportFile, Map<String, PlaceholderMeta> metaMap, List<Map> valueMapList, Map valueMap) throws IOException {
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        OutputStream outputStream = null;
        try {
            // excel文件类型
            String contentType = Files.probeContentType(templateFile.toPath());
            // 读取文件
            fileInputStream = new FileInputStream(templateFile);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
            if (EXCEL_XLS_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HSSFWorkbook workbook = new HSSFWorkbook(fileSystem);
                // 替换占位符
                HSSFSheet sheet = workbook.getSheetAt(0);
                excelWriter.writeSheet(sheet, metaMap, valueMapList, valueMap);
                sheet.setForceFormulaRecalculation(true);
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                workbook.write(outputStream);
            } else if (EXCEL_XLSX_CONTENT_TYPE.equals(contentType)) {
                XSSFWorkbook workbook = new XSSFWorkbook(bufferedInputStream);
                // 替换占位符
                XSSFSheet sheet = workbook.getSheetAt(0);
                excelXSSFWriter.writeSheet(sheet, metaMap, valueMapList, valueMap);
                sheet.setForceFormulaRecalculation(true);
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                workbook.write(outputStream);
            } else if (WORD_DOC_CONTENT_TYPE.equals(contentType)) {
                POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
                HWPFDocument document = new HWPFDocument(fileSystem);
                // 替换占位符
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                document.write(outputStream);
            } else if (WORD_DOCX_CONTENT_TYPE.equals(contentType)) {
                XWPFDocument document = new XWPFDocument(bufferedInputStream);
                document.getParagraphs();
                // 替换占位符
                wordXWPFWriter.writeDocument(document, metaMap, valueMapList, valueMap);
                // 写入文件
                outputStream = new FileOutputStream(exportFile);
                document.write(outputStream);
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
        }
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

    /**
     * 获取查询sql
     *
     * @param request
     * @return
     */
    private String getGql(HttpServletRequest request) {
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
