package cn.oa.service.workflow;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight graph workflow runtime.
 *
 * <p>The engine intentionally keeps BPMN-like semantics small and OA-focused:
 * start/end, approval nodes, gateways, conditional edges, and node routing
 * rules. Existing schemaVersion=2 definitions are accepted; new definitions
 * should use schemaVersion=3.</p>
 */
@Component
public class WorkflowRuntimeEngine {
    public static final int CURRENT_SCHEMA_VERSION = 3;
    private static final int MIN_GRAPH_SCHEMA_VERSION = 2;

    public WorkflowGraph parseNodeConfig(String nodeConfig) {
        if (nodeConfig == null || nodeConfig.isBlank()) {
            return graphWithError(CURRENT_SCHEMA_VERSION, "empty", null, "nodeConfig 为空");
        }

        String trimmed = nodeConfig.trim();
        if (!trimmed.startsWith("{")) {
            return graphWithError(0, "graph_schema_required", null, "流程定义必须是 graph JSON，旧数组格式已不支持");
        }

        JSONObject config;
        try {
            config = JSONUtil.parseObj(trimmed);
        } catch (Exception e) {
            return graphWithError(CURRENT_SCHEMA_VERSION, "parse_error", null, "流程定义 JSON 解析失败: " + e.getMessage());
        }

        int schemaVersion = config.getInt("schemaVersion", 0);
        JSONArray nodesArr = config.getJSONArray("nodes");
        JSONArray edgesArr = config.getJSONArray("edges");

        Map<String, JSONObject> nodes = new LinkedHashMap<>();
        Map<String, List<JSONObject>> outgoing = new LinkedHashMap<>();
        List<JSONObject> edges = new ArrayList<>();
        List<WorkflowValidationError> errors = new ArrayList<>();

        if (schemaVersion < MIN_GRAPH_SCHEMA_VERSION) {
            errors.add(new WorkflowValidationError("graph_schema_required", null,
                    "流程定义必须声明 schemaVersion >= " + MIN_GRAPH_SCHEMA_VERSION));
        }

        if (nodesArr == null || nodesArr.isEmpty()) {
            errors.add(new WorkflowValidationError("no_nodes", null, "图格式缺少 nodes 数组"));
        } else {
            for (int i = 0; i < nodesArr.size(); i++) {
                JSONObject node = nodesArr.getJSONObject(i);
                String nodeId = node.getStr("nodeId");
                if (nodeId == null || nodeId.isBlank()) {
                    errors.add(new WorkflowValidationError("missing_node_id", null, "第 " + i + " 个节点缺少 nodeId"));
                    continue;
                }
                if (nodes.containsKey(nodeId)) {
                    errors.add(new WorkflowValidationError("duplicate_node_id", nodeId, "节点 ID 重复: " + nodeId));
                }
                normalizeNode(node);
                nodes.put(nodeId, node);
                outgoing.computeIfAbsent(nodeId, key -> new ArrayList<>());
            }
        }

        if (edgesArr == null || edgesArr.isEmpty()) {
            errors.add(new WorkflowValidationError("no_edges", null, "流程图至少需要一条连线"));
        }

        if (edgesArr != null) {
            for (int i = 0; i < edgesArr.size(); i++) {
                JSONObject edge = edgesArr.getJSONObject(i);
                String source = firstText(edge.getStr("source"), edge.getStr("sourceId"));
                String target = firstText(edge.getStr("target"), edge.getStr("targetId"));
                edge.set("source", source);
                edge.set("target", target);
                if (source == null || !nodes.containsKey(source)) {
                    errors.add(new WorkflowValidationError("unknown_edge_endpoint", source, "边的 source 不存在: " + source));
                }
                if (target == null || !nodes.containsKey(target)) {
                    errors.add(new WorkflowValidationError("unknown_edge_endpoint", target, "边的 target 不存在: " + target));
                }
                if (source != null && target != null) {
                    outgoing.computeIfAbsent(source, key -> new ArrayList<>()).add(edge);
                    edges.add(edge);
                }
            }
        }

        long startCount = nodes.values().stream().filter(node -> "start".equals(node.getStr("nodeType"))).count();
        long endCount = nodes.values().stream().filter(node -> "end".equals(node.getStr("nodeType"))).count();
        long approvalCount = nodes.values().stream().filter(node -> "approval".equals(node.getStr("nodeType"))).count();
        boolean hasStart = startCount > 0;
        boolean hasEnd = endCount > 0;
        if (startCount == 0) {
            errors.add(new WorkflowValidationError("no_start", null, "图缺少开始节点"));
        } else if (startCount > 1) {
            errors.add(new WorkflowValidationError("multiple_start", null, "流程图只能有一个开始节点"));
        }
        if (endCount == 0) {
            errors.add(new WorkflowValidationError("no_end", null, "图缺少结束节点"));
        } else if (endCount > 1) {
            errors.add(new WorkflowValidationError("multiple_end", null, "流程图只能有一个结束节点"));
        }
        if (approvalCount == 0) {
            errors.add(new WorkflowValidationError("no_approval", null, "流程图至少需要一个审批节点"));
        }

        if (hasStart) {
            String startId = nodes.values().stream()
                    .filter(node -> "start".equals(node.getStr("nodeType")))
                    .findFirst()
                    .map(node -> node.getStr("nodeId"))
                    .orElse(null);
            if (startId != null && hasCycle(startId, outgoing, new HashSet<>(), new HashSet<>())) {
                errors.add(new WorkflowValidationError("cycle", startId, "图存在环，可能导致死循环"));
            }
        }

        for (JSONObject node : nodes.values()) {
            validateApprovalNode(node, errors);
            validateRoutingRules(node, nodes, errors);
            validateGatewayNode(node, nodes, errors);
        }
        if (hasStart && hasEnd) {
            validateConnectivity(nodes, outgoing, errors);
        }

        return new WorkflowGraph(schemaVersion, nodes, outgoing, edges, errors);
    }

