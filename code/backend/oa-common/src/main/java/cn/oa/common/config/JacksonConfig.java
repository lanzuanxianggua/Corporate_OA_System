package cn.oa.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                ObjectMapper mapper = jacksonConverter.getObjectMapper();
                // Long → String 防止 JS 精度丢失
                SimpleModule longModule = new SimpleModule();
                longModule.addSerializer(Long.class, ToStringSerializer.instance);
                longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
                mapper.registerModule(longModule);
                // 注册自定义的 JavaTimeModule
                JavaTimeModule module = new JavaTimeModule();
                module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(OUTPUT_FORMAT));
                module.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
                mapper.registerModule(module);
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }
        }
    }
}
