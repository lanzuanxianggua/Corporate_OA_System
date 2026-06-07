package cn.oa.message.service;

import cn.oa.message.entity.MsgNotificationType;
import cn.oa.message.mapper.MsgNotificationTypeMapper;
import cn.oa.message.vo.MsgNotificationTypeVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MsgNotificationTypeService 单元测试.
 */
@ExtendWith(MockitoExtension.class)
class MsgNotificationTypeServiceTest {

    @Mock
    private MsgNotificationTypeMapper typeMapper;

    private MsgNotificationTypeService service;

    @BeforeEach
    void setUp() {
        service = new MsgNotificationTypeService(typeMapper);
    }

    @Nested
    @DisplayName("listEnabled() 启用的类型")
    class ListEnabled {

        @Test
        @DisplayName("返回 enabled=1 的类型 VO 列表")
        void returnsEnabled() {
            MsgNotificationType t1 = new MsgNotificationType();
            t1.setCode("LEAVE_APPROVE");
            t1.setName("请假通过");
            t1.setEnabled(1);
            t1.setSortOrder(10);

            MsgNotificationType t2 = new MsgNotificationType();
            t2.setCode("GENERAL");
            t2.setName("通用");
            t2.setEnabled(1);
            t2.setSortOrder(99);

            when(typeMapper.selectList(any())).thenReturn(List.of(t1, t2));

            List<MsgNotificationTypeVO> result = service.listEnabled();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("LEAVE_APPROVE");
            assertThat(result.get(1).getCode()).isEqualTo("GENERAL");
        }

        @Test
        @DisplayName("空列表 — 返回空集合")
        void empty_returnsEmpty() {
            when(typeMapper.selectList(any())).thenReturn(List.of());

            List<MsgNotificationTypeVO> result = service.listEnabled();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("requireEnabled() 校验")
    class RequireEnabled {

        @Test
        @DisplayName("类型存在且 enabled=1 — 不抛异常")
        void enabled_ok() {
            MsgNotificationType t = new MsgNotificationType();
            t.setCode("LEAVE_APPROVE");
            t.setEnabled(1);
            when(typeMapper.selectOne(any())).thenReturn(t);

            service.requireEnabled("LEAVE_APPROVE");

            verify(typeMapper).selectOne(any());
        }

        @Test
        @DisplayName("类型不存在 — 抛 BizException")
        void notFound_throws() {
            when(typeMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> service.requireEnabled("UNKNOWN"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("通知类型不存在");
        }

        @Test
        @DisplayName("类型被禁用 — 抛 BizException")
        void disabled_throws() {
            MsgNotificationType t = new MsgNotificationType();
            t.setCode("X");
            t.setEnabled(0);
            when(typeMapper.selectOne(any())).thenReturn(t);

            assertThatThrownBy(() -> service.requireEnabled("X"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("未启用");
        }
    }

    @Nested
    @DisplayName("create() 新增")
    class Create {

        @Test
        @DisplayName("新增成功 — 默认 enabled=1, sortOrder=0")
        void create_ok() {
            when(typeMapper.selectOne(any())).thenReturn(null);  // 不存在
            doReturn(1).when(typeMapper).insert(any(MsgNotificationType.class));

            MsgNotificationType input = new MsgNotificationType();
            input.setCode("NEW_TYPE");
            input.setName("新类型");

            service.create(input);

            ArgumentCaptor<MsgNotificationType> cap = ArgumentCaptor.forClass(MsgNotificationType.class);
            verify(typeMapper).insert(cap.capture());
            assertThat(cap.getValue().getEnabled()).isEqualTo(1);
            assertThat(cap.getValue().getSortOrder()).isZero();
        }

        @Test
        @DisplayName("code 已存在 — 抛 BizException")
        void create_duplicateCode() {
            MsgNotificationType exist = new MsgNotificationType();
            exist.setCode("EXIST");
            when(typeMapper.selectOne(any())).thenReturn(exist);

            MsgNotificationType input = new MsgNotificationType();
            input.setCode("EXIST");

            assertThatThrownBy(() -> service.create(input))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("已存在");

            verify(typeMapper, never()).insert(any(MsgNotificationType.class));
        }

        @Test
        @DisplayName("code 为空 — 抛 BizException")
        void create_blankCode() {
            MsgNotificationType input = new MsgNotificationType();
            input.setCode("  ");

            assertThatThrownBy(() -> service.create(input))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("编码不能为空");

            verifyNoInteractions(typeMapper);
        }
    }

    @Nested
    @DisplayName("update() 更新")
    class Update {

        @Test
        @DisplayName("更新 name/description/enabled/sortOrder")
        void update_ok() {
            MsgNotificationType exist = new MsgNotificationType();
            exist.setId(1L);
            exist.setCode("X");
            exist.setName("old");
            exist.setEnabled(1);
            exist.setSortOrder(0);
            when(typeMapper.selectById(1L)).thenReturn(exist);
            doReturn(1).when(typeMapper).updateById(any(MsgNotificationType.class));

            MsgNotificationType patch = new MsgNotificationType();
            patch.setName("new");
            patch.setEnabled(0);
            patch.setSortOrder(50);

            service.update(1L, patch);

            ArgumentCaptor<MsgNotificationType> cap = ArgumentCaptor.forClass(MsgNotificationType.class);
            verify(typeMapper).updateById(cap.capture());
            assertThat(cap.getValue().getName()).isEqualTo("new");
            assertThat(cap.getValue().getEnabled()).isZero();
            assertThat(cap.getValue().getSortOrder()).isEqualTo(50);
        }

        @Test
        @DisplayName("id 不存在 — 抛 BizException")
        void update_notFound() {
            when(typeMapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.update(99L, new MsgNotificationType()))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining("不存在");

            verify(typeMapper, never()).updateById(any(MsgNotificationType.class));
        }
    }

    @Nested
    @DisplayName("delete() 删除")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void delete_ok() {
            MsgNotificationType exist = new MsgNotificationType();
            exist.setId(1L);
            exist.setCode("X");
            when(typeMapper.selectById(1L)).thenReturn(exist);

            service.delete(1L);

            verify(typeMapper).deleteById(1L);
        }

        @Test
        @DisplayName("id 不存在 — 抛 BizException")
        void delete_notFound() {
            when(typeMapper.selectById(99L)).thenReturn(null);

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(BizException.class);

            verify(typeMapper, never()).deleteById(any());
        }
    }
}
