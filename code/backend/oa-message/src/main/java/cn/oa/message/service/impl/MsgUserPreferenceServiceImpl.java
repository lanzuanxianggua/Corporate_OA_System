package cn.oa.message.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.oa.message.dto.MsgPreferenceDTO;
import cn.oa.message.entity.MsgUserPreference;
import cn.oa.message.mapper.MsgUserPreferenceMapper;
import cn.oa.message.service.MsgUserPreferenceService;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MsgUserPreferenceServiceImpl extends ServiceImpl<MsgUserPreferenceMapper, MsgUserPreference> implements MsgUserPreferenceService {

    @Override
    public MsgUserPreference getPreference(Long empId, String msgType) {
        if (empId == null || StrUtil.isBlank(msgType)) {
            throw new BusinessException("参数不能为空");
        }
        return getOne(new LambdaQueryWrapper<MsgUserPreference>()
                .eq(MsgUserPreference::getEmpId, empId)
                .eq(MsgUserPreference::getMsgType, msgType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePreference(MsgPreferenceDTO dto) {
        if (dto.getEmpId() == null || StrUtil.isBlank(dto.getMsgType())) {
            throw new BusinessException("参数不能为空");
        }

        MsgUserPreference exist = getOne(new LambdaQueryWrapper<MsgUserPreference>()
                .eq(MsgUserPreference::getEmpId, dto.getEmpId())
                .eq(MsgUserPreference::getMsgType, dto.getMsgType()));

        if (exist != null) {
            exist.setChannels(dto.getChannels());
            exist.setEnabled(dto.getEnabled());
            updateById(exist);
        } else {
            MsgUserPreference pref = new MsgUserPreference();
            pref.setEmpId(dto.getEmpId());
            pref.setMsgType(dto.getMsgType());
            pref.setChannels(dto.getChannels());
            pref.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
            pref.setCreatedAt(LocalDateTime.now());
            save(pref);
        }
    }

    /**
     * 判断用户是否启用某类消息的某个渠道
     */
    public boolean isEnabled(Long empId, String msgType, String channel) {
        MsgUserPreference pref = getPreference(empId, msgType);
        if (pref == null) {
            // 没有配置默认启用
            return true;
        }
        if (Integer.valueOf(0).equals(pref.getEnabled())) {
            return false;
        }
        // 检查渠道是否在允许列表中
        if (StrUtil.isNotBlank(pref.getChannels()) && channel != null) {
            return pref.getChannels().contains(channel);
        }
        return true;
    }
}
