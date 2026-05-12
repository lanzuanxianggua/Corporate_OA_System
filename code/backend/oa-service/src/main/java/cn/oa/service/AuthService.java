package cn.oa.service;

import cn.oa.entity.LoginVO;
import cn.oa.entity.SysEmployee;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    LoginVO login(String username, String password);

    LoginVO login(String username, String password, HttpServletRequest request);

    void logout(Long empId);

    void register(SysEmployee employee);

    LoginVO refreshToken(String refreshToken);
}
