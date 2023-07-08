package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/5 14:01
 */
@Component
public class DownloadService {


    public File downloadFile(String name, String path) {
        if (Strings.isBlank(path)) {
            return null;
        }

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        return file;
    }

}
