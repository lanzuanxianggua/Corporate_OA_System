package cn.oa.controller;

import cn.oa.common.result.R;
import cn.oa.entity.LoginDTO;
import cn.oa.entity.LoginVO;
import cn.oa.entity.SysEmployee;
import cn.oa.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO vo = authService.login(dto.getUsername(), dto.getPassword(), request);
        return R.ok(vo);
    }

    @PostMapping("/refresh-token")
    public R<LoginVO> refreshToken(@RequestBody RefreshTokenDTO dto) {
        LoginVO vo = authService.refreshToken(dto.getRefreshToken());
        return R.ok(vo);
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        if (empId != null) {
            authService.logout(empId);
        }
        return R.ok();
    }

    @lombok.Data
    public static class RefreshTokenDTO {
        private String refreshToken;
    }
}
