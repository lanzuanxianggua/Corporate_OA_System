package cn.oa.workflow.core.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表达式解析器
 * 支持条件路由中的表达式解析
 *
 * 支持的语法：
 * - 简单比较: amount > 5000, status == 'APPROVED'
 * - 包含检查: department in ['IT', 'HR']
 * - 字符串匹配: name contains '张'
 * - 逻辑运算: amount > 1000 && status == 'PENDING'
 */
@Slf4j
@Component
public class ExpressionParser {

    /**
     * 解析并计算表达式
     *
     * @param expression 表达式字符串
     * @param context    上下文变量
     * @return 计算结果
     */
    public boolean evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return true;
        }

        if (context == null || context.isEmpty()) {
            log.warn("Empty context for expression: {}", expression);
            return false;
        }

        try {
            // 替换变量
            String resolved = resolveVariables(expression, context);

            // 计算表达式
            return evaluateExpression(resolved);
        } catch (Exception e) {
            log.error("Failed to evaluate expression: {} with context: {}", expression, context, e);
            return false;
        }
    }

    /**
     * 替换表达式中的变量
     */
    private String resolveVariables(String expression, Map<String, Object> context) {
        String result = expression;

        // 替换字符串变量: ${var} -> 'value'
        Pattern stringPattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = stringPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            if (value != null) {
                if (value instanceof String) {
                    matcher.appendReplacement(sb, "'" + value + "'");
                } else {
                    matcher.appendReplacement(sb, String.valueOf(value));
                }
            } else {
                matcher.appendReplacement(sb, "null");
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();

        // 直接替换变量名（不带${}）
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !result.contains("${" + varName + "}")) {
                String varValue;
                if (value instanceof String) {
                    varValue = "'" + value + "'";
                } else {
                    varValue = String.valueOf(value);
                }
                // 使用词边界匹配，避免部分替换
                result = result.replaceAll("\\b" + varName + "\\b", varValue);
            }
        }

        return result;
    }

    /**
     * 计算表达式
     */
    private boolean evaluateExpression(String expression) {
        // 去除空格
        String expr = expression.trim();
        log.debug("Evaluating expression: {}", expr);

        // 处理逻辑运算符
        if (expr.contains("&&")) {
            String[] parts = expr.split("&&");
            for (String part : parts) {
                if (!evaluateExpression(part.trim())) {
                    return false;
                }
            }
            return true;
        }

        if (expr.contains("||")) {
            String[] parts = expr.split("\\|\\|");
            for (String part : parts) {
                if (evaluateExpression(part.trim())) {
                    return true;
                }
            }
            return false;
        }

        // 处理 in 运算符
        if (expr.contains(" in ")) {
            return evaluateInExpression(expr);
        }

        // 处理 contains 运算符
        if (expr.contains(" contains ")) {
            return evaluateContainsExpression(expr);
        }

        // 处理比较运算符
        return evaluateComparison(expr);
    }

    /**
     * 处理 in 运算符
     * 格式: value in ['a', 'b', 'c']
     */
    private boolean evaluateInExpression(String expr) {
        String[] parts = expr.split(" in ");
        if (parts.length != 2) return false;

        String value = parts[0].trim();
        String arrayStr = parts[1].trim();

        // 解析数组
        if (arrayStr.startsWith("[") && arrayStr.endsWith("]")) {
            arrayStr = arrayStr.substring(1, arrayStr.length() - 1);
            String[] elements = arrayStr.split(",");
            for (String element : elements) {
                element = element.trim().replace("'", "").replace("\"", "");
                if (value.replace("'", "").equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 处理 contains 运算符
     * 格式: string contains 'substring'
     */
    private boolean evaluateContainsExpression(String expr) {
        String[] parts = expr.split(" contains ");
        if (parts.length != 2) return false;

        String str = parts[0].trim().replace("'", "");
        String substring = parts[1].trim().replace("'", "");
        return str.contains(substring);
    }

    /**
     * 处理比较运算符
     */
    private boolean evaluateComparison(String expr) {
        // >=
        if (expr.contains(">=")) {
            String[] parts = expr.split(">=");
            return compareNumeric(parts[0].trim(), parts[1].trim()) >= 0;
        }
        // <=
        if (expr.contains("<=")) {
            String[] parts = expr.split("<=");
            return compareNumeric(parts[0].trim(), parts[1].trim()) <= 0;
        }
        // !=
        if (expr.contains("!=")) {
            String[] parts = expr.split("!=");
            return !compareString(parts[0].trim(), parts[1].trim());
        }
        // ==
        if (expr.contains("==")) {
            String[] parts = expr.split("==");
            return compareString(parts[0].trim(), parts[1].trim());
        }
        // >
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            return compareNumeric(parts[0].trim(), parts[1].trim()) > 0;
        }
        // <
        if (expr.contains("<")) {
            String[] parts = expr.split("<");
            return compareNumeric(parts[0].trim(), parts[1].trim()) < 0;
        }

        // 无法解析的表达式，返回 true
        log.warn("Unrecognized expression format: {}", expr);
        return true;
    }

    /**
     * 数值比较
     */
    private int compareNumeric(String left, String right) {
        try {
            double l = parseNumeric(left);
            double r = parseNumeric(right);
            return Double.compare(l, r);
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，按字符串比较
            return left.compareTo(right);
        }
    }

    /**
     * 解析数值（去除引号）
     */
    private double parseNumeric(String value) {
        value = value.trim().replace("'", "").replace("\"", "");
        return Double.parseDouble(value);
    }

    /**
     * 字符串比较
     */
    private boolean compareString(String left, String right) {
        left = left.trim().replace("'", "").replace("\"", "");
        right = right.trim().replace("'", "").replace("\"", "");
        return left.equals(right);
    }
}
