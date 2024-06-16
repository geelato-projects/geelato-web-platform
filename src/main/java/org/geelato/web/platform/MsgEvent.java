package org.geelato.web.platform;

import org.springframework.context.ApplicationEvent;

public class MsgEvent extends ApplicationEvent {
    private String msg;

    public MsgEvent(Object source, String msg) {
        super(source);
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "MsgEvent{" +
                "msg='" + msg + '\'' +
                '}';
    }
}