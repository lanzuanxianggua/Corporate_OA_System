package cn.oa.platform.security.interceptor;

import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.exception.AuthException;
import cn.oa.platform.common.exception.ForbiddenException;
import cn.oa.platform.security.annotation.RequirePermission;
import cn.oa.platform.security.annotation.RequireRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PermissionInterceptorTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ObjectMapper objectMapper;

    private PermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor(objectMapper);
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ---- test controllers ----

    @RequirePermission("system:admin")
    static class ClassLevelController {
        public void method() {}
    }

    static class MethodLevelController {
        @RequirePermission("hr:leave:approve")
        public void singlePerm() {}

        @RequirePermission(value = {"perm1", "perm2"}, logical = RequirePermission.Logical.OR)
        public void anyPerm() {}

        @RequirePermission({"perm1", "perm2"})
        public void allPerm() {}

        @RequireRole("ADMIN")
        public void singleRole() {}

        @RequireRole(value = {"ADMIN", "MANAGER"}, logical = RequirePermission.Logical.OR)
        public void anyRole() {}

        public void noAnnotation() {}
    }

    private HandlerMethod handlerMethod(Object bean, String methodName) throws Exception {
        return new HandlerMethod(bean, bean.getClass().getMethod(methodName));
    }

    @Test
    void shouldSkipWhenHandlerIsNotHandlerMethod() throws Exception {
        Object nonHandler = new Object();
        boolean result = interceptor.preHandle(request, response, nonHandler);
        assertThat(result).isTrue();
    }

    @Test
    void shouldSkipWhenMethodHasNoAnnotation() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of("system:admin")));

        Object controller = new MethodLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "noAnnotation"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldThrowAuthExceptionWhenUserNotLoggedIn() throws Exception {
        Object controller = new MethodLevelController();
        assertThatThrownBy(() -> interceptor.preHandle(request, response,
                handlerMethod(controller, "singlePerm")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void shouldAllowWhenUserHasRequiredPermissionAndMode() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of("perm1", "perm2")));

        Object controller = new MethodLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "allPerm"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldThrowForbiddenWhenUserMissingPermissionAndMode() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of("perm1")));

        Object controller = new MethodLevelController();
        assertThatThrownBy(() -> interceptor.preHandle(request, response,
                handlerMethod(controller, "allPerm")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("缺少权限");
    }

    @Test
    void shouldAllowWhenUserHasAnyPermissionOrMode() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of("perm1")));

        Object controller = new MethodLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "anyPerm"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldAllowWhenUserHasRequiredRole() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of()));

        Object controller = new MethodLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "singleRole"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldThrowForbiddenWhenUserMissingRoleAndMode() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "user", "User", 1L,
                "Dept", "ALL", List.of("USER"), List.of()));

        Object controller = new MethodLevelController();
        assertThatThrownBy(() -> interceptor.preHandle(request, response,
                handlerMethod(controller, "singleRole")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("缺少角色");
    }

    @Test
    void shouldAllowWhenUserHasAnyRoleOrMode() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "mgmt", "Mgmt", 1L,
                "Dept", "ALL", List.of("MANAGER"), List.of()));

        Object controller = new MethodLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "anyRole"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldRespectClassLevelAnnotation() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "admin", "Admin", 1L,
                "Dept", "ALL", List.of("ADMIN"), List.of("system:admin")));

        Object controller = new ClassLevelController();
        boolean result = interceptor.preHandle(request, response,
                handlerMethod(controller, "method"));
        assertThat(result).isTrue();
    }

    @Test
    void shouldThrowForbiddenWhenClassLevelPermissionMissing() throws Exception {
        UserContext.set(new UserContext.UserInfo(1L, "user", "User", 1L,
                "Dept", "ALL", List.of("USER"), List.of("hr:leave:list")));

        Object controller = new ClassLevelController();
        assertThatThrownBy(() -> interceptor.preHandle(request, response,
                handlerMethod(controller, "method")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("缺少权限");
    }
}
