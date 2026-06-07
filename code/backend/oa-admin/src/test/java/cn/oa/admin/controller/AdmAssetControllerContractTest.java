package cn.oa.admin.controller;

import cn.oa.admin.dto.AdmAssetCreateDTO;
import cn.oa.admin.dto.AdmAssetUpdateDTO;
import cn.oa.admin.service.AdmAssetService;
import cn.oa.admin.vo.AdmAssetVO;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdmAssetControllerContractTest {

    private final AdmAssetService service = mock(AdmAssetService.class);
    private final AdmAssetController controller = new AdmAssetController(service);

    @Test
    void controllerMapping_matchesV2Contract() {
        RequestMapping mapping = AdmAssetController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/v1/admin/assets");
    }

    @Test
    void methodPermissions_matchV2Contract() throws Exception {
        assertPermission("create", "admin:asset:create", AdmAssetCreateDTO.class);
        assertPermission("update", "admin:asset:update", Long.class, AdmAssetUpdateDTO.class);
        assertPermission("delete", "admin:asset:delete", Long.class);
        assertPermission("getById", "admin:asset:view", Long.class);
        assertPermission("list", "admin:asset:list", String.class, String.class, int.class, int.class);
    }

    @Test
    void httpMappings_matchV2Contract() throws Exception {
        assertThat(method("create", AdmAssetCreateDTO.class).getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(method("update", Long.class, AdmAssetUpdateDTO.class).getAnnotation(PutMapping.class).value()).containsExactly("/{id}");
        assertThat(method("delete", Long.class).getAnnotation(DeleteMapping.class).value()).containsExactly("/{id}");
        assertThat(method("getById", Long.class).getAnnotation(GetMapping.class).value()).containsExactly("/{id}");
        assertThat(method("list", String.class, String.class, int.class, int.class).getAnnotation(GetMapping.class).value()).isEmpty();
    }

    @Test
    void create_wrapsServiceResult() {
        AdmAssetCreateDTO dto = new AdmAssetCreateDTO();
        when(service.create(dto)).thenReturn(100L);

        R<Long> response = controller.create(dto);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(100L);
    }

    @Test
    void getById_wrapsVo() {
        AdmAssetVO vo = new AdmAssetVO();
        vo.setId(1L);
        when(service.getById(1L)).thenReturn(vo);

        R<AdmAssetVO> response = controller.getById(1L);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(vo);
    }

    @Test
    void list_wrapsServiceResult() {
        Map<String, Object> page = Map.of("total", 0L);
        when(service.list("IT", "IDLE", 1, 20)).thenReturn(page);

        R<Map<String, Object>> response = controller.list("IT", "IDLE", 1, 20);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(page);
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes) throws Exception {
        RequirePermission annotation = method(methodName, parameterTypes).getAnnotation(RequirePermission.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private static Method method(String methodName, Class<?>... parameterTypes) throws Exception {
        return AdmAssetController.class.getMethod(methodName, parameterTypes);
    }
}
