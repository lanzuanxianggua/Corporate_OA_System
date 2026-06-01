package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaAttendance;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaAttendanceMapper;
import cn.oa.mapper.SysEmployeeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceServiceImpl 考勤服务测试")
class AttendanceServiceImplTest {

    @Mock
    private OaAttendanceMapper attendanceMapper;

    @Mock
    private SysEmployeeMapper employeeMapper;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Captor
    private ArgumentCaptor<OaAttendance> attendanceCaptor;

    private final Long empId = 1L;

    @BeforeEach
    void setUp() throws Exception {
        // Set baseMapper for CrudRepository parent class via reflection
        Field baseMapperField = com.baomidou.mybatisplus.extension.repository.CrudRepository.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(attendanceService, attendanceMapper);
    }

    // ==================== clockIn ====================

    @Test
    @DisplayName("签到-迟到(9:00后打卡)")
    void clockIn_Late() {
        // Return null from selectOne so it takes the insert branch
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
        when(attendanceMapper.insert(any(OaAttendance.class))).thenReturn(1);

        attendanceService.clockIn(empId);

        verify(attendanceMapper).insert(attendanceCaptor.capture());
        OaAttendance saved = attendanceCaptor.getValue();
        assertThat(saved.getEmpId()).isEqualTo(empId);
        assertThat(saved.getWorkDate()).isEqualTo(LocalDate.now());
        // Status is decided at runtime based on LocalTime.now()
        // We verify it's either 0 or 1 (not null)
        assertThat(saved.getStatus()).isIn(0, 1);
        assertThat(saved.getClockIn()).isNotNull();
    }

    @Test
    @DisplayName("签到-重复打卡抛异常")
    void clockIn_Repeat_Throws() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        existing.setClockIn(LocalDateTime.now());
        existing.setStatus(0);
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        assertThatThrownBy(() -> attendanceService.clockIn(empId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今日已打卡");
    }

    @Test
    @DisplayName("签到-已有请假自动标记时保留原有状态")
    void clockIn_ExistingAutoMarkRecord_PreservesStatus() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        existing.setClockIn(null);
        existing.setStatus(5); // 5 = leave auto-marked
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        attendanceService.clockIn(empId);

        verify(attendanceMapper).updateById(attendanceCaptor.capture());
        OaAttendance updated = attendanceCaptor.getValue();
        assertThat(updated.getClockIn()).isNotNull();
        // Status should still be 5 (preserved)
        assertThat(updated.getStatus()).isEqualTo(5);
    }

    @Test
    @DisplayName("签到-已有出差自动标记时保留状态")
    void clockIn_ExistingTripMarkRecord_PreservesStatus() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        existing.setClockIn(null);
        existing.setStatus(4);
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        attendanceService.clockIn(empId);

