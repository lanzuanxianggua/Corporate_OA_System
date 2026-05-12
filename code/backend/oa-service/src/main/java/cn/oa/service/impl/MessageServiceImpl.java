package cn.oa.service.impl;

import cn.oa.entity.OaMessage;
import cn.oa.mapper.OaMessageMapper;
import cn.oa.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<OaMessageMapper, OaMessage> implements MessageService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void send(OaMessage message) {
        message.setIsRead(0);
        this.save(message);
    }

    @Override
    public void markAsRead(Long msgId) {
        LambdaUpdateWrapper<OaMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OaMessage::getId, msgId)
                .set(OaMessage::getIsRead, 1);
        this.update(wrapper);
    }

    @Override
    public Long getUnreadCount(Long empId) {
        LambdaQueryWrapper<OaMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaMessage::getReceiverId, empId)
                .eq(OaMessage::getIsRead, 0);
        return this.count(wrapper);
    }
}
