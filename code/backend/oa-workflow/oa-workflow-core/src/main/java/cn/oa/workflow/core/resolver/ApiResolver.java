package cn.oa.workflow.core.resolver;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * API解析器 - 调用外部接口获取审批人
 * ruleValue: API配置JSON
 *            {
 *              "url": "http://api.example.com/approvers",
 *              "method": "POST",
 *              "headers": {"Authorization": "Bearer xxx"},
 *              "body": {"businessId": "${businessId}"},
 *              "resultPath": "$.data.approvers[*].id"
 *            }
 */
@Slf4j
@Component
@Order(60)
public class ApiResolver implements AssigneeResolver {

    private static final int TIMEOUT = 5000; // 5秒超时

    @Override
    public String getRuleType() {
        return "API";
    }

    @Override
    public List<Long> resolve(String ruleValue, Long starterId, String formDataSnapshot) {
        if (ruleValue == null || ruleValue.isBlank()) {
            log.warn("ApiResolver: ruleValue is empty");
            return Collections.emptyList();
        }

        try {
            var config = JSONUtil.parseObj(ruleValue);
            String url = config.getStr("url");
            String method = config.getStr("method", "GET");
            String resultPath = config.getStr("resultPath", "$.data");

            if (url == null || url.isBlank()) {
                log.warn("ApiResolver: url is missing in config");
                return Collections.emptyList();
            }

            // 替换变量
            url = replaceVariables(url, starterId, formDataSnapshot);

            log.debug("ApiResolver: calling {} {}", method, url);

            // 构建请求
            HttpRequest request;
            if ("POST".equalsIgnoreCase(method)) {
                request = HttpRequest.post(url);
                String body = config.getStr("body");
                if (body != null) {
                    body = replaceVariables(body, starterId, formDataSnapshot);
                    request.body(body, "application/json");
                }
            } else {
                request = HttpRequest.get(url);
            }

            // 添加请求头
            var headers = config.getJSONObject("headers");
            if (headers != null) {
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    request.header(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            // 发送请求
            HttpResponse response = request.timeout(TIMEOUT).execute();
            if (!response.isOk()) {
                log.error("ApiResolver: API call failed with status {}", response.getStatus());
                return Collections.emptyList();
            }

            // 解析响应
            String responseBody = response.body();
            return extractUserIds(responseBody, resultPath);

        } catch (Exception e) {
            log.error("ApiResolver: error calling API", e);
            return Collections.emptyList();
        }
    }

    /**
     * 替换变量占位符
     */
    private String replaceVariables(String template, Long starterId, String formDataSnapshot) {
        if (template == null) return null;

        String result = template;
        result = result.replace("${starterId}", String.valueOf(starterId));

        if (formDataSnapshot != null) {
            try {
                var formData = JSONUtil.parseObj(formDataSnapshot);
                for (String key : formData.keySet()) {
                    Object value = formData.get(key);
                    if (value != null) {
                        result = result.replace("${" + key + "}", String.valueOf(value));
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse formDataSnapshot for variable replacement", e);
            }
        }

        return result;
    }

    /**
     * 从响应中提取用户ID列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> extractUserIds(String responseBody, String resultPath) {
        try {
            var json = JSONUtil.parseObj(responseBody);

            // 简单路径解析 ($.data.approvers)
            Object target = json;
            if (resultPath != null && resultPath.startsWith("$.")) {
                String[] paths = resultPath.substring(2).split("\\.");
                for (String path : paths) {
                    if (path.contains("[*]")) {
                        // 数组通配符
                        String arrayPath = path.replace("[*]", "");
                        target = ((Map<String, Object>) target).get(arrayPath);
                        break;
                    }
                    if (target instanceof Map) {
                        target = ((Map<String, Object>) target).get(path);
                    }
                }
            }

            if (target instanceof List) {
                return ((List<?>) target).stream()
                        .map(item -> {
                            if (item instanceof Number) return ((Number) item).longValue();
                            if (item instanceof Map) {
                                Object id = ((Map<?, ?>) item).get("id");
                                if (id instanceof Number) return ((Number) id).longValue();
                            }
                            return null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
            }

            if (target instanceof Number) {
                return Collections.singletonList(((Number) target).longValue());
            }

            log.warn("ApiResolver: cannot extract user IDs from response at path {}", resultPath);
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("ApiResolver: error parsing response", e);
            return Collections.emptyList();
        }
    }
}
