package org.geelato.web.platform;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class TestEventListener implements ApplicationListener<MsgEvent> {
    @Override
    public void onApplicationEvent(MsgEvent event) {
        System.out.println("receive msg event: " + event);
    }
}
