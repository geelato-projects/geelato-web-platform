package org.geelato.web.platform.m.excel.service;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.web.platform.enums.AttachmentSourceEnum;
import org.geelato.web.platform.m.base.entity.Resources;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.base.service.ResourcesService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/8/11 15:05
 */
@Component
public class ExportTemplateService extends BaseService {
    public static final String[] EXPORT_META_HEADER = {"占位符", "变量", "列表变量", "常量值", "表达式", "值类型", "取值计算方式", "是否列表", "是否合并", "合并唯一约束", "是否图片", "图片宽度cm", "图片高度cm", "备注"};
    public static final String[] IMPORT_META_TYPE_HEADER = {"列名", "类型", "格式", "多值分隔符", "多值场景", "清洗规则", "备注"};
    public static final String[] IMPORT_META_META_HEADER = {"表格", "字段名称", "取值计算方式", "常量取值", "变量取值", "表达式取值", "数据字典取值", "模型取值", "备注"};
    private static final String ROOT_DIRECTORY = "upload";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    private final Logger logger = LoggerFactory.getLogger(ExportTemplateService.class);
    @Autowired
    private UploadService uploadService;
    @Autowired
    private ResourcesService resourcesService;

    public ApiResult generateFile(String id, String fileType) throws IOException {
        ApiResult result = new ApiResult();
        if (Strings.isBlank(id) || Strings.isBlank(fileType)) {
            return result.error().setMsg(ApiErrorMsg.PARAMETER_MISSING);
        }
        String[] fileTypes = fileType.split(",");
        ExportTemplate exportTemplate = this.getModel(ExportTemplate.class, id);
        Assert.notNull(exportTemplate, ApiErrorMsg.QUERY_FAIL);
        if (Arrays.asList(fileTypes).contains("template") && "import".equalsIgnoreCase(exportTemplate.getUseType()) && Strings.isNotBlank(exportTemplate.getBusinessTypeData())) {
            result = this.generateImportTemplate(exportTemplate);
        }
        if (Arrays.asList(fileTypes).contains("meta")) {
            if ("import".equalsIgnoreCase(exportTemplate.getUseType()) && Strings.isNotBlank(exportTemplate.getBusinessMetaData()) && Strings.isNotBlank(exportTemplate.getBusinessRuleData()) && Strings.isNotBlank(exportTemplate.getBusinessTypeData())) {
                result = this.generateImportMeta(exportTemplate);
            } else if ("export".equalsIgnoreCase(exportTemplate.getUseType()) && Strings.isNotBlank(exportTemplate.getBusinessMetaData())) {
                result = this.generateExportMeta(exportTemplate);
            }
        }

        return result;
    }

    private ApiResult generateImportTemplate(ExportTemplate meta) throws IOException {
        ApiResult result = new ApiResult();
        OutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        FileInputStream fileInputStream = null;
        try {
            List<BusinessTypeData> businessTypeData = JSON.parseArray(meta.getBusinessTypeData(), BusinessTypeData.class);
            if (businessTypeData == null || businessTypeData.isEmpty()) {
                return result.error().setMsg("Excel模板字段定义不存在，无法生成文件！");
            }
            // 创建文件，
            String excelPath = getSavePath(meta, "import-template.xlsx", true);
            // 读取文件，
            workbook = new XSSFWorkbook();
            importTemplateSheet(workbook, "IMPORT TEMPLATE", businessTypeData);
            // 输出数据到文件
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(excelPath)));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.close();
            // 保存附件
            String excelFileName = String.format("%s：导入模板 %s.xlsx", meta.getTitle(), sdf.format(new Date()));
            Resources attach = saveAttach(meta, excelPath, excelFileName);
            // 转base64
            fileInputStream = new FileInputStream(new File(excelPath));
            Map<String, Object> templateMap = fileToBase64(fileInputStream, attach);
            // 模板数据处理，保留备份
            backupsAndUpdateExportTemplate(meta, JSON.toJSONString(templateMap), null);

