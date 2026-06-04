package cn.oa.platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台层通用配置.
 *
 * <p>本类只声明属性绑定，由 IdGeneratorConfig 通过 @EnableConfigurationProperties 显式启用.
 * 不要加 @Configuration 否则与 @ComponentScan 产生重复注册.
 */
@ConfigurationProperties(prefix = "oa.common")
public class CommonProperties {

    private IdGenerator idGenerator = new IdGenerator();

    public IdGenerator getIdGenerator() { return idGenerator; }
    public void setIdGenerator(IdGenerator idGenerator) { this.idGenerator = idGenerator; }

    public static class IdGenerator {
        private long workerId = 1L;
        private long datacenterId = 1L;

        public long getWorkerId() { return workerId; }
        public void setWorkerId(long workerId) { this.workerId = workerId; }
        public long getDatacenterId() { return datacenterId; }
        public void setDatacenterId(long datacenterId) { this.datacenterId = datacenterId; }
    }
}
