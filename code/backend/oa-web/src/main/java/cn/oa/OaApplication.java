package cn.oa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@MapperScan({
    "cn.oa.workflow.mapper",
    "cn.oa.document.mapper",
    "cn.oa.knowledge.mapper",
    "cn.oa.meeting.mapper",
    "cn.oa.task.mapper",
    "cn.oa.hr.mapper",
    "cn.oa.admin.mapper",
    "cn.oa.finance.mapper",
    "cn.oa.message.mapper"
})
public class OaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaApplication.class, args);
    }
}