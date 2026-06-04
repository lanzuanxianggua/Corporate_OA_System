package cn.oa.platform.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RTest {

    @Test
    void shouldCreateSuccessResponse() {
        R<String> r = R.ok("hello");
        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getData()).isEqualTo("hello");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getTimestamp()).isPositive();
    }

    @Test
    void shouldCreateFailureResponse() {
        R<String> r = R.fail(RCode.NOT_FOUND, "user not found");
        assertThat(r.getCode()).isEqualTo(101);
        assertThat(r.getMessage()).isEqualTo("user not found");
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void shouldHandleNullData() {
        R<Object> r = R.ok();
        assertThat(r.getCode()).isEqualTo(0);
        assertThat(r.getData()).isNull();
    }
}
