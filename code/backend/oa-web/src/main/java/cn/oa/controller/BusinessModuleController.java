package cn.oa.controller;

import cn.oa.common.annotation.RequirePermission;
import cn.oa.common.exception.BusinessException;
import cn.oa.common.result.R;
import cn.oa.service.BusinessModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BusinessModuleController {

    private final BusinessModuleService service;

    @GetMapping("/api/admin/supplies/categories")
    @RequirePermission("admin:supply:list")
    public R<List<Map<String, Object>>> supplyCategories(@RequestParam Map<String, Object> params) {
        return R.ok(service.list("supplyCategory", params));
    }

    @PostMapping("/api/admin/supplies/categories")
    @RequirePermission("admin:supply:create")
    public R<Long> createSupplyCategory(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("supplyCategory", body, Map.of("status", "ACTIVE")));
    }

    @PutMapping("/api/admin/supplies/categories/{id}")
    @RequirePermission("admin:supply:update")
    public R<Void> updateSupplyCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.update("supplyCategory", id, body);
        return R.ok();
    }

    @GetMapping("/api/admin/supplies")
    @RequirePermission("admin:supply:list")
    public R<Map<String, Object>> supplies(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("supply", params));
    }

    @PostMapping("/api/admin/supplies")
    @RequirePermission("admin:supply:create")
    public R<Long> createSupply(@RequestBody Map<String, Object> body) {
        body = withCode(body, "supplyCode", "SUP");
        Long id = service.create("supply", body, Map.of("status", "ACTIVE", "safetyStock", 0));
        service.create("supplyStock", Map.of("supplyId", id, "quantity", 0, "lockedQuantity", 0), Map.of());
        return R.ok(id);
    }

    @PutMapping("/api/admin/supplies/{id}")
    @RequirePermission("admin:supply:update")
    public R<Void> updateSupply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.update("supply", id, body);
        return R.ok();
    }

    @DeleteMapping("/api/admin/supplies/{id}")
    @RequirePermission("admin:supply:delete")
    public R<Void> deleteSupply(@PathVariable Long id) {
        service.delete("supply", id);
        return R.ok();
    }

    @GetMapping("/api/admin/supplies/{id}")
    @RequirePermission("admin:supply:list")
    public R<Map<String, Object>> supply(@PathVariable Long id) {
        return R.ok(service.get("supply", id));
    }

    @GetMapping("/api/admin/supplies/{id}/stock")
    @RequirePermission("admin:supply:list")
    public R<Map<String, Object>> supplyStock(@PathVariable Long id) {
        Map<String, Object> stock = service.first("supplyStock", Map.of("supplyId", id));
        if (stock == null) {
            Long stockId = service.create("supplyStock", Map.of("supplyId", id, "quantity", 0, "lockedQuantity", 0), Map.of());
            stock = service.get("supplyStock", stockId);
        }
        return R.ok(stock);
    }

    @PostMapping("/api/admin/supplies/{id}/stock-adjustments")
    @RequirePermission("admin:supply:stock")
    public R<Void> adjustSupplyStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 0)));
        service.adjustSupplyStock(id, quantity, (String) body.get("location"));
        return R.ok();
    }

    @GetMapping("/api/admin/supplies/requests")
    @RequirePermission("admin:supply:request")
    public R<Map<String, Object>> supplyRequests(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("supplyRequest", params));
    }

    @PostMapping("/api/admin/supplies/requests")
    @RequirePermission("admin:supply:request")
    public R<Long> createSupplyRequest(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = (Map<String, Object>) body.getOrDefault("request", body);
        request = withCode(request, "requestNo", "SR");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
        Long id = service.create("supplyRequest", request, Map.of("status", "PENDING", "requestType", "OUT"));
        for (Map<String, Object> item : items) {
            service.create("supplyRequestItem", item, Map.of("requestId", id));
        }
        return R.ok(id);
    }

    @GetMapping("/api/admin/supplies/requests/{id}/items")
    @RequirePermission("admin:supply:request")
    public R<List<Map<String, Object>>> supplyRequestItems(@PathVariable Long id) {
        return R.ok(service.list("supplyRequestItem", Map.of("requestId", id)));
    }

    @PostMapping("/api/admin/supplies/requests/{id}/approve")
    @RequirePermission("admin:supply:request")
    public R<Void> approveSupplyRequest(@PathVariable Long id) {
        service.approveSupplyRequest(id);
        return R.ok();
    }

    @PostMapping("/api/admin/supplies/requests/{id}/reject")
    @RequirePermission("admin:supply:request")
    public R<Void> rejectSupplyRequest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateFields("supplyRequest", id, Map.of("status", "REJECTED", "rejectReason", stringOrDefault(body.get("reason"), "")));
        return R.ok();
    }

    @GetMapping("/api/hr/employees/{empId}/contracts")
    @RequirePermission("hr:employee:list")
    public R<Map<String, Object>> employeeContracts(@PathVariable Long empId, @RequestParam Map<String, Object> params) {
        params.put("empId", empId);
        return R.ok(service.page("employeeContract", params));
    }

    @PostMapping("/api/hr/employees/{empId}/contracts")
    @RequirePermission("hr:employee:update")
    public R<Long> createEmployeeContract(@PathVariable Long empId, @RequestBody Map<String, Object> body) {
        return R.ok(service.create("employeeContract", body, Map.of("empId", empId, "status", "ACTIVE")));
    }

    @GetMapping("/api/hr/employees/{empId}/changes")
    @RequirePermission("hr:employee:list")
    public R<Map<String, Object>> employeeChanges(@PathVariable Long empId, @RequestParam Map<String, Object> params) {
        params.put("empId", empId);
        return R.ok(service.page("employeeChange", params));
    }

    @PostMapping("/api/hr/employees/{empId}/changes")
    @RequirePermission("hr:employee:update")
    public R<Long> createEmployeeChange(@PathVariable Long empId, @RequestBody Map<String, Object> body) {
        return R.ok(service.create("employeeChange", body, Map.of("empId", empId, "status", "EFFECTIVE")));
    }

    @GetMapping("/api/hr/employees/{empId}/certificates")
    @RequirePermission("hr:employee:list")
    public R<Map<String, Object>> employeeCertificates(@PathVariable Long empId, @RequestParam Map<String, Object> params) {
        params.put("empId", empId);
        return R.ok(service.page("employeeCertificate", params));
    }

    @PostMapping("/api/hr/employees/{empId}/certificates")
    @RequirePermission("hr:employee:update")
    public R<Long> createEmployeeCertificate(@PathVariable Long empId, @RequestBody Map<String, Object> body) {
        return R.ok(service.create("employeeCertificate", body, Map.of("empId", empId, "status", "VALID")));
    }

    @GetMapping("/api/hr/employees/{empId}/educations")
    @RequirePermission("hr:employee:list")
    public R<Map<String, Object>> employeeEducations(@PathVariable Long empId, @RequestParam Map<String, Object> params) {
        params.put("empId", empId);
        return R.ok(service.page("employeeEducation", params));
    }

    @PostMapping("/api/hr/employees/{empId}/educations")
    @RequirePermission("hr:employee:update")
    public R<Long> createEmployeeEducation(@PathVariable Long empId, @RequestBody Map<String, Object> body) {
        return R.ok(service.create("employeeEducation", body, Map.of("empId", empId)));
    }

    @GetMapping("/api/finance/contracts")
    @RequirePermission("finance:contract:list")
    public R<Map<String, Object>> financeContracts(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("financeContract", params));
    }

    @PostMapping("/api/finance/contracts")
    @RequirePermission("finance:contract:list")
    public R<Long> createFinanceContract(@RequestBody Map<String, Object> body) {
        body = withCode(body, "contractNo", "FC");
        return R.ok(service.create("financeContract", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/finance/contracts/{id}/activate")
    @RequirePermission("finance:contract:activate")
    public R<Void> activateFinanceContract(@PathVariable Long id) {
        service.updateStatus("financeContract", id, "ACTIVE");
        return R.ok();
    }

    @PostMapping("/api/finance/contracts/{id}/close")
    @RequirePermission("finance:contract:close")
    public R<Void> closeFinanceContract(@PathVariable Long id) {
        service.updateStatus("financeContract", id, "CLOSED");
        return R.ok();
    }

    @GetMapping("/api/finance/payments")
    @RequirePermission("finance:payment:list")
    public R<Map<String, Object>> financePayments(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("financePayment", params));
    }

    @PostMapping("/api/finance/payments")
    @RequirePermission("finance:payment:list")
    public R<Long> createFinancePayment(@RequestBody Map<String, Object> body) {
        body = withCode(body, "paymentNo", "FP");
        return R.ok(service.create("financePayment", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/finance/payments/{id}/submit")
    @RequirePermission("finance:payment:submit")
    public R<Void> submitFinancePayment(@PathVariable Long id) {
        service.updateStatus("financePayment", id, "SUBMITTED");
        return R.ok();
    }

    @PostMapping("/api/finance/payments/{id}/paid")
    @RequirePermission("finance:payment:approve")
    public R<Void> markFinancePaymentPaid(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateFields("financePayment", id, Map.of(
                "status", "PAID",
                "payMethod", stringOrDefault(body.get("payMethod"), ""),
                "paidTime", LocalDateTime.now()));
        return R.ok();
    }

    @GetMapping("/api/hr-performance/goals")
    @RequirePermission("hr:performance:list")
    public R<Map<String, Object>> performanceGoals(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("performanceGoal", params));
    }

    @PostMapping("/api/hr-performance/goals")
    @RequirePermission("hr:performance:update")
    public R<Long> createPerformanceGoal(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("performanceGoal", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/hr-performance/goals/{id}/submit")
    @RequirePermission("hr:performance:update")
    public R<Void> submitPerformanceGoal(@PathVariable Long id) {
        service.updateStatus("performanceGoal", id, "SUBMITTED");
        return R.ok();
    }

    @GetMapping("/api/hr-performance/evals")
    @RequirePermission("hr:performance:list")
    public R<Map<String, Object>> performanceEvals(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("performanceEval", params));
    }

    @PostMapping("/api/hr-performance/evals")
    @RequirePermission("hr:performance:update")
    public R<Long> createPerformanceEval(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("performanceEval", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/hr-performance/evals/{id}/submit")
    @RequirePermission("hr:performance:update")
    public R<Void> submitPerformanceEval(@PathVariable Long id) {
        service.updateStatus("performanceEval", id, "SUBMITTED");
        return R.ok();
    }

    @GetMapping("/api/hr-performance/results")
    @RequirePermission("hr:performance:list")
    public R<Map<String, Object>> performanceResults(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("performanceResult", params));
    }

    @PostMapping("/api/hr-performance/results/generate")
    @RequirePermission("hr:performance:update")
    public R<Void> generatePerformanceResults(@RequestParam Long cycleId) {
        service.generatePerformanceResults(cycleId);
        return R.ok();
    }

    @GetMapping("/api/hr-recruitment/jobs")
    @RequirePermission("hr:recruitment:list")
    public R<Map<String, Object>> recruitJobs(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("recruitJob", params));
    }

    @PostMapping("/api/hr-recruitment/jobs")
    @RequirePermission("hr:recruitment:update")
    public R<Long> createRecruitJob(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("recruitJob", body, Map.of("status", "OPEN")));
    }

    @GetMapping("/api/hr-recruitment/candidates")
    @RequirePermission("hr:recruitment:list")
    public R<Map<String, Object>> recruitCandidates(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("recruitCandidate", params));
    }

    @PostMapping("/api/hr-recruitment/candidates")
    @RequirePermission("hr:recruitment:update")
    public R<Long> createRecruitCandidate(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("recruitCandidate", body, Map.of("status", "NEW")));
    }

    @GetMapping("/api/hr-recruitment/interviews")
    @RequirePermission("hr:recruitment:list")
    public R<Map<String, Object>> recruitInterviews(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("recruitInterview", params));
    }

    @PostMapping("/api/hr-recruitment/interviews")
    @RequirePermission("hr:recruitment:update")
    public R<Long> createRecruitInterview(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("recruitInterview", body, Map.of("status", "SCHEDULED")));
    }

    @PutMapping("/api/hr-recruitment/interviews/{id}")
    @RequirePermission("hr:recruitment:update")
    public R<Void> updateRecruitInterview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.update("recruitInterview", id, body);
        return R.ok();
    }

    @GetMapping("/api/hr-recruitment/offers")
    @RequirePermission("hr:recruitment:list")
    public R<Map<String, Object>> recruitOffers(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("recruitOffer", params));
    }

    @PostMapping("/api/hr-recruitment/offers")
    @RequirePermission("hr:recruitment:update")
    public R<Long> createRecruitOffer(@RequestBody Map<String, Object> body) {
        body = withCode(body, "offerNo", "OF");
        return R.ok(service.create("recruitOffer", body, Map.of("status", "SENT")));
    }

    @PostMapping("/api/hr-recruitment/offers/{id}/accept")
    @RequirePermission("hr:recruitment:update")
    public R<Void> acceptRecruitOffer(@PathVariable Long id) {
        service.updateFields("recruitOffer", id, Map.of("status", "ACCEPTED", "acceptTime", LocalDateTime.now()));
        return R.ok();
    }

    @PostMapping("/api/hr-recruitment/offers/{id}/onboard")
    @RequirePermission("hr:recruitment:update")
    public R<Void> onboardRecruitOffer(@PathVariable Long id) {
        service.updateFields("recruitOffer", id, Map.of("status", "ONBOARDED", "onboardTime", LocalDateTime.now()));
        return R.ok();
    }

    @GetMapping("/api/hr-training/courses")
    @RequirePermission("hr:training:list")
    public R<Map<String, Object>> trainingCourses(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("trainingCourse", params));
    }

    @PostMapping("/api/hr-training/courses")
    @RequirePermission("hr:training:update")
    public R<Long> createTrainingCourse(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("trainingCourse", body, Map.of("status", "ACTIVE")));
    }

    @GetMapping("/api/hr-training/plans")
    @RequirePermission("hr:training:list")
    public R<Map<String, Object>> trainingPlans(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("trainingPlan", params));
    }

    @PostMapping("/api/hr-training/plans")
    @RequirePermission("hr:training:update")
    public R<Long> createTrainingPlan(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("trainingPlan", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/hr-training/plans/{id}/publish")
    @RequirePermission("hr:training:update")
    public R<Void> publishTrainingPlan(@PathVariable Long id) {
        service.updateStatus("trainingPlan", id, "PUBLISHED");
        return R.ok();
    }

    @GetMapping("/api/hr-training/sessions")
    @RequirePermission("hr:training:list")
    public R<Map<String, Object>> trainingSessions(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("trainingSession", params));
    }

    @PostMapping("/api/hr-training/sessions")
    @RequirePermission("hr:training:update")
    public R<Long> createTrainingSession(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("trainingSession", body, Map.of("status", "DRAFT", "enrolledNum", 0)));
    }

    @PostMapping("/api/hr-training/sessions/{id}/start")
    @RequirePermission("hr:training:update")
    public R<Void> startTrainingSession(@PathVariable Long id) {
        service.updateStatus("trainingSession", id, "OPEN");
        return R.ok();
    }

    @PostMapping("/api/hr-training/sessions/{id}/close")
    @RequirePermission("hr:training:update")
    public R<Void> closeTrainingSession(@PathVariable Long id) {
        service.updateStatus("trainingSession", id, "CLOSED");
        return R.ok();
    }

    @GetMapping("/api/hr-training/enrollments")
    @RequirePermission("hr:training:list")
    public R<Map<String, Object>> trainingEnrollments(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("trainingEnroll", params));
    }

    @PostMapping("/api/hr-training/enrollments")
    @RequirePermission("hr:training:update")
    public R<Long> createTrainingEnrollment(@RequestBody Map<String, Object> body) {
        Long sessionId = requiredLong(body, "sessionId");
        Long empId = requiredLong(body, "empId");
        return R.ok(service.enrollTraining(sessionId, empId));
    }

    @PostMapping("/api/hr-training/enrollments/{id}/sign-in")
    @RequirePermission("hr:training:update")
    public R<Void> signInTraining(@PathVariable Long id) {
        service.updateFields("trainingEnroll", id, Map.of("attendance", "SIGNED", "signTime", LocalDateTime.now()));
        return R.ok();
    }

    @PostMapping("/api/hr-training/enrollments/{id}/score")
    @RequirePermission("hr:training:update")
    public R<Void> scoreTraining(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.scoreTraining(id, new BigDecimal(String.valueOf(body.getOrDefault("score", 0))));
        return R.ok();
    }

    @GetMapping("/api/hr-training/enrollments/records")
    @RequirePermission("hr:training:list")
    public R<Map<String, Object>> trainingRecords(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("trainingRecord", params));
    }

    @GetMapping("/api/knowledge/categories/tree")
    @RequirePermission("knowledge:list")
    public R<List<Map<String, Object>>> knowledgeCategories() {
        return R.ok(service.list("knowledgeCategory", Map.of()));
    }

    @PostMapping("/api/knowledge/categories")
    @RequirePermission("knowledge:update")
    public R<Long> createKnowledgeCategory(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("knowledgeCategory", body, Map.of("status", "ACTIVE")));
    }

    @GetMapping("/api/knowledge/entries")
    @RequirePermission("knowledge:list")
    public R<Map<String, Object>> knowledgeEntries(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("knowledgeEntry", params));
    }

    @PostMapping("/api/knowledge/entries")
    @RequirePermission("knowledge:update")
    public R<Long> createKnowledgeEntry(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("knowledgeEntry", body, Map.of("status", "DRAFT")));
    }

    @PostMapping("/api/knowledge/entries/{id}/actions/publish")
    @RequirePermission("knowledge:update")
    public R<Void> publishKnowledgeEntry(@PathVariable Long id) {
        service.updateStatus("knowledgeEntry", id, "PUBLISHED");
        return R.ok();
    }

    @PostMapping("/api/knowledge/entries/{id}/actions/archive")
    @RequirePermission("knowledge:update")
    public R<Void> archiveKnowledgeEntry(@PathVariable Long id) {
        service.updateStatus("knowledgeEntry", id, "ARCHIVED");
        return R.ok();
    }

    @GetMapping("/api/task/projects")
    @RequirePermission("task:list")
    public R<Map<String, Object>> taskProjects(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("taskProject", params));
    }

    @PostMapping("/api/task/projects")
    @RequirePermission("task:update")
    public R<Long> createTaskProject(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("taskProject", body, Map.of("status", "ACTIVE")));
    }

    @PutMapping("/api/task/projects/{id}/actions/status")
    @RequirePermission("task:update")
    public R<Void> updateTaskProjectStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateStatus("taskProject", id, requiredString(body, "status"));
        return R.ok();
    }

    @GetMapping("/api/task/items")
    @RequirePermission("task:list")
    public R<Map<String, Object>> taskItems(@RequestParam Map<String, Object> params) {
        return R.ok(service.page("taskItem", params));
    }

    @PostMapping("/api/task/items")
    @RequirePermission("task:update")
    public R<Long> createTaskItem(@RequestBody Map<String, Object> body) {
        return R.ok(service.create("taskItem", body, Map.of("status", "TODO", "progress", 0)));
    }

    @PutMapping("/api/task/items/{id}/actions/status")
    @RequirePermission("task:update")
    public R<Void> updateTaskItemStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateStatus("taskItem", id, requiredString(body, "status"));
        return R.ok();
    }

    @PutMapping("/api/task/items/{id}/actions/progress")
    @RequirePermission("task:update")
    public R<Void> updateTaskItemProgress(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        service.updateFields("taskItem", id, Map.of("progress", valueOrDefault(body.get("progress"), 0)));
        return R.ok();
    }

    private Map<String, Object> withCode(Map<String, Object> body, String field, String prefix) {
        Object value = body.get(field);
        if (value != null && !String.valueOf(value).isBlank()) {
            return body;
        }
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.put(field, prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        return copy;
    }

    private String requiredString(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BusinessException("Missing required field: " + field);
        }
        return String.valueOf(value);
    }

    private Long requiredLong(Map<String, Object> body, String field) {
        String value = requiredString(body, field);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid numeric field: " + field);
        }
    }

    private Object valueOrDefault(Object value, Object defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String stringOrDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
