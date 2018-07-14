package org.geelato.web.platform.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author hongxq
 */
@Configuration
public class SerializerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SerializerConfiguration.class);

    /**
     * 防止json时出现错误FAIL_ON_EMPTY_BEANS
     *
     * @return
     */
    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        ObjectMapper myObjectMapper = new ObjectMapper();
        myObjectMapper.registerModule(simpleModule);
        myObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return myObjectMapper;
    }

}