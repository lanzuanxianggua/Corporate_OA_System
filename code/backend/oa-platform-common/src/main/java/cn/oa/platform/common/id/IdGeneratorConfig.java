package cn.oa.platform.common.id;

import cn.oa.platform.common.config.CommonProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ID 生成器配置.
 */
@Configuration
@EnableConfigurationProperties(CommonProperties.class)
public class IdGeneratorConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(CommonProperties props) {
        return new SnowflakeIdGenerator(
                props.getIdGenerator().getWorkerId(),
                props.getIdGenerator().getDatacenterId());
    }
}
