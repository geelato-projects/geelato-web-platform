package org.geelato.web.platform.boot;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DynamicDataSource extends AbstractRoutingDataSource {
    public static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();


    public static String getCustomerType() {
        return contextHolder.get();
    }

    public static void clearCustomerType() {
        contextHolder.remove();
    }
    @Override
    protected Object determineCurrentLookupKey() {
        return getCustomerType();
    }
}
