package org.geelato.web.platform.m.base.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.util.Strings;
import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/4 10:46
 */
@Controller
@RequestMapping(value = "/api/upload")
public class UploadController extends BaseController {
    private static final String ROOT_DIRECTORY = "upload";
    private static final String ROOT_CONFIG_DIRECTORY = "/upload/config";
    private final Logger logger = LoggerFactory.getLogger(UploadController.class);
    @Autowired
    private UploadService uploadService;
    @Autowired
    private AttachService attachService;

    @RequestMapping(value = "/file", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult uploadFile(@RequestParam("file") MultipartFile file, Boolean isRename, HttpServletRequest request) {
        ApiResult result = new ApiResult();
        if (file == null || file.isEmpty()) {
            return result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }
        try {
            Attach attach = new Attach(file);
            attach.setUrl(uploadService.getSavePath(ROOT_DIRECTORY, attach.getName(), true));
            byte[] bytes = file.getBytes();
            Files.write(Paths.get(attach.getUrl()), bytes);
            result.success().setData(attachService.createModel(attach));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

    @RequestMapping(value = "/object", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult uploadObject(@RequestBody Map<String, Object> params, String fileName) throws IOException {
        ApiResult result = new ApiResult();
        if (params == null || params.isEmpty() || Strings.isBlank(fileName)) {
            return result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }
        FileOutputStream fops = null;
        ObjectOutputStream oops = null;
        try {
            String ext = uploadService.getFileExtension(fileName);
            if (Strings.isBlank(ext) || !ext.equalsIgnoreCase(".config")) {
                fileName += ".config";
            }
            uploadService.fileMkdirs(ROOT_CONFIG_DIRECTORY);
            File file = new File(String.format("%s/%s", ROOT_CONFIG_DIRECTORY, fileName));
            if (file.exists() && !uploadService.fileResetName(file)) {
                file.delete();
            }
            fops = new FileOutputStream(file);
            oops = new ObjectOutputStream(fops);
            oops.writeObject(params);
            result.setData(file.getName());
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        } finally {
            if (oops != null) {
                oops.close();
            }
            if (fops != null) {
                fops.close();
            }
        }

        return result;
    }

    @RequestMapping(value = "/json", method = RequestMethod.POST)
    @ResponseBody
    public ApiResult uploadJson(@RequestBody String JsonData, String fileName) throws IOException {
        ApiResult result = new ApiResult();
        if (Strings.isBlank(JsonData) || Strings.isBlank(fileName)) {
            return result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            String ext = uploadService.getFileExtension(fileName);
            if (Strings.isBlank(ext) || !ext.equalsIgnoreCase(".config")) {
                fileName += ".config";
            }
            uploadService.fileMkdirs(ROOT_CONFIG_DIRECTORY);
            File file = new File(String.format("%s/%s", ROOT_CONFIG_DIRECTORY, fileName));
            if (file.exists() && !uploadService.fileResetName(file)) {
                file.delete();
            }
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(JsonData);
            result.setData(file.getName());
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (fileWriter != null) {
                fileWriter.close();
            }
        }

        return result;
    }

}

