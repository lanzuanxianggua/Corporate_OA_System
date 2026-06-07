package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmSealCreateDTO;
import cn.oa.admin.dto.AdmSealUpdateDTO;
import cn.oa.admin.service.AdmSealService;
import cn.oa.admin.vo.AdmSealVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdmSealControllerContractTest {

    private final AdmSealService service = mock(AdmSealService.class);
    private final AdmSealController controller = new AdmSealController(service);

    @Test
    void controllerMapping_matchesV2Contract() {
        RequestMapping mapping = AdmSealController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/v1/admin/seals");
    }

    @Test
    void methodPermissions_matchV2Contract() throws Exception {
        assertPermission("create", "admin:seal:create", AdmSealCreateDTO.class);
        assertPermission("update", "admin:seal:update", Long.class, AdmSealUpdateDTO.class);
        assertPermission("delete", "admin:seal:delete", Long.class);
        assertPermission("getById", "admin:seal:view", Long.class);
        assertPermission("list", "admin:seal:list", Long.class, int.class, int.class);
    }

    @Test
    void httpMappings_matchV2Contract() throws Exception {
        assertThat(method("create", AdmSealCreateDTO.class).getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(method("update", Long.class, AdmSealUpdateDTO.class).getAnnotation(PutMapping.class).value()).containsExactly("/{id}");
        assertThat(method("delete", Long.class).getAnnotation(DeleteMapping.class).value()).containsExactly("/{id}");
        assertThat(method("getById", Long.class).getAnnotation(GetMapping.class).value()).containsExactly("/{id}");
        assertThat(method("list", Long.class, int.class, int.class).getAnnotation(GetMapping.class).value()).isEmpty();
    }

    @Test
    void create_wrapsServiceResult() {
        AdmSealCreateDTO dto = new AdmSealCreateDTO();
        when(service.create(dto)).thenReturn(100L);

        R<Long> response = controller.create(dto);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(100L);
    }

    @Test
    void getById_wrapsVo() {
        AdmSealVO vo = new AdmSealVO();
        vo.setId(1L);
        when(service.getById(1L)).thenReturn(vo);

        R<AdmSealVO> response = controller.getById(1L);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(vo);
    }

    @Test
    void list_wrapsServiceResult() {
        Map<String, Object> page = Map.of("total", 0L);
        when(service.list(10L, 1, 20)).thenReturn(page);

        R<Map<String, Object>> response = controller.list(10L, 1, 20);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(page);
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes) throws Exception {
        RequirePermission annotation = method(methodName, parameterTypes).getAnnotation(RequirePermission.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private static Method method(String methodName, Class<?>... parameterTypes) throws Exception {
        return AdmSealController.class.getMethod(methodName, parameterTypes);
    }
}
