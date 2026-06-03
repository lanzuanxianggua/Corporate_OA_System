package cn.oa.message.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.oa.message.entity.MsgNotification;
import cn.oa.message.enums.MsgChannel;
import cn.oa.message.enums.MsgType;
import cn.oa.message.mapper.MsgNotificationMapper;
import cn.oa.message.service.MsgNotificationService;
import cn.oa.message.service.MsgUserPreferenceService;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.platform.core.base.PageResult;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MsgNotificationServiceImpl extends ServiceImpl<MsgNotificationMapper, MsgNotification> implements MsgNotificationService {

    private final MsgUserPreferenceService msgUserPreferenceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(String msgType, Long empId, String title, String content, Map<String, Object> params) {
        if (empId == null) {
            throw new BusinessException("接收人ID不能为空");
        }
        if (StrUtil.isBlank(msgType)) {
            throw new BusinessException("消息类型不能为空");
        }

        // 提取扩展参数
        String channel = extractStr(params, "channel", "SITE");
        Long bizId = extractLong(params, "bizId");
        String bizType = extractStr(params, "bizType", null);

        // 检查用户偏好，过滤已禁用的渠道
        boolean enabled = msgUserPreferenceService.isEnabled(empId, msgType, channel);
        if (!enabled) {
            log.debug("用户 {} 已禁用消息类型 {} 的 {} 渠道", empId, msgType, channel);
            return;
        }

        MsgNotification notification = new MsgNotification();
        notification.setEmpId(empId);
        notification.setMsgType(msgType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setChannel(channel);
        notification.setIsRead(0);
        notification.setBizId(bizId);
        notification.setBizType(bizType);
        notification.setCreatedAt(LocalDateTime.now());

        save(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMulti(String msgType, List<Long> empIds, String title, String content, Map<String, Object> params) {
        if (empIds == null || empIds.isEmpty()) {
            return;
        }
        for (Long empId : empIds) {
            send(msgType, empId, title, content, params);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        MsgNotification entity = getById(id);
        if (entity == null) {
            throw new BusinessException("消息不存在");
        }
        if (Integer.valueOf(1).equals(entity.getIsRead())) {
            return;
        }
        MsgNotification update = new MsgNotification();
        update.setId(id);
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long empId) {
        LambdaQueryWrapper<MsgNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgNotification::getEmpId, empId)
                .eq(MsgNotification::getIsRead, 0);
        MsgNotification update = new MsgNotification();
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        update(update, wrapper);
    }

    @Override
    public long getUnreadCount(Long empId) {
        return count(new LambdaQueryWrapper<MsgNotification>()
                .eq(MsgNotification::getEmpId, empId)
                .eq(MsgNotification::getIsRead, 0));
    }

    @Override
    public PageResult<MsgNotificationVO> pageQuery(Long empId, Integer pageNum, Integer pageSize) {
        Page<MsgNotification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MsgNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgNotification::getEmpId, empId);
        wrapper.orderByDesc(MsgNotification::getCreatedAt);

        IPage<MsgNotification> result = page(page, wrapper);
        List<MsgNotificationVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(result.getTotal(), records);
    }

    private MsgNotificationVO toVO(MsgNotification entity) {
        MsgNotificationVO vo = new MsgNotificationVO();
        vo.setId(entity.getId());
        vo.setEmpId(entity.getEmpId());
        vo.setMsgType(entity.getMsgType());
        vo.setMsgTypeName(getMsgTypeDisplayName(entity.getMsgType()));
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setChannel(entity.getChannel());
        vo.setChannelName(getChannelDisplayName(entity.getChannel()));
        vo.setIsRead(entity.getIsRead());
        vo.setReadTime(entity.getReadTime());
        vo.setBizId(entity.getBizId());
        vo.setBizType(entity.getBizType());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private String getMsgTypeDisplayName(String msgType) {
        if (msgType == null) return null;
        try {
            return MsgType.valueOf(msgType).getDisplayName();
        } catch (IllegalArgumentException e) {
            return msgType;
        }
    }

    private String getChannelDisplayName(String channel) {
        if (channel == null) return null;
        try {
            return MsgChannel.valueOf(channel).getDisplayName();
        } catch (IllegalArgumentException e) {
            return channel;
        }
    }

    private String extractStr(Map<String, Object> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object val = params.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private Long extractLong(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        Object val = params.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            return StrUtil.isBlank((String) val) ? null : Long.parseLong((String) val);
        }
        return null;
    }
}
