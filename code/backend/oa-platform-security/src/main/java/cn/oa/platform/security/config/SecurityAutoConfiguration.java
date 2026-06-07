package cn.oa.platform.security.config;

import cn.oa.platform.security.filter.JwtAuthenticationFilter;
import cn.oa.platform.security.interceptor.PermissionInterceptor;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.platform.security.password.BCryptPasswordEncoder;
import cn.oa.platform.security.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全模块自动配置.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private final SecurityProperties props;
    private final ObjectMapper objectMapper;

    public SecurityAutoConfiguration(SecurityProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtUtil jwtUtil) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, props.getJwt().getHeader());
        reg.setFilter(filter);
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public PermissionInterceptor permissionInterceptor() {
        return new PermissionInterceptor(objectMapper);
    }

    /**
     * 注册平台 BCrypt 密码编码器.
     *
     * <p>强度来自 {@code oa.security.bcrypt.strength} (默认 10).
     * 业务模块 (oa-system) 注入 {@link PasswordEncoder} 接口即可使用,
     * 避免直接依赖 spring-security-crypto.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(props.getBcrypt().getStrength());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/refresh", "/api/auth/captcha", "/api/public/**");
    }
}
