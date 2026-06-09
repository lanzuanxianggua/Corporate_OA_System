package cn.oa.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissingBusinessSchemaInitializer implements ApplicationRunner {

    private static final String SCRIPT = "db/migration/V992__missing_business_tables.sql";

    private final DataSource dataSource;

    @Value("${oa.schema.init-missing-business-tables:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Missing business schema initializer disabled");
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(false);
        populator.addScript(new ClassPathResource(SCRIPT));
        populator.execute(dataSource);
        log.info("Missing business schema initializer executed: {}", SCRIPT);
    }
}
