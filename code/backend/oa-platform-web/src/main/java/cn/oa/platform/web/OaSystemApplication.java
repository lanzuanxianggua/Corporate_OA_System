package cn.oa.platform.web;

import cn.oa.platform.common.config.JacksonConfig;
import cn.oa.platform.common.config.MybatisPlusConfig;
import cn.oa.platform.common.id.IdGeneratorConfig;
import cn.oa.platform.security.config.SecurityAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Corporate OA System v2 启动类.
 *
 * <p>v2 设计文档：<a href="https://example.com/docs/v2/01-architecture.md">docs/v2/01-architecture.md</a>
 *
 * @author Hermes
 */
@SpringBootApplication(scanBasePackages = "cn.oa")
@MapperScan({
        "cn.oa.system.mapper",
        "cn.oa.platform.common.mapper",
        "cn.oa.workflow.mapper",
        "cn.oa.hr.leave.mapper",
        "cn.oa.hr.employee.mapper",
        "cn.oa.hr.mapper",
        "cn.oa.document.mapper",
        "cn.oa.admin.mapper",
        "cn.oa.finance.mapper",
	"cn.oa.meeting.mapper",
	"cn.oa.message.mapper"
})
@Import({
        MybatisPlusConfig.class,
        JacksonConfig.class,
        IdGeneratorConfig.class,
        SecurityAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class OaSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaSystemApplication.class, args);
    }
}
