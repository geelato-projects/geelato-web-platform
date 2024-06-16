package org.geelato.web.platform.graal.service;

import com.ibm.icu.impl.duration.impl.Utils;
import org.geelato.core.graal.GraalService;
import org.geelato.utils.NumbChineseUtils;
import org.geelato.web.platform.m.security.service.UserService;

@GraalService(name="fn",built = "true")
public class FnService {

    public String getUser(){
        return null;
    }

    public String toChineseCurrency(String digit){
        return NumbChineseUtils.byOldChinese(digit);
    }

    public String dateText(){
        return null;
    }
}
