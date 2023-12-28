package org.geelato.web.platform;

public  class PlatformRuntimeException extends RuntimeException {

    private int code;

    private String msg;

    public PlatformRuntimeException(int code,String msg){
        super();
        this.code=code;
        this.msg=msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
