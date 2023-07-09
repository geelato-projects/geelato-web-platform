package org.geelato.web.platform.m.base.service;

import org.apache.logging.log4j.util.Strings;
import org.geelato.core.constants.ApiErrorMsg;
import org.geelato.web.platform.m.base.entity.Attach;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author diabl
 * @description: TODO
 * @date 2023/7/5 10:06
 */
@Component
public class AttachService extends BaseService {

    /**
     * 删除实体文件
     *
     * @param model
     * @return
     */
    public boolean deleteFile(Attach model) {
        Assert.notNull(model, ApiErrorMsg.IS_NULL);
        if (Strings.isNotBlank(model.getUrl())) {
            File file = new File(model.getUrl());
            if (file.exists()) {
                return file.delete();
            }
        }

        return true;
    }
}
