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
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

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
        SimpleDateFormat mmFt = new SimpleDateFormat("mm");
        String datePath = String.format("%s/%s/%s/%s/%s/", yyyyFt.format(date), MMFt.format(date), ddFt.format(date), HHFt.format(date), mmFt.format(date));

        // 处理文件名称
        if (isRename) {
            String ext = this.getFileExtension(fileName);
            fileName = UIDGenerator.generate() + ext;
        }
        // 路径检验
        this.fileMkdirs("/" + subPath + datePath);

        return "/" + subPath + datePath + fileName;
    }

    /**
     * 创建全部路径
     *
     * @param path
     */
    public void fileMkdirs(String path) {
        if (Strings.isNotBlank(path)) {
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
        }
    }

    /**
     * 文件后缀。例：.xlsx
     *
     * @param fileName 文件名称
     * @return
     */
    public String getFileExtension(String fileName) {
        if (Strings.isNotBlank(fileName)) {
            int lastIndexOfDot = fileName.lastIndexOf('.');
            if (lastIndexOfDot != -1) {
                return fileName.substring(lastIndexOfDot);
            }
        }

        return "";
    }

    public String getFileName(String fileName) {
        if (Strings.isNotBlank(fileName)) {
            int lastIndexOfDot = fileName.lastIndexOf('.');
            if (lastIndexOfDot != -1) {
                return fileName.substring(0, lastIndexOfDot);
            }
        }

        return "";
    }

    /**
     * 文件复制
     *
     * @param file
     * @param fileName 重命名文件名称
     * @return
     */
    public boolean fileResetName(File file, String fileName) {
        if (file != null && file.exists()) {
            if (Strings.isBlank(fileName)) {
                fileName = String.format("%s_bak_%s%s", this.getFileName(file.getName()), sdf.format(new Date()), this.getFileExtension(file.getName()));
            }
            File newFile = new File(String.format("%s/%s", file.getParent(), fileName));
            if (!newFile.exists()) {
                return file.renameTo(newFile);
            }
        }

        return false;
    }

    /**
     * 文件复制，重命名：name_uuid_bak.extension
     *
     * @param file
     * @return
     */
    public boolean fileResetName(File file) {
        return this.fileResetName(file, null);
    }
}
