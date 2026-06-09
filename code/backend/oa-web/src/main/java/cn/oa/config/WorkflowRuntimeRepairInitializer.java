package cn.oa.config;

import cn.oa.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class WorkflowRuntimeRepairInitializer implements ApplicationRunner {

    private final WorkflowService workflowService;

    @Value("${oa.workflow.repair-missing-pending-tasks:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Workflow missing pending task repair disabled");
            return;
        }
        int repaired = workflowService.repairMissingPendingTasks();
        log.info("Workflow missing pending task repair completed: repaired={}", repaired);
    }
}
