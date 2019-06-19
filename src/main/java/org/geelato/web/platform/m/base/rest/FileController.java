package org.geelato.web.platform.m.base.rest;


import org.geelato.core.api.ApiMetaResult;
import org.geelato.core.api.ApiResultCode;
import org.geelato.core.orm.Dao;
import org.geelato.web.platform.m.base.service.RuleService;
import org.geelato.web.platform.m.base.entity.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * 文件上传下载管理
 * @author itechgee@126.com
 */
@Controller
@RequestMapping(value = "/api/file/")
public class FileController implements InitializingBean {

    @Autowired
    @Qualifier("primaryDao")
    protected Dao dao;

    @Autowired
    protected RuleService ruleService;


    private static Logger logger = LoggerFactory.getLogger(FileController.class);

    /**
     * 处理文件上传
     * e.g.:http://localhost:8080/api/file/upload/
     *
     * @param file
     * @param request
     * @return 上传的字节数，-1表示上传失败
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public @ResponseBody
    ApiMetaResult uploadFile(@RequestParam("file") MultipartFile file,
                             HttpServletRequest request) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        /*System.out.println("fileName-->" + fileName);
        System.out.println("getContentType-->" + contentType);*/
        String filePath = request.getSession().getServletContext().getRealPath("upload/");
        ApiMetaResult apiMetaResult = new ApiMetaResult();
        try {
            int size = file.getBytes().length;
            saveToFileSystem(file.getBytes(), filePath, fileName);
            // TODO 事务
            apiMetaResult.setData(saveToDb(size, filePath, fileName));
            apiMetaResult.setCode(ApiResultCode.SUCCESS);
            apiMetaResult.setMsg("上传文件成功！");
        } catch (IOException e) {
            apiMetaResult.setCode(ApiResultCode.FAIL);
            apiMetaResult.setMsg("上传文件失败！");
            logger.error("上传文件失败！", e);
        }
        return apiMetaResult;
    }

    /**
     * @param file
     * @param filePath
     * @param fileName
     * @throws Exception
     */
    private void saveToFileSystem(byte[] file, String filePath, String fileName) throws IOException {
        File targetFile = new File(filePath);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(filePath + fileName);
        out.write(file);
        out.flush();
        out.close();
    }

    private Map saveToDb(int size, String filePath, String fileName) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(fileName);
        fileInfo.setSize(size);
        fileInfo.setFileType(filePath.substring(filePath.lastIndexOf(".")));
        return dao.save(fileInfo);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ruleService.setDao(dao);
    }
}
