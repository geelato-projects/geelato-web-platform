package org.geelato.web.platform.m.base.rest;

import org.geelato.core.api.ApiResult;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.geelato.web.platform.m.base.service.AttachService;
import org.geelato.web.platform.m.base.service.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/4 10:46
 */
@Controller
@RequestMapping(value = "/api/upload")
public class UploadController extends BaseController {
    private static final String ROOT_DIRECTORY = "upload";
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
            attach.setUrl(uploadService.getSavePath(ROOT_DIRECTORY, attach.getName(), isRename));
            byte[] bytes = file.getBytes();
            Files.write(Paths.get(attach.getUrl()), bytes);
            result.success().setData(attachService.createModel(attach));
        } catch (Exception e) {
            logger.error(e.getMessage());
            result.error().setMsg(ApiErrorMsg.OPERATE_FAIL);
        }

        return result;
    }

}
