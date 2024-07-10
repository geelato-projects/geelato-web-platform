package org.geelato.web.platform.m.syspackage.service;

import org.geelato.web.platform.m.base.service.BaseService;
import org.geelato.web.platform.m.syspackage.entity.AppVersion;
import org.springframework.stereotype.Component;

@Component
public class AppVersionService extends BaseService {

    public AppVersion getAppVersionByVersion(String version) {
        return dao.queryForObject(AppVersion.class, "version", version);
    }
}
