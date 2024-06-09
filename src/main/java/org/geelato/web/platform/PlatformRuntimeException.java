package org.geelato.web.platform;

import org.geelato.core.constants.ApiResultCode;

public  class PlatformRuntimeException extends RuntimeException {

    private int code;

    private String msg;
    public PlatformRuntimeException() {
        super();
    }

    public PlatformRuntimeException(String msg) {
        super(msg);
        this.msg = msg;
        this.code = ApiResultCode.ERROR;
    }

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
