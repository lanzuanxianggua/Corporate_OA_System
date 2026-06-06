package cn.oa.platform.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldStartAndRespondToPing() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity("http://localhost:" + port + "/api/ping", String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        JsonNode body = objectMapper.readTree(resp.getBody());
        assertThat(body.path("code").asInt()).isEqualTo(0);
        assertThat(body.path("data").path("status").asText()).isEqualTo("UP");
        assertThat(body.path("data").path("service").asText()).isEqualTo("oa-system");
    }

    @Test
    void shouldReturnUnauthorizedWhenProtectedApiHasNoToken() throws Exception {
        // v2 API 契约: 未登录访问受保护资源返回 HTTP 401 + RCode.UNAUTHORIZED.
        ResponseEntity<String> resp = rest.getForEntity("http://localhost:" + port + "/api/system/emps", String.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).isNotNull();
        assertThat(objectMapper.readTree(resp.getBody()).path("code").asInt()).isEqualTo(10001);
    }
}
