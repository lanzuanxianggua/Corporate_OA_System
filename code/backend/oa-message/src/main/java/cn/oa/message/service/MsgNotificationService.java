package cn.oa.message.service;

import cn.oa.message.dto.MsgSendDTO;
import cn.oa.message.dto.MsgNotificationQueryDTO;
import cn.oa.message.entity.MsgNotification;
import cn.oa.message.entity.MsgNotificationRecipient;
import cn.oa.message.mapper.MsgNotificationMapper;
import cn.oa.message.mapper.MsgNotificationRecipientMapper;
import cn.oa.message.vo.MsgNotificationVO;
import cn.oa.message.vo.MsgUnreadCountVO;
import java.util.Map;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor
public class MsgNotificationService {
    private final MsgNotificationMapper mapper;
    private final MsgNotificationRecipientMapper recipientMapper;

    @Transactional
    public void send(MsgSendDTO dto, Long senderId) {
        for (Long recipientId : dto.getRecipientIds()) {
            MsgNotification notif = new MsgNotification();
            notif.setTitle(dto.getTitle());
            notif.setContent(dto.getContent());
            notif.setType(dto.getType());
            notif.setSenderId(senderId);
            notif.setRecipientId(recipientId);
            notif.setStatus("UNREAD");
            notif.setDelFlag("0");
            mapper.insert(notif);
        }
        log.info("消息发送成功: title={}, recipients={}", dto.getTitle(), dto.getRecipientIds().size());
    }

    public List<MsgNotificationVO> listByRecipient(Long recipientId, MsgNotificationQueryDTO query) {
        List<MsgNotification> list = mapper.findByRecipientId(recipientId, query.getPageSize());
        return list.stream().map(n -> {
            MsgNotificationVO vo = new MsgNotificationVO();
            vo.setId(n.getId()); vo.setTitle(n.getTitle()); vo.setContent(n.getContent());
            vo.setType(n.getType()); vo.setSenderId(n.getSenderId());
            vo.setStatus(n.getStatus()); vo.setReadTime(n.getReadTime()); vo.setCreateTime(n.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    public MsgUnreadCountVO countUnread(Long recipientId) {
        long count = mapper.countUnread(recipientId);
        return new MsgUnreadCountVO(count);
    }

    @Transactional
    public void markRead(Long id, Long recipientId) {
        MsgNotification notif = mapper.selectById(id);
        if (notif == null) throw new BizException(RCode.NOT_FOUND, "消息不存在");
        notif.setStatus("READ");
        notif.setReadTime(LocalDateTime.now());
        mapper.updateById(notif);
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        recipientMapper.update(null, new LambdaUpdateWrapper<MsgNotificationRecipient>()
                .eq(MsgNotificationRecipient::getRecipientId, recipientId)
                .set(MsgNotificationRecipient::getIsRead, "Y"));
    }

    public MsgNotificationVO getById(Long id) {
        List<Map<String, Object>> list = mapper.findDetailByRecipientId(id, 1);
        if (list.isEmpty()) throw new BizException(RCode.NOT_FOUND, "消息不存在");
        Map<String, Object> m = list.get(0);
        MsgNotificationVO vo = new MsgNotificationVO();
        vo.setId((Long) m.get("id")); vo.setTitle((String) m.get("title"));
        vo.setContent((String) m.get("content")); vo.setSenderName((String) m.get("senderName"));
        return vo;
    }
}