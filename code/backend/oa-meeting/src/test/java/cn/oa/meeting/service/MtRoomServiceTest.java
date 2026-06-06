package cn.oa.meeting.service;

import cn.oa.meeting.dto.MtRoomCreateDTO;
import cn.oa.meeting.entity.MtRoom;
import cn.oa.meeting.mapper.MtRoomMapper;
import cn.oa.meeting.vo.MtRoomVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MtRoomService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class MtRoomServiceTest {

    @Mock
    private MtRoomMapper mapper;

    @Captor
    private ArgumentCaptor<MtRoom> roomCaptor;

    private MtRoomService service;

    @BeforeEach
    void setUp() {
        service = new MtRoomService(mapper);
    }

    @Nested
    @DisplayName("create() 创建会议室")
    class Create {

        @Test
        @DisplayName("创建成功 — 返回房间ID, 默认状态 ACTIVE")
        void create_success() {
            // given
            MtRoomCreateDTO dto = new MtRoomCreateDTO();
            dto.setRoomName("第一会议室");
            dto.setRoomCode("ROOM-001");
            dto.setFloor("3F");
            dto.setCapacity(20);
            dto.setFacility("{\"projector\":true}");
            dto.setLocation("A栋3楼");

            when(mapper.insert(any(MtRoom.class))).thenAnswer(invocation -> {
                MtRoom room = invocation.getArgument(0);
                room.setId(100L);
                return 1;
            });

            // when
            Long id = service.create(dto);

            // then
            assertThat(id).isEqualTo(100L);
            verify(mapper).insert(roomCaptor.capture());

            MtRoom saved = roomCaptor.getValue();
            assertThat(saved.getRoomName()).isEqualTo("第一会议室");
            assertThat(saved.getRoomCode()).isEqualTo("ROOM-001");
            assertThat(saved.getFloor()).isEqualTo("3F");
            assertThat(saved.getCapacity()).isEqualTo(20);
            assertThat(saved.getFacility()).isEqualTo("{\"projector\":true}");
            assertThat(saved.getLocation()).isEqualTo("A栋3楼");
            assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("update() 更新会议室")
    class Update {

        @Test
        @DisplayName("更新成功")
        void update_success() {
            // given
            MtRoom exist = new MtRoom();
            exist.setId(1L);
            exist.setRoomName("旧会议室");
            exist.setRoomCode("ROOM-001");

            MtRoomCreateDTO dto = new MtRoomCreateDTO();
            dto.setRoomName("新会议室");
            dto.setRoomCode("ROOM-002");
            dto.setFloor("5F");
            dto.setCapacity(30);
            dto.setFacility("{\"video_conf\":true}");
            dto.setLocation("B栋5楼");

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.update(1L, dto);

            // then
            verify(mapper).selectById(1L);
            verify(mapper).updateById(roomCaptor.capture());

            MtRoom patch = roomCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getRoomName()).isEqualTo("新会议室");
            assertThat(patch.getRoomCode()).isEqualTo("ROOM-002");
            assertThat(patch.getFloor()).isEqualTo("5F");
            assertThat(patch.getCapacity()).isEqualTo(30);
            assertThat(patch.getFacility()).isEqualTo("{\"video_conf\":true}");
            assertThat(patch.getLocation()).isEqualTo("B栋5楼");
        }

        @Test
        @DisplayName("会议室不存在时抛出 BizException")
        void update_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.update(999L, new MtRoomCreateDTO()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("会议室不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).updateById(any(MtRoom.class));
        }
    }

    @Nested
    @DisplayName("delete() 软删除会议室")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_success() {
            // given
            MtRoom exist = new MtRoom();
            exist.setId(1L);

            when(mapper.selectById(1L)).thenReturn(exist);

            // when
            service.delete(1L);

            // then
            verify(mapper).selectById(1L);
            verify(mapper).deleteById(1L);
        }

        @Test
        @DisplayName("会议室不存在时抛出 BizException")
        void delete_notFound_throwsException() {
            // given
            when(mapper.selectById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("会议室不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));

            verify(mapper, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("getById() 查询会议室详情")
    class GetById {

        @Test
        @DisplayName("查询成功")
        void getById_success() {
            // given
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", 1L);
            detail.put("room_name", "第一会议室");
            detail.put("room_code", "ROOM-001");
            detail.put("floor", "3F");
            detail.put("capacity", 20);
            detail.put("status", "ACTIVE");
            detail.put("today_bookings", 3);

            when(mapper.selectDetailById(1L)).thenReturn(detail);

            // when
            MtRoomVO vo = service.getById(1L);

            // then
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getRoomName()).isEqualTo("第一会议室");
            assertThat(vo.getRoomCode()).isEqualTo("ROOM-001");
            assertThat(vo.getFloor()).isEqualTo("3F");
            assertThat(vo.getCapacity()).isEqualTo(20);
            assertThat(vo.getStatus()).isEqualTo("ACTIVE");
            assertThat(vo.getTodayBookings()).isEqualTo(3);
        }

        @Test
        @DisplayName("会议室不存在时抛出 BizException")
        void getById_notFound_throwsException() {
            // given
            when(mapper.selectDetailById(999L)).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("会议室不存在")
                    .satisfies(e -> assertThat(((BizException) e).getCode()).isEqualTo(RCode.NOT_FOUND.getCode()));
        }
    }
}