            result.success().setData(attach);
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

        return result;
    }

    private ApiResult generateImportMeta(ExportTemplate meta) throws IOException {
        ApiResult result = new ApiResult();
        OutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        FileInputStream fileInputStream = null;
        try {
            List<BusinessTypeData> businessTypeData = JSON.parseArray(meta.getBusinessTypeData(), BusinessTypeData.class);
            if (businessTypeData == null || businessTypeData.isEmpty()) {
                return result.error().setMsg("Excel模板字段定义不存在，无法生成文件！");
            }
            List<BusinessTypeRuleData> businessTypeRuleData = JSON.parseArray(meta.getBusinessRuleData(), BusinessTypeRuleData.class);
            if (businessTypeRuleData == null || businessTypeRuleData.isEmpty()) {
                return result.error().setMsg("Excel模板数据处理规则不存在，无法生成文件！");
            }
            List<BusinessMeta> businessMetaData = JSON.parseArray(meta.getBusinessMetaData(), BusinessMeta.class);
            if (businessMetaData == null || businessMetaData.isEmpty()) {
                return result.error().setMsg("数据保存配置不存在，无法生成文件！");
            }
            // 创建文件，
            String excelPath = getSavePath(meta, "import-meta.xlsx", true);
            // 读取文件，
            workbook = new XSSFWorkbook();
            importMetaTypeSheet(workbook, "Excel模板字段定义", businessTypeData);
            importMetaRuleSheet(workbook, "Excel模板数据处理规则", businessTypeRuleData);
            importMetaMetaSheet(workbook, "数据保存配置", businessMetaData);
            // 输出数据到文件
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(excelPath)));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.close();
            // 保存附件
            String excelFileName = String.format("%s：元数据 %s.xlsx", meta.getTitle(), sdf.format(new Date()));
            Resources attach = saveAttach(meta, excelPath, excelFileName);
            // 转base64
            fileInputStream = new FileInputStream(new File(excelPath));
            Map<String, Object> templateMap = fileToBase64(fileInputStream, attach);
            // 模板数据处理，保留备份
            backupsAndUpdateExportTemplate(meta, null, JSON.toJSONString(templateMap));

            result.success().setData(attach);
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

        return result;
    }

    private ApiResult generateExportMeta(ExportTemplate meta) throws IOException {
        ApiResult result = new ApiResult();
        OutputStream outputStream = null;
        XSSFWorkbook workbook = null;
        FileInputStream fileInputStream = null;
        try {
            List<PlaceholderMeta> placeholderMetas = JSON.parseArray(meta.getBusinessMetaData(), PlaceholderMeta.class);
            if (placeholderMetas == null || placeholderMetas.isEmpty()) {
                return result.error().setMsg("数据保存配置不存在，无法生成文件！");
            }
            // 创建文件，
            String excelPath = getSavePath(meta, "export-meta.xlsx", true);
            // 读取文件，
            workbook = new XSSFWorkbook();
            exportMetaSheet(workbook, "EXPORT META", placeholderMetas);
            // 输出数据到文件
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(excelPath)));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.close();
            // 保存附件
            String excelFileName = String.format("%s：元数据 %s.xlsx", meta.getTitle(), sdf.format(new Date()));
            Resources attach = saveAttach(meta, excelPath, excelFileName);
            // 转base64
            fileInputStream = new FileInputStream(new File(excelPath));
            Map<String, Object> templateMap = fileToBase64(fileInputStream, attach);
            // 模板数据处理，保留备份
            backupsAndUpdateExportTemplate(meta, null, JSON.toJSONString(templateMap));

            result.success().setData(attach);
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

        return result;
    }

    /**
     * 往文档中写入信息，导入功能，模板
     *
     * @param workbook
     * @param sheetName
     * @param metas
     */
    private void importTemplateSheet(XSSFWorkbook workbook, String sheetName, List<BusinessTypeData> metas) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 创建字体样式
        XSSFCellStyle headerCellStyle = getHeaderCellStyle(workbook);
        // 写入信息，
        XSSFRow row = sheet.createRow(0);
        for (int i = 0; i < metas.size(); i++) {
            BusinessTypeData data = metas.get(i);
            XSSFCell cell = row.createCell(i);
            cell.setCellStyle(headerCellStyle);
            if (Strings.isNotBlank(data.getName())) {
                cell.setCellValue(data.getName());
                // 调整列宽
                setColumnWidth(sheet, data.getName(), i);
            }
            // 备注
            String mark = String.format("类型：%s；\r\n格式：%s；\r\n说明：%s；", data.getType(), data.getFormat(), data.getRemark());
            setCellComment(sheet, cell, mark);
        }
        //  冻结第一行，列不冻结
        sheet.createFreezePane(0, 1);
    }

    /**
     * 往文档中写入信息，导入功能，数据类型
     *
     * @param workbook
     * @param sheetName
     * @param metas
     */
    private void importMetaTypeSheet(XSSFWorkbook workbook, String sheetName, List<BusinessTypeData> metas) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入表头
        XSSFRow row = sheet.createRow(0);
        XSSFCellStyle headerCellStyle = getHeaderCellStyle(workbook);
        for (int i = 0; i < IMPORT_META_TYPE_HEADER.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellStyle(headerCellStyle);
            cell.setCellValue(IMPORT_META_TYPE_HEADER[i]);
        }
        // 写入数据
        XSSFCellStyle cellStyle = getCellStyle(workbook);
        for (int i = 0; i < metas.size(); i++) {
            XSSFRow dRow = sheet.createRow(i + 1);
            setCell(dRow, 0, cellStyle, metas.get(i).getName());
            setCell(dRow, 1, cellStyle, metas.get(i).getType());
            setCell(dRow, 2, cellStyle, metas.get(i).getFormat());
            setCell(dRow, 3, cellStyle, "");
            setCell(dRow, 4, cellStyle, "");
            setCell(dRow, 5, cellStyle, "");
            setCell(dRow, 6, cellStyle, metas.get(i).getRemark());
        }
        // 设置列宽
        setColumnWidth(sheet, 0, IMPORT_META_TYPE_HEADER.length);
    }

    /**
     * 往文档中写入信息，导入功能，清洗规则
     *
     * @param workbook
     * @param sheetName
     * @param metas
     */
    private void importMetaRuleSheet(XSSFWorkbook workbook, String sheetName, List<BusinessTypeRuleData> metas) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入表头
        CellRangeAddress mergedRegion1 = new CellRangeAddress(0, 1, 0, 0);
        sheet.addMergedRegion(mergedRegion1);
        CellRangeAddress mergedRegion2 = new CellRangeAddress(0, 0, 1, 5);
        sheet.addMergedRegion(mergedRegion2);
        CellRangeAddress mergedRegion3 = new CellRangeAddress(0, 1, 6, 6);
        sheet.addMergedRegion(mergedRegion3);
        XSSFCellStyle headerCellStyle = getHeaderCellStyle(workbook);
        XSSFRow row1 = sheet.createRow(0);
        setCell(row1, 0, headerCellStyle, "处理列名");
        setCell(row1, 1, headerCellStyle, "清洗规则");
        setCell(row1, 6, headerCellStyle, "说明");
        XSSFRow row2 = sheet.createRow(1);
        setCell(row2, 1, headerCellStyle, "类型");
        setCell(row2, 2, headerCellStyle, "规则");
        setCell(row2, 3, headerCellStyle, "目标");
        setCell(row2, 4, headerCellStyle, "保留原值");
        setCell(row2, 5, headerCellStyle, "次序");
        // 写入数据
        XSSFCellStyle cellStyle = getCellStyle(workbook);
        for (int i = 0; i < metas.size(); i++) {
            XSSFRow dRow = sheet.createRow(i + 2);
            setCell(dRow, 0, cellStyle, metas.get(i).getColumnName());
            setCell(dRow, 1, cellStyle, metas.get(i).getType());
            setCell(dRow, 2, cellStyle, metas.get(i).getRule());
            setCell(dRow, 3, cellStyle, metas.get(i).getGoal());
            setCell(dRow, 4, cellStyle, metas.get(i).isRetain());
            setCell(dRow, 5, cellStyle, metas.get(i).getOrder());
            setCell(dRow, 6, cellStyle, metas.get(i).getRemark());
        }
        // 设置列宽
        setColumnWidth(sheet, 0, 7);
    }

    /**
     * 往文档中写入信息，导入功能，元数据
     *
     * @param workbook
     * @param sheetName
     * @param metas
     */
    private void importMetaMetaSheet(XSSFWorkbook workbook, String sheetName, List<BusinessMeta> metas) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入表头
        XSSFRow row = sheet.createRow(0);
        XSSFCellStyle headerCellStyle = getHeaderCellStyle(workbook);
        for (int i = 0; i < IMPORT_META_META_HEADER.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellStyle(headerCellStyle);
            cell.setCellValue(IMPORT_META_META_HEADER[i]);
        }
        // 写入数据
        XSSFCellStyle cellStyle = getCellStyle(workbook);
        for (int i = 0; i < metas.size(); i++) {
            XSSFRow dRow = sheet.createRow(i + 1);
            setCell(dRow, 0, cellStyle, metas.get(i).getTableName());
            setCell(dRow, 1, cellStyle, metas.get(i).getColumnName());
            setCell(dRow, 2, cellStyle, metas.get(i).getEvaluation());
            setCell(dRow, 3, cellStyle, metas.get(i).getConstValue());
            setCell(dRow, 4, cellStyle, metas.get(i).getVariableValue());
            setCell(dRow, 5, cellStyle, metas.get(i).getExpression());
            setCell(dRow, 6, cellStyle, metas.get(i).getDictCode());
            setCell(dRow, 7, cellStyle, metas.get(i).getPrimaryValue());
            setCell(dRow, 8, cellStyle, metas.get(i).getRemark());
        }
        // 设置列宽
        setColumnWidth(sheet, 0, IMPORT_META_META_HEADER.length);
    }

    /**
     * 往文档中写入信息，导出功能，元数据
     *
     * @param workbook
     * @param sheetName
     * @param metas
     */
    private void exportMetaSheet(XSSFWorkbook workbook, String sheetName, List<PlaceholderMeta> metas) {
        XSSFSheet sheet = workbook.createSheet(sheetName);
        // 写入表头
        XSSFRow row = sheet.createRow(0);
        XSSFCellStyle headerCellStyle = getHeaderCellStyle(workbook);
        for (int i = 0; i < EXPORT_META_HEADER.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellStyle(headerCellStyle);
            cell.setCellValue(EXPORT_META_HEADER[i]);
        }
        // 写入数据
        XSSFCellStyle cellStyle = getCellStyle(workbook);
        for (int i = 0; i < metas.size(); i++) {
            XSSFRow dRow = sheet.createRow(i + 1);
            setCell(dRow, 0, cellStyle, metas.get(i).getPlaceholder());
            setCell(dRow, 1, cellStyle, metas.get(i).getVar());
            setCell(dRow, 2, cellStyle, metas.get(i).getListVar());
            setCell(dRow, 3, cellStyle, metas.get(i).getConstValue());
            setCell(dRow, 4, cellStyle, metas.get(i).getExpression());
            setCell(dRow, 5, cellStyle, metas.get(i).getValueType());
            setCell(dRow, 6, cellStyle, metas.get(i).getValueComputeMode());
            setCell(dRow, 7, cellStyle, metas.get(i).isIsList());
            setCell(dRow, 8, cellStyle, metas.get(i).isIsMerge());
            setCell(dRow, 9, cellStyle, metas.get(i).isIsUnique());
            setCell(dRow, 10, cellStyle, metas.get(i).isIsImage());
            setCell(dRow, 11, cellStyle, metas.get(i).getImageWidth());
            setCell(dRow, 12, cellStyle, metas.get(i).getImageHeight());
            setCell(dRow, 13, cellStyle, metas.get(i).getDescription());
        }
        // 设置列宽
        setColumnWidth(sheet, 0, EXPORT_META_HEADER.length);
    }

    /**
     * 获取文件路径 upload/存放表/租户编码/应用Id
     *
     * @param meta
     * @param fileName
     * @return
     */
    private String getSavePath(ExportTemplate meta, String fileName, boolean isRename) {
        return UploadService.getSavePath(ROOT_DIRECTORY, AttachmentSourceEnum.PLATFORM_RESOURCES.getValue(), meta.getTenantCode(), meta.getAppId(), fileName, isRename);
    }

    /**
     * 保存文件至附件表中
     *
     * @param meta
     * @param excelPath
     * @param fileName
     * @return
     * @throws IOException
     */
    public Resources saveAttach(ExportTemplate meta, String excelPath, String fileName) throws IOException {
        File excelFile = new File(excelPath);
        BasicFileAttributes attributes = Files.readAttributes(excelFile.toPath(), BasicFileAttributes.class);
        Resources attach = new Resources();
        attach.setName(fileName);
        attach.setType(Files.probeContentType(excelFile.toPath()));
        attach.setSize(attributes.size());
        attach.setPath(excelPath);
        attach.setGenre("fileTemplate");
        attach.setObjectId(meta.getId());
        attach.setAppId(meta.getAppId());

        return resourcesService.createModel(attach);
    }

    /**
     * 将文件转为base64格式
     *
     * @param fileInputStream
     * @param attach
     * @return
     * @throws IOException
     */
    private Map<String, Object> fileToBase64(FileInputStream fileInputStream, Resources attach) throws IOException {
        Map<String, Object> templateMap = new HashMap<>();
        byte[] excelBytes = fileInputStream.readAllBytes();
        String base64Content = Base64.getEncoder().encodeToString(excelBytes);
        templateMap.put("type", attach.getType());
        templateMap.put("size", attach.getSize());
        templateMap.put("name", attach.getName());
        templateMap.put("base64", base64Content);

        return templateMap;
    }

    /**
     * 备份原数据，在更新原数据
     *
     * @param meta
     * @param template
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void backupsAndUpdateExportTemplate(ExportTemplate meta, String template, String templateRule) throws InvocationTargetException, IllegalAccessException {
        if (template == null && templateRule == null) {
            return;
        }
        // 新建
        ExportTemplate newExTe = new ExportTemplate();
        BeanUtils.copyProperties(newExTe, meta);
        newExTe.setId(null);
        newExTe.setDeleteAt(new Date());
        newExTe.setDelStatus(DeleteStatusEnum.IS.getCode());
        newExTe.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
        dao.save(newExTe);
        // 更新
        if (template != null) {
            meta.setTemplate(template);
        }
        if (templateRule != null) {
            meta.setTemplateRule(templateRule);
        }
        this.updateModel(meta);
    }

    /**
     * 表头样式
     *
     * @param workbook
     * @return
     */
    private XSSFCellStyle getHeaderCellStyle(XSSFWorkbook workbook) {
        // 创建字体样式
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 14); // 设置字体大小为14磅
        font.setBold(true); // 设置字体加粗
        // 创建单元格样式，并将字体样式应用到单元格样式中
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN); // 设置上边框
        style.setBorderBottom(BorderStyle.THIN); // 设置下边框
        style.setBorderLeft(BorderStyle.THIN); // 设置左边框
        style.setBorderRight(BorderStyle.THIN); // 设置右边框
        // 创建一个单元格样式，并设置背景色为浅灰色
        byte[] rgb = new byte[]{(byte) 242, (byte) 243, (byte) 245}; // RGB for #C0C0C0 242, 243, 245;
        XSSFColor myColor = new XSSFColor(rgb, null);
        style.setFillForegroundColor(myColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // 数值剧中
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 普通单元格样式
     *
     * @param workbook
     * @return
     */
    private XSSFCellStyle getCellStyle(XSSFWorkbook workbook) {
        // 创建字体样式
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 11); // 设置字体大小为11磅
        // 创建单元格样式，并将字体样式应用到单元格样式中
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN); // 设置上边框
        style.setBorderBottom(BorderStyle.THIN); // 设置下边框
        style.setBorderLeft(BorderStyle.THIN); // 设置左边框
        style.setBorderRight(BorderStyle.THIN); // 设置右边框

        return style;
    }

    /**
     * 设置列宽，根据字符串长度
     *
     * @param sheet
     * @param value
     * @param index
     */
    private void setColumnWidth(XSSFSheet sheet, String value, int index) {
        // 根据文本长度估算列宽（这里是一个简单的估算，可能需要调整）
        int estimatedWidth = (int) (value.length() * 3.5 * 256); // 假设每个字符大约需要1.5个字符宽度的空间
        sheet.setColumnWidth(index, estimatedWidth); // 设置第一列的列宽
    }

    /**
     * 设置列宽，固定列宽
     *
     * @param sheet
     * @param index
     * @param extent
     */
    private void setColumnWidth(XSSFSheet sheet, int index, int extent) {
        int estimatedWidth = (int) (5 * 3.5 * 256); // 假设每个字符大约需要1.5个字符宽度的空间
        // 根据文本长度估算列宽（这里是一个简单的估算，可能需要调整）
        for (int i = index; i < extent; i++) {
            sheet.setColumnWidth(i, estimatedWidth); // 设置第一列的列宽
        }
    }

    /**
     * 设置单元格 备注
     *
     * @param sheet
     * @param cell
     * @param mark
     */
    private void setCellComment(XSSFSheet sheet, XSSFCell cell, String mark) {
        ClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setRow1(cell.getRowIndex());
        anchor.setCol2(cell.getColumnIndex() + 5);
        anchor.setRow2(cell.getRowIndex() + 5);
        Drawing drawing = sheet.createDrawingPatriarch();
        XSSFComment comment = (XSSFComment) drawing.createCellComment(anchor);
        comment.setString(new XSSFRichTextString(mark));
        cell.setCellComment(comment);
    }

    /**
     * 设置单元格，样式，值
     *
     * @param row
     * @param index
     * @param cellStyle
     * @param value
     */
    private void setCell(XSSFRow row, int index, XSSFCellStyle cellStyle, String value) {
        XSSFCell cell = row.createCell(index);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
    }

    /**
     * 设置单元格，样式，值
     *
     * @param row
     * @param index
     * @param cellStyle
     * @param value
     */
    private void setCell(XSSFRow row, int index, XSSFCellStyle cellStyle, Boolean value) {
        XSSFCell cell = row.createCell(index);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
    }

    /**
     * 设置单元格，样式，值
     *
     * @param row
     * @param index
     * @param cellStyle
     * @param value
     */
    private void setCell(XSSFRow row, int index, XSSFCellStyle cellStyle, Double value) {
        XSSFCell cell = row.createCell(index);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value);
    }

    /**
     * 设置单元格，样式，值
     *
     * @param row
     * @param index
     * @param cellStyle
     * @param value
     */
    private void setCell(XSSFRow row, int index, XSSFCellStyle cellStyle, Integer value) {
        XSSFCell cell = row.createCell(index);
        cell.setCellStyle(cellStyle);
        cell.setCellValue(value == null ? null : String.valueOf(value));
    }
}
