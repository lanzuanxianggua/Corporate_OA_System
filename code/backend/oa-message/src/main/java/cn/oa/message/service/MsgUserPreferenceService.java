package cn.oa.message.service;

import cn.oa.message.dto.MsgPreferenceDTO;
import cn.oa.message.entity.MsgUserPreference;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MsgUserPreferenceService extends IService<MsgUserPreference> {

    /**
     * 获取指定类型的偏好设置
     */
    MsgUserPreference getPreference(Long empId, String msgType);

    /**
     * 保存偏好设置（存在则更新，不存在则新增）
     */
    void savePreference(MsgPreferenceDTO dto);

    /**
     * 判断用户是否启用某类消息的某个渠道
     */
    boolean isEnabled(Long empId, String msgType, String channel);
}
