package org.geelato.web.platform.m.excel.rest;

import com.aliyun.dingtalkyida_1_0.models.BatchGetFormDataByIdListResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.geelato.core.api.ApiPagedResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.rest.BaseController;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.ExportTemplate;
import org.geelato.web.platform.m.excel.entity.PlaceholderMeta;
import org.geelato.web.platform.m.excel.service.ExcelWriter;
import org.geelato.web.platform.m.excel.service.ExportTemplateService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/9/2 15:23
 */
@Controller
@RequestMapping(value = "/api/export/file")
public class ExportExcelController extends BaseController {
    private static final String ROOT_DIRECTORY = "upload";
    private final Logger logger = LoggerFactory.getLogger(ExportExcelController.class);
    @Autowired
    private ExportTemplateService exportTemplateService;
    @Autowired
    private ExcelWriter excelWriter;
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

    @RequestMapping(value = "/excel", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public void exportBill(HttpServletRequest req, HttpServletResponse response, @PathVariable String fileName, @PathVariable String templateId) {
        try {
            // 表单数据
            String gql = getGql(request);
            List<BatchGetFormDataByIdListResponseBody.BatchGetFormDataByIdListResponseBodyResult> resultList = null;
            // 模型
            ExportTemplate exportTemplate = exportTemplateService.getModel(ExportTemplate.class, templateId);
            Assert.notNull(exportTemplate, "object can't be null");

            // 模板文件
            File templateFilePath = getFile(exportTemplate.getTemplate());

            FileInputStream templateFileInputStream = new FileInputStream(templateFilePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(templateFileInputStream);
            POIFSFileSystem fileSystem = new POIFSFileSystem(bufferedInputStream);
            HSSFWorkbook templateWorkbook = new HSSFWorkbook(fileSystem);

            // 如果多组数据写在一个Sheet中
            writeSheet(templateWorkbook, resultList);

            // 生成文件
            String directory = uploadService.getSavePath(ROOT_DIRECTORY, fileName, true);
            File exportFile = new File(directory);
            OutputStream outputStream = new FileOutputStream(exportFile);
            templateWorkbook.removeSheetAt(0);
            templateWorkbook.write(outputStream);
            outputStream.close();

            bufferedInputStream.close();

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
            logger.error("基于Yida的表单信息导出Excel出错。", e);
        }
    }

    /**
     * 多组数据写在一个Sheet中，对于指定需要迭代的行，将多组数据的拼接形成多行列表
     *
     * @param templateWorkbook
     * @param resultList       多组数据
     */
    private void writeSheet(HSSFWorkbook templateWorkbook, List<BatchGetFormDataByIdListResponseBody.BatchGetFormDataByIdListResponseBodyResult> resultList) {
        HSSFSheet placeholderMetaSheet = templateWorkbook.getSheetAt(0);
        Map<String, PlaceholderMeta> placeholderMetaMap = excelWriter.readPlaceholderMeta(placeholderMetaSheet);
        HSSFSheet sheet = templateWorkbook.getSheetAt(1);

        List<Map> valueMapList = new ArrayList<>();
        // 取多项数据中的其中一项作为非列表单元格的变量数据（因为多项数据中非列表数据的值应是一致的，这里只取第一项数据）
        Map valueMap = new HashMap();

        excelWriter.writeSheet(sheet, placeholderMetaMap, valueMapList, valueMap);
    }

    /**
     * 获取文件
     *
     * @param attachId
     * @return
     */
    private File getFile(String attachId) {
        if (Strings.isNotBlank(attachId)) {
            Attach attach = attachService.getModel(Attach.class, attachId);
            Assert.notNull(attach, "object can't be null");
            File file = new File(attach.getUrl());
            if (file.exists()) {
                return file;
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
