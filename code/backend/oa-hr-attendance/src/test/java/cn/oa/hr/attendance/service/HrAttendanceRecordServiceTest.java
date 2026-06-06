package cn.oa.hr.attendance.service;

import cn.oa.hr.attendance.dto.HrAttendanceRecordCreateDTO;
import cn.oa.hr.attendance.entity.HrAttendanceRecord;
import cn.oa.hr.attendance.mapper.HrAttendanceRecordMapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * HrAttendanceRecordService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class HrAttendanceRecordServiceTest {

    @Mock
    private HrAttendanceRecordMapper mapper;

    @Captor
    private ArgumentCaptor<HrAttendanceRecord> recordCaptor;

    private HrAttendanceRecordService service;

    private static final Long EMP_ID = 42L;

    @BeforeEach
    void setUp() {
        service = new HrAttendanceRecordService(mapper);
    }

    @Nested
    @DisplayName("clockIn() 签到")
    class ClockIn {

        @Test
        @DisplayName("签到成功 — 创建新记录")
        void clockIn_success() {
            // given
            HrAttendanceRecordCreateDTO dto = new HrAttendanceRecordCreateDTO();
            dto.setClockDate(LocalDate.of(2026, 6, 5));
            dto.setClockTime(LocalDateTime.of(2026, 6, 5, 9, 0));
            dto.setMethod("GPS");

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(mapper.insert(any(HrAttendanceRecord.class))).thenAnswer(invocation -> {
                HrAttendanceRecord r = invocation.getArgument(0);
                r.setId(100L);
                return 1;
            });

            // when
            Long id = service.clockIn(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).selectOne(any(LambdaQueryWrapper.class));
            verify(mapper).insert(recordCaptor.capture());

            HrAttendanceRecord saved = recordCaptor.getValue();
            assertThat(saved.getEmpId()).isEqualTo(EMP_ID);
            assertThat(saved.getClockDate()).isEqualTo(LocalDate.of(2026, 6, 5));
            assertThat(saved.getClockInTime()).isEqualTo(LocalDateTime.of(2026, 6, 5, 9, 0));
            assertThat(saved.getClockInMethod()).isEqualTo("GPS");
        }

        @Test
        @DisplayName("重复签到抛出 BizException")
        void clockIn_duplicate_throwsException() {
            // given
            HrAttendanceRecordCreateDTO dto = new HrAttendanceRecordCreateDTO();
            dto.setClockDate(LocalDate.of(2026, 6, 5));
            dto.setMethod("WIFI");

            HrAttendanceRecord exist = new HrAttendanceRecord();
            exist.setId(1L);
            exist.setEmpId(EMP_ID);
            exist.setClockDate(LocalDate.of(2026, 6, 5));
            exist.setClockInTime(LocalDateTime.of(2026, 6, 5, 8, 55));

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exist);

            // when & then
            assertThatThrownBy(() -> service.clockIn(dto, EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessage("今日已打卡")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.BAD_REQUEST.getCode()));

            verify(mapper).selectOne(any(LambdaQueryWrapper.class));
            verify(mapper, never()).insert(any(HrAttendanceRecord.class));
            verify(mapper, never()).updateById(any(HrAttendanceRecord.class));
        }

        @Test
        @DisplayName("签到成功 — 使用服务器时间（不传 clockTime）")
        void clockIn_usesServerTime() {
            // given
            HrAttendanceRecordCreateDTO dto = new HrAttendanceRecordCreateDTO();
            dto.setClockDate(LocalDate.of(2026, 6, 5));
            dto.setMethod("MANUAL");

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(mapper.insert(any(HrAttendanceRecord.class))).thenAnswer(invocation -> {
                HrAttendanceRecord r = invocation.getArgument(0);
                r.setId(101L);
                return 1;
            });

            // when
            Long id = service.clockIn(dto, EMP_ID);

            // then
            assertThat(id).isEqualTo(101L);
            verify(mapper).insert(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getClockInTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("clockOut() 签退")
    class ClockOut {

        @Test
        @DisplayName("签退成功")
        void clockOut_success() {
            // given
            HrAttendanceRecord record = new HrAttendanceRecord();
            record.setId(1L);
            record.setEmpId(EMP_ID);
            record.setClockInTime(LocalDateTime.of(2026, 6, 5, 9, 0));

            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

            // when
            Long id = service.clockOut(EMP_ID);

            // then
            assertThat(id).isEqualTo(1L);
            verify(mapper).selectOne(any(LambdaQueryWrapper.class));
            verify(mapper).updateById(recordCaptor.capture());

            HrAttendanceRecord updated = recordCaptor.getValue();
            assertThat(updated.getClockOutTime()).isNotNull();
        }

        @Test
        @DisplayName("未签到直接签退抛出 BizException")
        void clockOut_withoutClockIn_throwsException() {
            // given
            when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.clockOut(EMP_ID))
                    .isInstanceOf(BizException.class)
                    .hasMessage("今日未签到")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.BAD_REQUEST.getCode()));

            verify(mapper, never()).updateById(any(HrAttendanceRecord.class));
        }
    }
}
