package org.geelato.web.platform.m.excel.service;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.xssf.usermodel.*;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.core.enums.DeleteStatusEnum;
import org.geelato.core.enums.EnableStatusEnum;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.BusinessTypeData;
import org.geelato.web.platform.m.excel.entity.ExportTemplate;
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

/**
 * @author diabl
 * @description: TODO
 * @date 2023/8/11 15:05
 */
@Component
public class ExportTemplateService extends BaseService {
    private static final String ROOT_DIRECTORY = "upload";
    private final Logger logger = LoggerFactory.getLogger(ExportTemplateService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    private UploadService uploadService;
    @Autowired
    private AttachService attachService;

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

            } else if ("export".equalsIgnoreCase(exportTemplate.getUseType()) && Strings.isNotBlank(exportTemplate.getBusinessMetaData())) {

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
            List<BusinessTypeData> typeData = JSON.parseArray(meta.getBusinessTypeData(), BusinessTypeData.class);
            if (typeData == null || typeData.isEmpty()) {
                return result.error().setMsg("数据类型不存在，无法生成文件！");
            }
            // 创建文件，
            String excelPath = uploadService.getSavePath(ROOT_DIRECTORY, "excel.xlsx", true);
            File excelFile = new File(excelPath);
            // 读取文件，
            workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet();
            // 创建字体样式
            XSSFFont font = workbook.createFont();
            font.setFontHeightInPoints((short) 14); // 设置字体大小为12磅
            font.setBold(true); // 设置字体加粗
            // 创建单元格样式，并将字体样式应用到单元格样式中
            XSSFCellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(font);
            cellStyle.setBorderTop(BorderStyle.THIN); // 设置上边框
            cellStyle.setBorderBottom(BorderStyle.THIN); // 设置下边框
            cellStyle.setBorderLeft(BorderStyle.THIN); // 设置左边框
            cellStyle.setBorderRight(BorderStyle.THIN); // 设置右边框
            // 写入信息，
            XSSFRow row = sheet.createRow(0);
            for (int i = 0; i < typeData.size(); i++) {
                BusinessTypeData data = typeData.get(i);
                XSSFCell cell = row.createCell(i);
                cell.setCellStyle(cellStyle);
                if (Strings.isNotBlank(data.getName())) {
                    cell.setCellValue(data.getName());
                    // 根据文本长度估算列宽（这里是一个简单的估算，可能需要调整）
                    int estimatedWidth = (int) (data.getName().length() * 3.5 * 256); // 假设每个字符大约需要1.5个字符宽度的空间
                    sheet.setColumnWidth(i, estimatedWidth); // 设置第一列的列宽
                }
                String mark = String.format("类型：%s；\r\n格式：%s；\r\n说明：%s；", data.getType(), data.getFormat(), data.getRemark());
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
            //  冻结第一行，列不冻结
            sheet.createFreezePane(0, 1);
            // 输出数据到文件
            outputStream = new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(outputStream);
            outputStream.flush();
            workbook.close();
            // 保存附件
            BasicFileAttributes attributes = Files.readAttributes(excelFile.toPath(), BasicFileAttributes.class);
            Attach attach = new Attach();
            attach.setName(String.format("%s：导入模板 %s.xlsx", meta.getTitle(), sdf.format(new Date())));
            attach.setType(Files.probeContentType(excelFile.toPath()));
            attach.setSize(attributes.size());
            attach.setPath(excelPath);
            Map<String, Object> attachMap = attachService.createModel(attach);
            result.success().setData(attachMap);
            // 转base64
            fileInputStream = new FileInputStream(excelFile);
            byte[] excelBytes = fileInputStream.readAllBytes();
            String base64Content = Base64.getEncoder().encodeToString(excelBytes);
            Map<String, Object> templateMap = new HashMap<>();
            templateMap.put("type", attach.getType());
            templateMap.put("size", attach.getSize());
            templateMap.put("name", attach.getName());
            templateMap.put("base64", base64Content);
            // 新建
            ExportTemplate newExTe = new ExportTemplate();
            BeanUtils.copyProperties(newExTe, meta);
            newExTe.setId(null);
            newExTe.setDeleteAt(new Date());
            newExTe.setDelStatus(DeleteStatusEnum.IS.getCode());
            newExTe.setEnableStatus(EnableStatusEnum.DISABLED.getCode());
            dao.save(newExTe);
            // 更新
            meta.setTemplate(JSON.toJSONString(templateMap));
            this.updateModel(meta);
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
}
