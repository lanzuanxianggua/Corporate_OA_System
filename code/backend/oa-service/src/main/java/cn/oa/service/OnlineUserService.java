package cn.oa.service;

import cn.oa.entity.OnlineUserVO;

import java.util.List;

public interface OnlineUserService {
    void userLogin(Long empId, String empName, String ip, String browser);

    void userLogout(Long empId);

    void refreshTTL(Long empId);

    List<OnlineUserVO> getOnlineUsers();
}
