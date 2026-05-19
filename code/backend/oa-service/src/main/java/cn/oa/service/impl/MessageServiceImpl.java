package cn.oa.service.impl;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<OaMessageMapper, OaMessage> implements MessageService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    public void send(OaMessage message) {
        message.setIsRead(0);
        this.save(message);
    }

    @Override
    public void markAsRead(Long msgId, Long empId) {
        LambdaUpdateWrapper<OaMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OaMessage::getId, msgId)
                .eq(OaMessage::getReceiverId, empId)
                .set(OaMessage::getIsRead, 1);
        boolean ok = this.update(wrapper);
        if (!ok) {
            throw new RuntimeException("消息不存在或无权操作");
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
        java.util.List<Long> senderIds = result.getRecords().stream()
                .map(OaMessage::getSenderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!senderIds.isEmpty()) {
            java.util.List<SysEmployee> senders = employeeMapper.selectBatchIds(senderIds);
            Map<Long, String> senderMap = senders.stream()
                    .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName));
            result.getRecords().forEach(m -> {
                if (m.getSenderId() != null) {
                    m.setSenderName(senderMap.getOrDefault(m.getSenderId(), ""));
                }
            });
        }
        return result;
    }
}
