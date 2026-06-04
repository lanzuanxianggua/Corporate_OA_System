package cn.oa.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void shouldStartAndRespondToPing() {
        ResponseEntity<Map> resp = rest.getForEntity("http://localhost:" + port + "/api/ping", Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().get("code")).isEqualTo(0);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("UP");
        assertThat(data.get("service")).isEqualTo("oa-system");
    }

    @Test
    void shouldAllowGetEmpsWithoutAuthSincePermCheckIsInInterceptor() {
        // v2 设计: JwtAuthenticationFilter 仅解析 Token 写入 UserContext,
        // 未登录时不影响请求. 业务侧 @RequirePermission 拦截器才会拒绝.
        // 单元测试: 仅验证不抛异常即可 (业务层会返回 200 + 空列表).
        ResponseEntity<Map> resp = rest.getForEntity("http://localhost:" + port + "/api/system/emps", Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
