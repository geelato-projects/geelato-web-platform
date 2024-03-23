package org.geelato.web.platform.boot;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class DynamicDatasourceHolder {
    public static final ThreadLocal<String> contextHolder = new ThreadLocal<String>();
    public static String getDataSource() {
        return contextHolder.get();
    }
    public static void setDataSource(String dataSourceKey) {
        contextHolder.set(dataSourceKey);
    }
    public static void removeDataSource(String dataSourceKey) {
        contextHolder.remove();
    }
}
