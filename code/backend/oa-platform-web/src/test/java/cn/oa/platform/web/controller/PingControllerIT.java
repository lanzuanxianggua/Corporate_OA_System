package cn.oa.platform.web.controller;

import cn.oa.platform.common.api.R;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PingController 纯单元测试.
 *
 * <p>不启动 Spring 容器, 直接调用 controller 方法.
 * 避免 @WebMvcTest 加载 MyBatis Mapper 的副作用.
 */
class PingControllerTest {

    private final PingController controller = new PingController();

    @Test
    void shouldReturnUpStatus() {
        R<Map<String, Object>> response = controller.ping();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().get("status")).isEqualTo("UP");
        assertThat(response.getData().get("service")).isEqualTo("oa-system");
        assertThat(response.getData().get("version")).isEqualTo("2.0.0-SNAPSHOT");
        assertThat(response.getData().get("timestamp")).isNotNull();
    }
}