    public JSONArray materializeGraphToRuntimePath(WorkflowGraph graph, Map<String, Object> context) {
        JSONArray output = new JSONArray();
        if (graph == null || !graph.valid) {
            return output;
        }

        String currentId = graph.nodes.values().stream()
                .filter(node -> "start".equals(node.getStr("nodeType")))
                .map(node -> node.getStr("nodeId"))
                .findFirst()
                .orElse(null);
        if (currentId == null) {
            return output;
        }

        Set<String> visited = new HashSet<>();
        int runtimeIndex = 0;
        int safetyLimit = 96;

        while (currentId != null && safetyLimit-- > 0) {
            if (!visited.add(currentId)) {
                break;
            }
            JSONObject current = graph.nodes.get(currentId);
            if (current == null) {
                break;
            }
            String type = current.getStr("nodeType");
            if ("approval".equals(type) || "cc".equals(type) || "subprocess".equals(type)) {
                JSONObject node = new JSONObject();
                node.set("runtimeIndex", runtimeIndex++);
                node.set("nodeId", currentId);
                node.set("nodeName", current.getStr("nodeName", current.getStr("name")));
                node.set("nodeType", type);
                node.set("assigneeType", current.getStr("assigneeType"));
                node.set("assigneeValue", current.getStr("assigneeValue"));
                node.set("multiType", current.getStr("multiType"));
                node.set("multiAssigneeIds", current.getJSONArray("multiAssigneeIds"));
                node.set("conditions", current.getJSONArray("conditions"));
                node.set("ccList", current.getJSONArray("ccList"));
                node.set("timeoutHours", current.getInt("timeoutHours", 0));
                node.set("timeoutAction", current.getStr("timeoutAction", "notify_only"));
                node.set("escalateTo", current.getJSONObject("escalateTo"));
                node.set("subProcessKey", current.getStr("subProcessKey"));
                output.add(node);
            } else if ("end".equals(type)) {
                break;
            }

            currentId = findNextNode(graph, currentId, context);
        }
        return output;
    }

