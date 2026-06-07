package cn.oa.hr.employee.controller;

import cn.oa.hr.employee.dto.HrEmployeeProfileCreateDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileQueryDTO;
import cn.oa.hr.employee.dto.HrEmployeeProfileUpdateDTO;
import cn.oa.hr.employee.service.HrEmployeeProfileService;
import cn.oa.hr.employee.vo.HrEmployeeProfileVO;
import cn.oa.platform.common.api.PageResult;
import cn.oa.platform.common.api.R;
import cn.oa.platform.security.annotation.RequirePermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HrEmployeeProfileControllerContractTest {

    private final HrEmployeeProfileService service = mock(HrEmployeeProfileService.class);
    private final HrEmployeeProfileController controller = new HrEmployeeProfileController(service);

    @Test
    void controllerMapping_matchesV2Contract() {
        RequestMapping mapping = HrEmployeeProfileController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/v1/hr/employees");
    }

    @Test
    void methodPermissions_matchV2Contract() throws Exception {
        assertPermission("create", "hr-employee:profile:create", HrEmployeeProfileCreateDTO.class);
        assertPermission("update", "hr-employee:profile:update", Long.class, HrEmployeeProfileUpdateDTO.class);
        assertPermission("delete", "hr-employee:profile:delete", Long.class);
        assertPermission("list", "hr-employee:profile:list", HrEmployeeProfileQueryDTO.class);
        assertPermission("get", "hr-employee:profile:view", Long.class);
    }

    @Test
    void httpMappings_matchV2Contract() throws Exception {
        assertThat(method("create", HrEmployeeProfileCreateDTO.class).getAnnotation(PostMapping.class).value()).isEmpty();
        assertThat(method("update", Long.class, HrEmployeeProfileUpdateDTO.class).getAnnotation(PutMapping.class).value()).containsExactly("/{id}");
        assertThat(method("delete", Long.class).getAnnotation(DeleteMapping.class).value()).containsExactly("/{id}");
        assertThat(method("list", HrEmployeeProfileQueryDTO.class).getAnnotation(GetMapping.class).value()).isEmpty();
        assertThat(method("get", Long.class).getAnnotation(GetMapping.class).value()).containsExactly("/{id}");
    }

    @Test
    void create_wrapsServiceResult() {
        HrEmployeeProfileCreateDTO dto = new HrEmployeeProfileCreateDTO();
        when(service.create(dto)).thenReturn(100L);

        R<Long> response = controller.create(dto);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(100L);
        verify(service).create(dto);
    }

    @Test
    void list_wrapsPageResult() {
        HrEmployeeProfileQueryDTO query = new HrEmployeeProfileQueryDTO();
        PageResult<HrEmployeeProfileVO> page = PageResult.of(List.of(new HrEmployeeProfileVO()), 1, 1, 10);
        when(service.listPage(query)).thenReturn(page);

        R<PageResult<HrEmployeeProfileVO>> response = controller.list(query);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isSameAs(page);
    }

    @Test
    void get_wrapsDetailMap() {
        Map<String, Object> detail = Map.of("id", 1L, "emp_name", "张三");
        when(service.getDetail(1L)).thenReturn(detail);

        R<Map<String, Object>> response = controller.get(1L);

        assertThat(response.getCode()).isZero();
        assertThat(response.getData()).isEqualTo(detail);
    }

    private static void assertPermission(String methodName, String permission, Class<?>... parameterTypes) throws Exception {
        RequirePermission annotation = method(methodName, parameterTypes).getAnnotation(RequirePermission.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(permission);
    }

    private static Method method(String methodName, Class<?>... parameterTypes) throws Exception {
        return HrEmployeeProfileController.class.getMethod(methodName, parameterTypes);
    }
}
