package cn.oa.system.service;

import cn.oa.system.entity.SysEmp;
import cn.oa.system.entity.SysRole;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.system.mapper.SysEmpRoleMapper;
import cn.oa.system.mapper.SysRoleMapper;
import cn.oa.system.mapper.SysRolePermissionMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private SysEmpMapper empMapper;
    private SysEmpRoleMapper empRoleMapper;
    private SysRoleMapper roleMapper;
    private SysRolePermissionMapper rolePermMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        empMapper = mock(SysEmpMapper.class);
        empRoleMapper = mock(SysEmpRoleMapper.class);
        roleMapper = mock(SysRoleMapper.class);
        rolePermMapper = mock(SysRolePermissionMapper.class);
        authService = new AuthService(empMapper, empRoleMapper, roleMapper, rolePermMapper);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldFindByUsername() {
        SysEmp emp = new SysEmp();
        emp.setId(1L);
        emp.setUsername("alice");
        when(empMapper.selectOne(any(Wrapper.class))).thenReturn(emp);

        SysEmp found = authService.findByUsername("alice");

        assertThat(found).isNotNull();
        assertThat(found.getUsername()).isEqualTo("alice");
    }

    @Test
    void shouldReturnEmptyRolesWhenNoRoleMapping() {
        when(empRoleMapper.selectRoleIdsByEmpId(100L)).thenReturn(List.of());

        assertThat(authService.findRolesByEmpId(100L)).isEmpty();
        assertThat(authService.findPermCodesByEmpId(100L)).isEmpty();
    }

    @Test
    void shouldAggregateRolesAndPermissions() {
        when(empRoleMapper.selectRoleIdsByEmpId(1L)).thenReturn(List.of(10L, 20L));

        SysRole admin = new SysRole();
        admin.setId(10L);
        admin.setRoleCode("ADMIN");
        SysRole user = new SysRole();
        user.setId(20L);
        user.setRoleCode("USER");

        when(roleMapper.selectBatchIds(List.of(10L, 20L))).thenReturn(List.of(admin, user));
        when(rolePermMapper.selectPermCodesByRoleIds(anyList()))
                .thenReturn(List.of("hr:leave:list", "system:user:list"));

        assertThat(authService.findRolesByEmpId(1L))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(authService.findPermCodesByEmpId(1L))
                .containsExactlyInAnyOrder("hr:leave:list", "system:user:list");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRecordLoginWithIp() {
        SysEmp update = new SysEmp();
        authService.recordLogin(42L, "10.0.0.1");

        ArgumentCaptor<SysEmp> captor = ArgumentCaptor.forClass(SysEmp.class);
        verify(empMapper).updateById(captor.capture());
        SysEmp captured = captor.getValue();
        assertThat(captured.getId()).isEqualTo(42L);
        assertThat(captured.getLastLoginIp()).isEqualTo("10.0.0.1");
        assertThat(captured.getLastLoginTime()).isNotNull();
    }
}