    public String findNextNode(WorkflowGraph graph, String currentNodeId, Map<String, Object> context) {
        if (graph == null || !graph.valid || currentNodeId == null) {
            return null;
        }
        JSONObject current = graph.nodes.get(currentNodeId);
        if (current == null) {
            return null;
        }

        JSONArray rules = current.getJSONArray("routingRules");
        if (rules != null && context != null) {
            for (int i = 0; i < rules.size(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                if (evaluateRoutingRule(rule, context)) {
                    String skipTo = rule.getStr("skipTo");
                    if (skipTo != null && graph.nodes.containsKey(skipTo)) {
                        return skipTo;
                    }
                    String jumpTo = rule.getStr("jumpTo");
                    if (jumpTo != null && graph.nodes.containsKey(jumpTo)) {
                        return jumpTo;
                    }
                }
            }
        }

        if ("gateway".equals(current.getStr("nodeType"))) {
            String branch = pickGatewayBranch(current, graph, context);
            if (branch != null) {
                return branch;
            }
        }

        List<JSONObject> edges = graph.outgoing.getOrDefault(currentNodeId, Collections.emptyList());
        for (JSONObject edge : edges) {
            if (evaluateEdgeCondition(edge, context)) {
                return edge.getStr("target");
            }
        }
        return edges.isEmpty() ? null : edges.get(0).getStr("target");
    }

    public List<JSONObject> filterApplicableNodes(JSONArray nodes, Map<String, Object> context) {
        List<JSONObject> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            JSONArray conditions = node.getJSONArray("conditions");
            if (conditions == null || conditions.isEmpty() || context == null || context.isEmpty()) {
                result.add(node);
            } else if (evaluateConditions(conditions, context)) {
                result.add(node);
            }
        }
        return result;
    }

