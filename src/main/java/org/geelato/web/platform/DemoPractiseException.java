package org.geelato.web.platform;

public class DemoPractiseException  extends PlatformRuntimeException{
    public DemoPractiseException(String msg) {
        super(9999,msg);
    }
    public DemoPractiseException() {
        super(9999,"test_msg");
    }
}
