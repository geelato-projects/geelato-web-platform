package org.geelato.web.platform.m.base.rest;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.DownloadService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.geelato.web.platform.m.excel.entity.OfficeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/5 14:00
 */
@Controller
@RequestMapping(value = "/api/resources")
public class DownloadController extends BaseController {
    private static final String ROOT_DIRECTORY = "upload";
    private final Logger logger = LoggerFactory.getLogger(DownloadController.class);
    @Autowired
    private DownloadService downloadService;
    @Autowired
    private UploadService uploadService;
    @Autowired
    private AttachService attachService;

    @RequestMapping(value = "/file", method = RequestMethod.GET)
    @ResponseBody
    public void downloadFile(String id, String name, String path, boolean isPdf, HttpServletRequest request, HttpServletResponse response) throws Exception {
        OutputStream out = null;
        FileInputStream in = null;
        try {
            File file = null;
            if (Strings.isNotBlank(id)) {
                Attach attach = attachService.getModel(Attach.class, id);
                file = downloadService.downloadFile(attach.getName(), attach.getUrl());
                name = attach.getName();
            } else if (Strings.isNotBlank(path)) {
                file = downloadService.downloadFile(name, path);
                name = Strings.isNotBlank(name) ? name : file.getName();
            }
            if (isPdf) {
                String ext = name.substring(name.lastIndexOf("."));
                name = Strings.isNotBlank(name) ? name.replace(ext, ".pdf") : null;
                String outputPath = uploadService.getSavePath(ROOT_DIRECTORY, "word-to-pdf.pdf", true);
                OfficeUtils.wordToPdf(file.getAbsolutePath(), outputPath, ext);
                File pFile = new File(outputPath);
                file = pFile.exists() ? pFile : null;
            }
            if (file != null && Strings.isNotBlank(name)) {
                out = response.getOutputStream();
                in = new FileInputStream(file);
                name = URLEncoder.encode(name, "UTF-8");
                String mineType = request.getServletContext().getMimeType(name);
                response.setContentType(mineType);
                response.setHeader("Content-disposition", "attachment; filename=" + name);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            } else {
                throw new Exception("文件不存在");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}