    public boolean evaluateConditions(JSONArray conditions, Map<String, Object> context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        if (context == null || context.isEmpty()) {
            return false;
        }
        for (int i = 0; i < conditions.size(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            if (!evaluateCondition(condition, context)) {
                return false;
            }
        }
        return true;
    }

    private void normalizeNode(JSONObject node) {
        if (node.getStr("nodeName") == null && node.getStr("name") != null) {
            node.set("nodeName", node.getStr("name"));
        }
        if (node.getStr("name") == null && node.getStr("nodeName") != null) {
            node.set("name", node.getStr("nodeName"));
        }
        if ("condition".equals(node.getStr("nodeType"))) {
            node.set("nodeType", "gateway");
            node.set("gatewayType", "exclusive");
        }
        if ("approval".equals(node.getStr("nodeType"))) {
            String mode = node.getStr("approvalMode");
            if (mode != null && node.getStr("multiType") == null) {
                if ("countersign".equals(mode) || "orsign".equals(mode)) {
                    node.set("multiType", mode);
                }
            }
            JSONObject assigneeRule = node.getJSONObject("assigneeRule");
            if (assigneeRule != null) {
                node.set("assigneeType", firstText(node.getStr("assigneeType"), assigneeRule.getStr("type")));
                node.set("assigneeValue", firstText(node.getStr("assigneeValue"),
                        assigneeRule.getStr("value"),
                        assigneeRule.getStr("roleKey"),
                        assigneeRule.getStr("userId"),
                        assigneeRule.getStr("employeeId"),
                        assigneeRule.getStr("assigneeId"),
                        assigneeRule.getStr("deptId"),
                        assigneeRule.getStr("scope")));
            }
        }
    }

    private void validateApprovalNode(JSONObject node, List<WorkflowValidationError> errors) {
        if (!"approval".equals(node.getStr("nodeType"))) {
            return;
        }
        String assigneeType = node.getStr("assigneeType");
        String assigneeValue = node.getStr("assigneeValue");
        if (assigneeType == null || assigneeType.isBlank()) {
            errors.add(new WorkflowValidationError("missing_assignee_type", node.getStr("nodeId"), "审批节点缺少审批人类型"));
        }
        if (assigneeValue == null || assigneeValue.isBlank()) {
            errors.add(new WorkflowValidationError("missing_assignee_value", node.getStr("nodeId"), "审批节点缺少审批人配置"));
        }
    }

    private void validateRoutingRules(JSONObject node,
                                      Map<String, JSONObject> nodes,
                                      List<WorkflowValidationError> errors) {
        JSONArray rules = node.getJSONArray("routingRules");
        if (rules == null || rules.isEmpty()) {
            return;
        }
        for (int i = 0; i < rules.size(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            String when = firstText(rule.getStr("when"), rule.getStr("condition"));
            String target = firstText(rule.getStr("skipTo"), rule.getStr("jumpTo"));
            if (when == null || when.isBlank()) {
                errors.add(new WorkflowValidationError("missing_routing_condition", node.getStr("nodeId"),
                        "条件路由缺少 when 表达式"));
            }
            if (target == null || !nodes.containsKey(target)) {
                errors.add(new WorkflowValidationError("unknown_routing_target", node.getStr("nodeId"),
                        "条件路由目标不存在: " + target));
            }
        }
    }

    private void validateGatewayNode(JSONObject node,
                                     Map<String, JSONObject> nodes,
                                     List<WorkflowValidationError> errors) {
        if (!"gateway".equals(node.getStr("nodeType"))) {
            return;
        }
        JSONArray branches = node.getJSONArray("branches");
        if (branches == null || branches.isEmpty()) {
            return;
        }
        for (int i = 0; i < branches.size(); i++) {
            JSONObject branch = branches.getJSONObject(i);
            String target = branch.getStr("to");
            if (target == null || !nodes.containsKey(target)) {
                errors.add(new WorkflowValidationError("unknown_branch_target", node.getStr("nodeId"),
                        "条件分支目标不存在: " + target));
            }
            if (branch.getStr("when") == null || branch.getStr("when").isBlank()) {
                errors.add(new WorkflowValidationError("missing_branch_condition", node.getStr("nodeId"),
                        "条件分支缺少 when 表达式"));
            }
        }
    }

    private void validateConnectivity(Map<String, JSONObject> nodes,
                                      Map<String, List<JSONObject>> outgoing,
                                      List<WorkflowValidationError> errors) {
        String startId = nodes.values().stream()
                .filter(node -> "start".equals(node.getStr("nodeType")))
                .map(node -> node.getStr("nodeId"))
                .findFirst()
                .orElse(null);
        if (startId == null) {
            return;
        }

        Set<String> reachable = new HashSet<>();
        collectReachable(startId, nodes, outgoing, reachable);
        boolean endReachable = nodes.values().stream()
                .filter(node -> "end".equals(node.getStr("nodeType")))
                .map(node -> node.getStr("nodeId"))
                .anyMatch(reachable::contains);
        if (!endReachable) {
            errors.add(new WorkflowValidationError("no_path_to_end", startId, "开始节点无法流转到结束节点"));
        }

        for (JSONObject node : nodes.values()) {
            String nodeId = node.getStr("nodeId");
            if (nodeId != null && !reachable.contains(nodeId)) {
                errors.add(new WorkflowValidationError("unreachable_node", nodeId, "节点无法从开始节点到达: " + nodeId));
            }
        }
    }

    private void collectReachable(String nodeId,
                                  Map<String, JSONObject> nodes,
                                  Map<String, List<JSONObject>> outgoing,
                                  Set<String> reachable) {
        if (nodeId == null || !nodes.containsKey(nodeId) || !reachable.add(nodeId)) {
            return;
        }
        for (String target : adjacencyTargets(nodeId, nodes, outgoing)) {
            collectReachable(target, nodes, outgoing, reachable);
        }
    }

    private List<String> adjacencyTargets(String nodeId,
                                          Map<String, JSONObject> nodes,
                                          Map<String, List<JSONObject>> outgoing) {
        List<String> targets = new ArrayList<>();
        for (JSONObject edge : outgoing.getOrDefault(nodeId, Collections.emptyList())) {
            String target = edge.getStr("target");
            if (target != null) {
                targets.add(target);
            }
        }

        JSONObject node = nodes.get(nodeId);
        if (node == null) {
            return targets;
        }
        JSONArray branches = node.getJSONArray("branches");
        if (branches != null) {
            for (int i = 0; i < branches.size(); i++) {
                String target = branches.getJSONObject(i).getStr("to");
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        JSONArray rules = node.getJSONArray("routingRules");
        if (rules != null) {
            for (int i = 0; i < rules.size(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                String target = firstText(rule.getStr("skipTo"), rule.getStr("jumpTo"));
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    private WorkflowGraph graphWithError(int schemaVersion, String type, String nodeId, String message) {
        return new WorkflowGraph(schemaVersion, new LinkedHashMap<>(), new LinkedHashMap<>(), new ArrayList<>(),
                Collections.singletonList(new WorkflowValidationError(type, nodeId, message)));
    }

    private boolean hasCycle(String startId,
                             Map<String, List<JSONObject>> outgoing,
                             Set<String> visiting,
                             Set<String> visited) {
        if (visited.contains(startId)) {
            return false;
        }
        if (visiting.contains(startId)) {
            return true;
        }
        visiting.add(startId);
        for (JSONObject edge : outgoing.getOrDefault(startId, Collections.emptyList())) {
            String target = edge.getStr("target");
            if (target != null && hasCycle(target, outgoing, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(startId);
        visited.add(startId);
        return false;
    }

    private String pickGatewayBranch(JSONObject gateway, WorkflowGraph graph, Map<String, Object> context) {
        JSONArray branches = gateway.getJSONArray("branches");
        if (branches != null) {
            for (int i = 0; i < branches.size(); i++) {
                JSONObject branch = branches.getJSONObject(i);
                if (evaluateRoutingRule(branch, context)) {
                    String target = branch.getStr("to");
                    if (target != null && graph.nodes.containsKey(target)) {
                        return target;
                    }
                }
            }
        }
        String gatewayId = gateway.getStr("nodeId");
        List<JSONObject> edges = graph.outgoing.getOrDefault(gatewayId, Collections.emptyList());
        return edges.isEmpty() ? null : edges.get(0).getStr("target");
    }

    private boolean evaluateEdgeCondition(JSONObject edge, Map<String, Object> context) {
        JSONObject condition = edge.getJSONObject("condition");
        if (condition == null) {
            return true;
        }
        return evaluateCondition(condition, context);
    }

    private boolean evaluateRoutingRule(JSONObject rule, Map<String, Object> context) {
        String when = firstText(rule.getStr("when"), rule.getStr("condition"));
        return when != null && evaluateExpression(when, context);
    }

    private boolean evaluateCondition(JSONObject condition, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return false;
        }
        String field = condition.getStr("field");
        String operator = firstText(condition.getStr("operator"), condition.getStr("op"));
        if (field == null || operator == null) {
            return false;
        }
        Object rawValue = resolveContextPath(field, context);
        Object threshold = condition.get("value");
        if (rawValue == null || threshold == null) {
            return false;
        }

        return switch (operator) {
            case "<=" -> compare(rawValue, threshold, "<=");
            case "<" -> compare(rawValue, threshold, "<");
            case ">=" -> compare(rawValue, threshold, ">=");
            case ">" -> compare(rawValue, threshold, ">");
            case "==" -> compare(rawValue, threshold, "==");
            case "!=" -> compare(rawValue, threshold, "!=");
            case "equals" -> String.valueOf(rawValue).equals(String.valueOf(threshold));
            case "not_equals" -> !String.valueOf(rawValue).equals(String.valueOf(threshold));
            case "contains" -> String.valueOf(rawValue).contains(String.valueOf(threshold));
            case "starts_with" -> String.valueOf(rawValue).startsWith(String.valueOf(threshold));
            case "in" -> {
                JSONArray values = condition.getJSONArray("values");
                yield values != null && values.toList(String.class).contains(String.valueOf(rawValue));
            }
            default -> false;
        };
    }

    private boolean evaluateExpression(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank() || context == null || context.isEmpty()) {
            return false;
        }
        List<String> orParts = splitExpression(expression, "||");
        if (orParts.size() > 1) {
            return orParts.stream().anyMatch(part -> evaluateExpression(part, context));
        }
        List<String> andParts = splitExpression(expression, "&&");
        if (andParts.size() > 1) {
            return andParts.stream().allMatch(part -> evaluateExpression(part, context));
        }

        String[] operators = {"<=", ">=", "==", "!=", "<", ">"};
        for (String operator : operators) {
            int idx = expression.indexOf(operator);
            if (idx <= 0) {
                continue;
            }
            String left = expression.substring(0, idx).trim();
            String right = expression.substring(idx + operator.length()).trim();
            Object leftValue = resolveContextPath(left, context);
            Object rightValue = tryParseLiteral(right);
            if (leftValue == null || rightValue == null) {
                return false;
            }
            return compare(leftValue, rightValue, operator);
        }
        return false;
    }

    private List<String> splitExpression(String expression, String delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i <= expression.length() - delimiter.length(); i++) {
            char ch = expression.charAt(i);
            if ((ch == '"' || ch == '\'') && (i == 0 || expression.charAt(i - 1) != '\\')) {
                if (!inQuote) {
                    inQuote = true;
                    quote = ch;
                } else if (quote == ch) {
                    inQuote = false;
                    quote = 0;
                }
            }
            if (!inQuote && expression.startsWith(delimiter, i)) {
                parts.add(expression.substring(start, i).trim());
                i += delimiter.length() - 1;
                start = i + 1;
            }
        }
        if (start == 0) {
            return Collections.singletonList(expression.trim());
        }
        parts.add(expression.substring(start).trim());
        return parts;
    }

    private Object resolveContextPath(String path, Map<String, Object> context) {
        if (path == null || context == null) {
            return null;
        }
        String normalized = path.startsWith("context.") ? path.substring("context.".length()) : path;
        if (context.containsKey(normalized)) {
            return context.get(normalized);
        }
        if (context.containsKey(path)) {
            return context.get(path);
        }
        String[] parts = normalized.split("\\.");
        Object current = context;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private Object tryParseLiteral(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            // keep parsing other literal types
        }
        if ("true".equals(trimmed)) {
            return Boolean.TRUE;
        }
        if ("false".equals(trimmed)) {
            return Boolean.FALSE;
        }
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private boolean compare(Object left, Object right, String operator) {
        Double leftNumber = toDoubleOrNull(left);
        Double rightNumber = toDoubleOrNull(right);
        if (leftNumber != null && rightNumber != null) {
            return switch (operator) {
                case "<=" -> leftNumber <= rightNumber;
                case "<" -> leftNumber < rightNumber;
                case ">=" -> leftNumber >= rightNumber;
                case ">" -> leftNumber > rightNumber;
                case "==" -> Math.abs(leftNumber - rightNumber) < 1e-9;
                case "!=" -> Math.abs(leftNumber - rightNumber) >= 1e-9;
                default -> false;
            };
        }
        int comparison = String.valueOf(left).compareTo(String.valueOf(right));
        return switch (operator) {
            case "==" -> String.valueOf(left).equals(String.valueOf(right));
            case "!=" -> !String.valueOf(left).equals(String.valueOf(right));
            case "<=" -> comparison <= 0;
            case "<" -> comparison < 0;
            case ">=" -> comparison >= 0;
            case ">" -> comparison > 0;
            default -> false;
        };
    }

    private Double toDoubleOrNull(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
