package cn.oa.controller;

import cn.oa.common.interceptor.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 测试基类 - 配置 Mock AuthInterceptor 以放行所有请求
 */
public abstract class BaseControllerTest {

    @MockitoBean
    protected AuthInterceptor authInterceptor;

    @BeforeEach
    void setupInterceptor() throws Exception {
        when(authInterceptor.preHandle(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(Object.class)
        )).thenReturn(true);
    }
}
