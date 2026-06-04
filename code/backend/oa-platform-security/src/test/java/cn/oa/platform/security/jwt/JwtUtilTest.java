package cn.oa.platform.security.jwt;

import cn.oa.platform.common.exception.BizException;
import cn.oa.platform.security.config.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityProperties props = new SecurityProperties();
        props.getJwt().setSecret("test-secret-key-12345678901234567890");
        props.getJwt().setAccessTtlSeconds(3600);
        props.getJwt().setRefreshTtlSeconds(86400);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    void shouldGenerateAndParseAccessToken() {
        String token = jwtUtil.generateAccessToken(1001L, "alice",
                List.of("ADMIN", "USER"), List.of("hr:leave:list", "system:user:list"));

        assertThat(token).isNotBlank();
        var info = jwtUtil.extract(jwtUtil.parse(token));

        assertThat(info.getEmpId()).isEqualTo(1001L);
        assertThat(info.getUsername()).isEqualTo("alice");
        assertThat(info.getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(info.getPermissions()).containsExactlyInAnyOrder("hr:leave:list", "system:user:list");
    }

    @Test
    void shouldGenerateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(2002L, "bob");
        var info = jwtUtil.extract(jwtUtil.parse(token));
        assertThat(info.getEmpId()).isEqualTo(2002L);
        assertThat(info.getRoles()).isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() {
        SecurityProperties props = new SecurityProperties();
        props.getJwt().setSecret("test-secret-key-12345678901234567890");
        props.getJwt().setAccessTtlSeconds(-10);
        JwtUtil shortJwt = new JwtUtil(props);

        String token = shortJwt.generateAccessToken(1L, "x", List.of(), List.of());

        assertThatThrownBy(() -> shortJwt.parse(token))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("过期");
    }

    @Test
    void shouldResolveTokenWithBearerPrefix() {
        assertThat(jwtUtil.resolveToken("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(jwtUtil.resolveToken("abc.def.ghi")).isEqualTo("abc.def.ghi");
        assertThat(jwtUtil.resolveToken(null)).isNull();
        assertThat(jwtUtil.resolveToken("")).isNull();
    }
}
