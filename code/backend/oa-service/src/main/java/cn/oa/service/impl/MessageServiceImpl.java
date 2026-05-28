package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaMessage;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaMessageMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<OaMessageMapper, OaMessage> implements MessageService {

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    @Transactional
    public void send(OaMessage message) {
        if (message.getReceiverId() == null) {
            throw new BusinessException("接收人不能为空");
        }
        if (message.getContent() == null || message.getContent().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        message.setIsRead(0);
        this.save(message);
    }

    @Override
    @Transactional
    public void markAsRead(Long msgId, Long empId) {
        LambdaUpdateWrapper<OaMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OaMessage::getId, msgId)
                .eq(OaMessage::getReceiverId, empId)
                .set(OaMessage::getIsRead, 1);
        boolean ok = this.update(wrapper);
        if (!ok) {
            throw new BusinessException("消息不存在或无权操作");
        }
    }

    @Override
    public Long getUnreadCount(Long empId) {
        LambdaQueryWrapper<OaMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaMessage::getReceiverId, empId)
                .eq(OaMessage::getIsRead, 0);
        return this.count(wrapper);
    }

    @Override
    public IPage<OaMessage> pageList(int pageNum, int pageSize, Long receiverId) {
        Page<OaMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaMessage> wrapper = new LambdaQueryWrapper<>();
        if (receiverId != null) {
            wrapper.eq(OaMessage::getReceiverId, receiverId);
        }
        wrapper.orderByDesc(OaMessage::getCreateTime);
        IPage<OaMessage> result = this.page(page, wrapper);

        // 填充 senderName
        List<Long> senderIds = result.getRecords().stream()
                .map(OaMessage::getSenderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!senderIds.isEmpty()) {
            List<SysEmployee> senders = employeeMapper.selectBatchIds(senderIds);
            Map<Long, String> senderMap = senders.stream()
                    .collect(Collectors.toMap(SysEmployee::getId, e -> e.getEmpName() != null ? e.getEmpName() : "", (a, b) -> a));
            result.getRecords().forEach(m -> {
                if (m.getSenderId() != null) {
                    m.setSenderName(senderMap.getOrDefault(m.getSenderId(), ""));
                }
            });
        }
        return result;
    }
}
