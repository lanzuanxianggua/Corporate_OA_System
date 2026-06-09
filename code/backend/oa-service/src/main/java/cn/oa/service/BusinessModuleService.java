package cn.oa.service;

import cn.oa.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessModuleService {

    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, TableConfig> TABLES = buildTables();

    public Map<String, Object> page(String key, Map<String, ?> params) {
        TableConfig config = table(key);
        int pn = intParam(params, "pn", intParam(params, "pageNum", 1));
        int ps = intParam(params, "ps", intParam(params, "pageSize", 10));
        pn = Math.max(pn, 1);
        ps = Math.min(Math.max(ps, 1), 100);

        List<Object> args = new ArrayList<>();
        String where = buildWhere(config, params, args);
        String countSql = "SELECT COUNT(*) FROM " + config.table + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, args.toArray());
        if (total == null) {
            total = 0L;
        }

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(ps);
        pageArgs.add((pn - 1) * ps);
        String sql = "SELECT * FROM " + config.table + where + " ORDER BY " + config.sortColumn + " DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, pageArgs.toArray()).stream()
                .map(this::camelize)
                .collect(Collectors.toList());

        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", records);
        page.put("total", total);
        page.put("current", pn);
        page.put("size", ps);
        return page;
    }

    public List<Map<String, Object>> list(String key, Map<String, ?> params) {
        Object oldPn = params.get("pn");
        Object oldPs = params.get("ps");
        Map<String, Object> copy = new LinkedHashMap<>(params);
        copy.put("pn", oldPn == null ? 1 : oldPn);
        copy.put("ps", oldPs == null ? 100 : oldPs);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) page(key, copy).get("records");
        return records;
    }

    public Map<String, Object> get(String key, Long id) {
        TableConfig config = table(key);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + config.table + " WHERE id = ? " + activeClause(config),
                id);
        if (rows.isEmpty()) {
            throw new BusinessException("Record not found: " + id);
        }
        return camelize(rows.get(0));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(String key, Map<String, ?> body, Map<String, ?> defaults) {
        TableConfig config = table(key);
        Map<String, Object> fields = normalize(config, body);
        fields.putAll(normalize(config, defaults));
        fields.remove("id");
        fields.remove("create_time");
        fields.remove("update_time");
        fields.remove("del_flag");
        fields.remove("version");
        if (fields.isEmpty()) {
            throw new BusinessException("Request body is empty");
        }

        String columns = String.join(", ", fields.keySet());
        String placeholders = fields.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + config.table + " (" + columns + ") VALUES (" + placeholders + ")";
        Object[] args = fields.values().toArray();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        return generatedId == null ? null : generatedId.longValue();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String key, Long id, Map<String, ?> body) {
        TableConfig config = table(key);
        Map<String, Object> fields = normalize(config, body);
        fields.remove("id");
        fields.remove("create_time");
        fields.remove("update_time");
        fields.remove("del_flag");
        fields.remove("version");
        if (fields.isEmpty()) {
            return;
        }
        List<Object> args = new ArrayList<>(fields.values());
        args.add(id);
        String set = fields.keySet().stream().map(c -> c + " = ?").collect(Collectors.joining(", "));
        jdbcTemplate.update("UPDATE " + config.table + " SET " + set + " WHERE id = ?", args.toArray());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String key, Long id) {
        TableConfig config = table(key);
        if (config.columns.contains("del_flag")) {
            jdbcTemplate.update("UPDATE " + config.table + " SET del_flag = '1' WHERE id = ?", id);
        } else {
            jdbcTemplate.update("DELETE FROM " + config.table + " WHERE id = ?", id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String key, Long id, String status) {
        TableConfig config = table(key);
        ensureColumn(config, "status");
        jdbcTemplate.update("UPDATE " + config.table + " SET status = ? WHERE id = ?", status, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFields(String key, Long id, Map<String, ?> fields) {
        update(key, id, fields);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustSupplyStock(Long supplyId, int quantity, String location) {
        Map<String, Object> stock = first("supplyStock", Map.of("supplyId", supplyId));
        if (stock == null) {
            create("supplyStock", Map.of("supplyId", supplyId, "quantity", 0, "lockedQuantity", 0), Map.of());
            stock = first("supplyStock", Map.of("supplyId", supplyId));
        }
        int current = number(stock.get("quantity")).intValue();
        int next = current + quantity;
        if (next < 0) {
            throw new BusinessException("Insufficient stock");
        }
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("quantity", next);
        if (location != null && !location.isBlank()) {
            update.put("location", location);
        }
        update("supplyStock", number(stock.get("id")).longValue(), update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveSupplyRequest(Long requestId) {
        Map<String, Object> request = get("supplyRequest", requestId);
        String requestType = String.valueOf(request.getOrDefault("requestType", "OUT"));
        for (Map<String, Object> item : list("supplyRequestItem", Map.of("requestId", requestId))) {
            int quantity = number(item.get("quantity")).intValue();
            Long supplyId = number(item.get("supplyId")).longValue();
            adjustSupplyStock(supplyId, "OUT".equalsIgnoreCase(requestType) ? -quantity : quantity, null);
        }
        updateFields("supplyRequest", requestId, Map.of("status", "APPROVED", "approveTime", LocalDateTime.now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long enrollTraining(Long sessionId, Long empId) {
        Map<String, Object> session = get("trainingSession", sessionId);
        int enrolled = number(session.get("enrolledNum")).intValue();
        int capacity = number(session.get("maxCapacity")).intValue();
        if (capacity > 0 && enrolled >= capacity) {
            throw new BusinessException("Training session is full");
        }
        Long id = create("trainingEnroll", Map.of(
                "sessionId", sessionId,
                "empId", empId,
                "enrollTime", LocalDateTime.now(),
                "attendance", "PENDING"
        ), Map.of());
        updateFields("trainingSession", sessionId, Map.of("enrolledNum", enrolled + 1));
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void scoreTraining(Long enrollId, Number score) {
        Map<String, Object> enroll = get("trainingEnroll", enrollId);
        Long sessionId = number(enroll.get("sessionId")).longValue();
        Map<String, Object> session = get("trainingSession", sessionId);
        Long planId = number(session.get("planId")).longValue();
        Map<String, Object> plan = planId == 0 ? Map.of() : get("trainingPlan", planId);
        Long courseId = number(plan.get("courseId")).longValue();
        Number credit = courseId == 0 ? 0 : number(get("trainingCourse", courseId).get("credit"));
        updateFields("trainingEnroll", enrollId, Map.of("score", score, "creditGranted", credit));
        create("trainingRecord", Map.of(
                "empId", number(enroll.get("empId")).longValue(),
                "sessionId", sessionId,
                "courseId", courseId,
                "totalCredit", credit
        ), Map.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public void generatePerformanceResults(Long cycleId) {
        List<Map<String, Object>> goals = list("performanceGoal", Map.of("cycleId", cycleId));
        Map<Long, List<Map<String, Object>>> byEmp = goals.stream()
                .filter(row -> row.get("empId") != null)
                .collect(Collectors.groupingBy(row -> number(row.get("empId")).longValue(), LinkedHashMap::new, Collectors.toList()));
        List<Map.Entry<Long, Double>> scores = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> entry : byEmp.entrySet()) {
            double avg = entry.getValue().stream()
                    .map(row -> number(row.get("score")).doubleValue())
                    .filter(v -> v > 0)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
            scores.add(Map.entry(entry.getKey(), avg));
        }
        scores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        int rank = 1;
        for (Map.Entry<Long, Double> entry : scores) {
            create("performanceResult", Map.of(
                    "cycleId", cycleId,
                    "empId", entry.getKey(),
                    "totalScore", entry.getValue(),
                    "grade", grade(entry.getValue()),
                    "ranking", rank++,
                    "status", "GENERATED"
            ), Map.of());
        }
    }

    public Map<String, Object> first(String key, Map<String, ?> params) {
        List<Map<String, Object>> rows = list(key, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String buildWhere(TableConfig config, Map<String, ?> params, List<Object> args) {
        List<String> parts = new ArrayList<>();
        if (config.columns.contains("del_flag")) {
            parts.add("del_flag = '0'");
        }
        for (String filter : config.filters) {
            Object value = value(params, toCamel(filter), filter);
            if (value != null && !String.valueOf(value).isBlank()) {
                parts.add(filter + " = ?");
                args.add(value);
            }
        }
        Object keyword = value(params, "keyword", "q");
        if (keyword != null && !String.valueOf(keyword).isBlank() && !config.searchColumns.isEmpty()) {
            parts.add("(" + config.searchColumns.stream().map(c -> c + " LIKE ?").collect(Collectors.joining(" OR ")) + ")");
            for (int i = 0; i < config.searchColumns.size(); i++) {
                args.add("%" + keyword + "%");
            }
        }
        return parts.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts);
    }

    private Map<String, Object> normalize(TableConfig config, Map<String, ?> data) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (data == null) {
            return out;
        }
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            String column = toSnake(entry.getKey());
            if (config.columns.contains(column)) {
                out.put(column, convertValue(entry.getValue()));
            }
        }
        return out;
    }

    private Object convertValue(Object value) {
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        return value;
    }

    private Map<String, Object> camelize(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        row.forEach((key, value) -> out.put(toCamel(key), value));
        return out;
    }

    private String activeClause(TableConfig config) {
        return config.columns.contains("del_flag") ? "AND del_flag = '0'" : "";
    }

    private TableConfig table(String key) {
        TableConfig config = TABLES.get(key);
        if (config == null) {
            throw new BusinessException("Unknown business table: " + key);
        }
        return config;
    }

    private void ensureColumn(TableConfig config, String column) {
        if (!config.columns.contains(column)) {
            throw new BusinessException("Column is not allowed: " + column);
        }
    }

    private static Object value(Map<String, ?> params, String... keys) {
        if (params == null) {
            return null;
        }
        for (String key : keys) {
            if (params.containsKey(key)) {
                return params.get(key);
            }
        }
        return null;
    }

    private static int intParam(Map<String, ?> params, String key, int fallback) {
        Object value = value(params, key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static String grade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "E";
    }

    private static String toSnake(String input) {
        StringBuilder out = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                out.append('_').append(Character.toLowerCase(ch));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String toCamel(String input) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char ch : input.toCharArray()) {
            if (ch == '_') {
                upper = true;
            } else if (upper) {
                out.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static Map<String, TableConfig> buildTables() {
        Map<String, TableConfig> map = new LinkedHashMap<>();
        add(map, "supplyCategory", "adm_supply_category", "id,category_name,parent_id,status,del_flag,create_by,create_time,update_by,update_time,version", "category_name", "parent_id,status", "create_time");
        add(map, "supply", "adm_supply", "id,supply_code,supply_name,category_id,unit,spec,safety_stock,status,del_flag,create_by,create_time,update_by,update_time,version", "supply_code,supply_name", "category_id,status", "create_time");
        add(map, "supplyStock", "adm_supply_stock", "id,supply_id,quantity,locked_quantity,location,del_flag,create_by,create_time,update_by,update_time,version", "", "supply_id", "create_time");
        add(map, "supplyRequest", "adm_supply_request", "id,request_no,request_type,emp_id,dept_id,reason,status,approve_time,reject_reason,del_flag,create_by,create_time,update_by,update_time,version", "request_no,reason", "emp_id,dept_id,status", "create_time");
        add(map, "supplyRequestItem", "adm_supply_request_item", "id,request_id,supply_id,quantity,remark,del_flag,create_by,create_time,update_by,update_time,version", "remark", "request_id,supply_id", "create_time");
        add(map, "employeeContract", "hr_employee_contract", "id,emp_id,contract_no,contract_type,start_date,end_date,sign_date,status,remark,del_flag,create_by,create_time,update_by,update_time,version", "contract_no,remark", "emp_id,status", "create_time");
        add(map, "employeeChange", "hr_employee_change", "id,emp_id,change_type,before_dept_id,after_dept_id,before_post,after_post,effective_date,reason,status,del_flag,create_by,create_time,update_by,update_time,version", "reason,before_post,after_post", "emp_id,status", "create_time");
        add(map, "employeeCertificate", "hr_employee_certificate", "id,emp_id,certificate_name,certificate_no,issue_org,issue_date,expire_date,attachment_url,status,del_flag,create_by,create_time,update_by,update_time,version", "certificate_name,certificate_no,issue_org", "emp_id,status", "create_time");
        add(map, "employeeEducation", "hr_employee_education", "id,emp_id,school_name,major,degree,start_date,end_date,certificate_url,del_flag,create_by,create_time,update_by,update_time,version", "school_name,major,degree", "emp_id", "create_time");
        add(map, "financeContract", "fin_contract", "id,contract_no,contract_name,counterparty,amount,signed_date,start_date,end_date,owner_emp_id,dept_id,status,remark,del_flag,create_by,create_time,update_by,update_time,version", "contract_no,contract_name,counterparty", "dept_id,owner_emp_id,status", "create_time");
        add(map, "financePayment", "fin_payment", "id,payment_no,contract_id,expense_id,payee,amount,planned_date,paid_time,pay_method,status,remark,del_flag,create_by,create_time,update_by,update_time,version", "payment_no,payee,remark", "contract_id,expense_id,status", "create_time");
        add(map, "performanceGoal", "hr_perf_goal", "id,cycle_id,emp_id,goal_content,target_value,weight,score,grade,status,del_flag,create_by,create_time,update_by,update_time,version", "goal_content,target_value", "cycle_id,emp_id,status", "create_time");
        add(map, "performanceEval", "hr_perf_eval", "id,goal_id,evaluator_id,eval_type,score,comment,status,del_flag,create_by,create_time,update_by,update_time,version", "comment", "goal_id,evaluator_id,status", "create_time");
        add(map, "performanceResult", "hr_perf_result", "id,cycle_id,emp_id,total_score,grade,ranking,status,del_flag,create_by,create_time,update_by,update_time,version", "grade", "cycle_id,emp_id,status", "create_time");
        add(map, "recruitJob", "hr_recruit_job", "id,job_title,dept_id,headcount,job_desc,status,del_flag,create_by,create_time,update_by,update_time,version", "job_title,job_desc", "dept_id,status", "create_time");
        add(map, "recruitCandidate", "hr_recruit_candidate", "id,candidate_name,phone,email,position,source,status,remark,del_flag,create_by,create_time,update_by,update_time,version", "candidate_name,phone,email,position", "status", "create_time");
        add(map, "recruitInterview", "hr_recruit_interview", "id,candidate_id,interviewer_id,interview_time,interview_type,result,comment,status,del_flag,create_by,create_time,update_by,update_time,version", "comment,result", "candidate_id,interviewer_id,status", "create_time");
        add(map, "recruitOffer", "hr_recruit_offer", "id,candidate_id,offer_no,position,salary,send_time,accept_time,onboard_time,status,remark,del_flag,create_by,create_time,update_by,update_time,version", "offer_no,position,remark", "candidate_id,status", "create_time");
        add(map, "trainingCourse", "hr_train_course", "id,course_name,course_type,credit,total_hours,status,del_flag,create_by,create_time,update_by,update_time,version", "course_name,course_type", "status", "create_time");
        add(map, "trainingPlan", "hr_train_plan", "id,plan_name,year,course_id,total_budget,status,del_flag,create_by,create_time,update_by,update_time,version", "plan_name", "year,course_id,status", "create_time");
        add(map, "trainingSession", "hr_train_session", "id,plan_id,session_name,location,trainer,max_capacity,enrolled_num,status,del_flag,create_by,create_time,update_by,update_time,version", "session_name,location,trainer", "plan_id,status", "create_time");
        add(map, "trainingEnroll", "hr_train_enroll", "id,session_id,emp_id,enroll_time,attendance,sign_time,score,credit_granted,del_flag,create_by,create_time,update_by,update_time,version", "", "session_id,emp_id,attendance", "create_time");
        add(map, "trainingRecord", "hr_train_record", "id,emp_id,session_id,course_id,total_credit,del_flag,create_by,create_time,update_by,update_time,version", "", "emp_id,session_id,course_id", "create_time");
        add(map, "knowledgeCategory", "km_category", "id,category_name,parent_id,status,del_flag,create_by,create_time,update_by,update_time,version", "category_name", "parent_id,status", "create_time");
        add(map, "knowledgeEntry", "km_entry", "id,category_id,title,summary,content,tags,status,del_flag,create_by,create_time,update_by,update_time,version", "title,summary,content,tags", "category_id,status", "create_time");
        add(map, "taskProject", "task_project", "id,project_name,dept_id,owner_id,status,description,del_flag,create_by,create_time,update_by,update_time,version", "project_name,description", "dept_id,owner_id,status", "create_time");
        add(map, "taskItem", "task_item", "id,project_id,task_name,assignee_id,priority,progress,status,description,del_flag,create_by,create_time,update_by,update_time,version", "task_name,description", "project_id,assignee_id,status,priority", "create_time");
        return Collections.unmodifiableMap(map);
    }

    private static void add(Map<String, TableConfig> map, String key, String table, String columns, String search, String filters, String sort) {
        map.put(key, new TableConfig(table, split(columns), splitList(search), split(search), split(filters), sort));
    }

    private static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> splitList(String csv) {
        return new ArrayList<>(split(csv));
    }

    private record TableConfig(String table, Set<String> columns, List<String> searchColumns, Set<String> searchSet, Set<String> filters, String sortColumn) {
    }
}
