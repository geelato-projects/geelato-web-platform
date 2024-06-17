package org.geelato.web.platform.graal.service;

import com.ibm.icu.impl.duration.impl.Utils;
import org.geelato.core.graal.GraalService;
import org.geelato.core.util.StringUtils;
import org.geelato.utils.NumbChineseUtils;
import org.geelato.web.platform.m.security.service.UserService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@GraalService(name="fn",built = "true")
public class FnService {

    public String getUser(){
        return null;
    }

    public String toChineseCurrency(String digit){
        return NumbChineseUtils.byOldChinese(digit);
    }

    public String dateText(String targetFormat,String dateStr) throws ParseException {
        String formatDate=null;
        Date date=null ;
        SimpleDateFormat targetDateFormat=new SimpleDateFormat(targetFormat);
        if(StringUtils.isEmpty(dateStr)){
            date=new Date();
            formatDate=targetDateFormat.format(date);
        }else{
            date = targetDateFormat.parse(dateStr);
            formatDate=targetDateFormat.format(date);
        }
        return formatDate;
    }
}
