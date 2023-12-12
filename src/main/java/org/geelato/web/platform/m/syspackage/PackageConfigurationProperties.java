package org.geelato.web.platform.m.syspackage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "geelato.package")
public class PackageConfigurationProperties {
    private String path;
    private String uploadFolder;

    public String getUploadPath(){
        return this.path+"/"+uploadFolder+"/";
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    public String getUploadFolder() {
        return  uploadFolder;
    }

    public void setUploadFolder(String uploadFolder) {
        this.uploadFolder = uploadFolder;
    }
}
