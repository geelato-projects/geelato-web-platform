package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.utils.UIDGenerator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/4 10:48
 */
@Component
public class UploadService {

    /**
     * 返回文件上传绝对路径
     *
     * @param subPath
     * @param fileName
     * @param isRename
     * @return
     */
    public String getSavePath(String subPath, String fileName, boolean isRename) {
        // 处理子路径
        if (Strings.isNotBlank(subPath)) {
            subPath += "/";
        } else {
            subPath = "";
        }

        // 处理日期路径
        Date date = new Date();
        SimpleDateFormat yyyyFt = new SimpleDateFormat("yyyy");
        SimpleDateFormat MMFt = new SimpleDateFormat("MM");
        SimpleDateFormat ddFt = new SimpleDateFormat("dd");
        SimpleDateFormat HHFt = new SimpleDateFormat("HH");
        String datePath = String.format("%s/%s/%s/%s/", yyyyFt.format(date), MMFt.format(date), ddFt.format(date), HHFt.format(date));

        // 处理文件名称
        if (isRename) {
            int dotIndex = fileName.lastIndexOf(".");
            String ext = fileName.substring(dotIndex);
            fileName = UIDGenerator.generate(1) + ext;
        }
        // 路径检验
        File pathFile = new File("/" + subPath + datePath);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        }

        return "/" + subPath + datePath + fileName;
    }
}