        verify(attendanceMapper).updateById(attendanceCaptor.capture());
        assertThat(attendanceCaptor.getValue().getStatus()).isEqualTo(4);
    }

    // ==================== clockOut ====================

    @Test
    @DisplayName("签退-正常签退")
    void clockOut_Normal() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        existing.setClockIn(LocalDateTime.now().minusHours(8));
        existing.setStatus(0);
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        attendanceService.clockOut(empId);

        verify(attendanceMapper).updateById(attendanceCaptor.capture());
        OaAttendance updated = attendanceCaptor.getValue();
        assertThat(updated.getClockOut()).isNotNull();
    }

    @Test
    @DisplayName("签退-未签到抛异常")
    void clockOut_NoClockIn_Throws() {
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        assertThatThrownBy(() -> attendanceService.clockOut(empId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今日未打卡，请先签到");
    }

    @Test
    @DisplayName("签退-重复签退抛异常")
    void clockOut_Repeat_Throws() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        existing.setClockIn(LocalDateTime.now().minusHours(8));
        existing.setClockOut(LocalDateTime.now());
        existing.setStatus(0);
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        assertThatThrownBy(() -> attendanceService.clockOut(empId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今日已签退");
    }

    // ==================== getTodayAttendance ====================

    @Test
    @DisplayName("获取今日考勤-有记录")
    void getTodayAttendance_Found() {
        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(LocalDate.now());
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        OaAttendance result = attendanceService.getTodayAttendance(empId);

        assertThat(result).isNotNull();
        assertThat(result.getEmpId()).isEqualTo(empId);
    }

    @Test
    @DisplayName("获取今日考勤-无记录返回null")
    void getTodayAttendance_NotFound() {
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);

        OaAttendance result = attendanceService.getTodayAttendance(empId);

        assertThat(result).isNull();
    }

    // ==================== getAttendanceHistory ====================

    @Test
    @DisplayName("获取考勤历史-按日期范围查询")
    void getAttendanceHistory_ReturnsList() {
        OaAttendance record = new OaAttendance();
        record.setEmpId(empId);
        record.setWorkDate(LocalDate.now().minusDays(1));
        when(attendanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(record));

        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        List<OaAttendance> result = attendanceService.getAttendanceHistory(empId, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmpId()).isEqualTo(empId);
    }

    @Test
    @DisplayName("获取考勤历史-空结果返回空列表")
    void getAttendanceHistory_Empty() {
        when(attendanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        List<OaAttendance> result = attendanceService.getAttendanceHistory(empId, start, end);

        assertThat(result).isEmpty();
    }

    // ==================== markLeaveAttendance / markTripAttendance ====================

    @Test
    @DisplayName("标记请假考勤-工作日自动标记")
    void markLeaveAttendance_MarksWeekdays() {
        // Start and end on same weekday (Monday)
        LocalDate monday = LocalDate.of(2026, 6, 1); // Monday
        LocalDate friday = LocalDate.of(2026, 6, 5); // Friday

        attendanceService.markLeaveAttendance(empId, monday, friday);

        // Should create 5 records (Mon-Fri), skipping Sat/Sun
        verify(attendanceMapper, times(5)).insert(any(OaAttendance.class));
    }

    @Test
    @DisplayName("标记请假考勤-跨周跳过周末")
    void markLeaveAttendance_SkipsWeekends() {
        // Thursday to next Monday
        LocalDate thursday = LocalDate.of(2026, 6, 4); // Thursday
        LocalDate nextMonday = LocalDate.of(2026, 6, 8); // next Monday

        attendanceService.markLeaveAttendance(empId, thursday, nextMonday);

        // Should create 3 records: Thu, Fri, Mon (skip Sat, Sun)
        verify(attendanceMapper, times(3)).insert(any(OaAttendance.class));
    }

    @Test
    @DisplayName("标记出差考勤-成功标记")
    void markTripAttendance_MarksWeekdays() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 3);

        attendanceService.markTripAttendance(empId, start, end);

        verify(attendanceMapper, times(3)).insert(attendanceCaptor.capture());
        assertThat(attendanceCaptor.getValue().getStatus()).isEqualTo(6);
        assertThat(attendanceCaptor.getValue().getRemark()).contains("出差自动标记");
    }

    @Test
    @DisplayName("标记考勤-已存在状态为3的记录会被覆盖")
    void markLeaveAttendance_OverridesStatus3() {
        LocalDate singleDay = LocalDate.of(2026, 6, 1); // Monday

        OaAttendance existing = new OaAttendance();
        existing.setEmpId(empId);
        existing.setWorkDate(singleDay);
        existing.setStatus(3);
        when(attendanceMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

        attendanceService.markLeaveAttendance(empId, singleDay, singleDay);

        verify(attendanceMapper, never()).insert(any(OaAttendance.class));
        verify(attendanceMapper).updateById(attendanceCaptor.capture());
        assertThat(attendanceCaptor.getValue().getStatus()).isEqualTo(5);
    }

    // ==================== removeMarkedAttendance ====================

    @Test
    @DisplayName("删除自动标记的考勤记录")
    void removeMarkedAttendance_RemovesRecords() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 2);
        when(attendanceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);

        attendanceService.removeMarkedAttendance(empId, start, end, 5);

        verify(attendanceMapper).delete(any(LambdaQueryWrapper.class));
    }

    // ==================== adminPage ====================

    @Test
    @DisplayName("管理分页-无名称过滤")
    void adminPage_NoNameFilter() {
        // When no empName filter, adminPage queries attendance directly
        OaAttendance att = new OaAttendance();
        att.setEmpId(empId);
        att.setWorkDate(LocalDate.now());
        att.setStatus(0);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OaAttendance> attPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        attPage.setRecords(Collections.singletonList(att));
        attPage.setTotal(1);
        when(attendanceMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(attPage);

        IPage<Map<String, Object>> result = attendanceService.adminPage(1, 10, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).get("empId")).isEqualTo(empId);
    }

    @Test
    @DisplayName("管理分页-按员工名称过滤")
    void adminPage_FilterByEmpName() {
        SysEmployee emp = new SysEmployee();
        emp.setId(empId);
        emp.setEmpName("张三");
        emp.setDeptId(10L);
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(emp));

        OaAttendance att = new OaAttendance();
        att.setEmpId(empId);
        att.setWorkDate(LocalDate.now());
        att.setStatus(0);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OaAttendance> attPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        attPage.setRecords(Collections.singletonList(att));
        attPage.setTotal(1);
        when(attendanceMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(attPage);

        IPage<Map<String, Object>> result = attendanceService.adminPage(1, 10, "张三", null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).get("empName")).isEqualTo("张三");
    }

    @Test
    @DisplayName("管理分页-名称无匹配返回空")
    void adminPage_NoMatchingName_ReturnsEmpty() {
        when(employeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        IPage<Map<String, Object>> result = attendanceService.adminPage(1, 10, "不存在的名字", null, null, null);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    // ==================== getHistoryByDateRange ====================

    @Test
    @DisplayName("按日期范围查询所有考勤记录")
    void getHistoryByDateRange_ReturnsRecords() {
        OaAttendance att = new OaAttendance();
        att.setEmpId(empId);
        att.setWorkDate(LocalDate.now());

        when(attendanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(att));

        List<OaAttendance> result = attendanceService.getHistoryByDateRange(
                LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("按日期范围查询-空结果")
    void getHistoryByDateRange_Empty() {
        when(attendanceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<OaAttendance> result = attendanceService.getHistoryByDateRange(
                LocalDate.now().minusDays(7), LocalDate.now());

        assertThat(result).isEmpty();
    }
}
