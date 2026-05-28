package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.exception.BusinessException;
import cn.oa.common.utils.JwtUtil;
import cn.oa.entity.*;
import cn.oa.mapper.OaLoginLogMapper;
import cn.oa.mapper.SysEmpRoleMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.mapper.SysRoleMapper;
import cn.oa.service.AuthService;
import cn.oa.service.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private SysEmpRoleMapper empRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private OaLoginLogMapper loginLogMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(String username, String password) {
        return login(username, password, null);
    }

    public LoginVO login(String username, String password, HttpServletRequest request) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployee::getEmpCode, username);
        SysEmployee employee = employeeMapper.selectOne(wrapper);
        if (employee == null) {
            recordLoginLog(null, username, request, 0, "用户名不存在");
            throw new BusinessException("用户名不存在");
        }
        if (!BCrypt.checkpw(password, employee.getPassword())) {
            recordLoginLog(employee.getId(), username, request, 0, "密码错误");
            throw new BusinessException("密码错误");
        }

        LambdaQueryWrapper<SysEmpRole> empRoleWrapper = new LambdaQueryWrapper<>();
        empRoleWrapper.eq(SysEmpRole::getEmpId, employee.getId());
        List<SysEmpRole> empRoles = empRoleMapper.selectList(empRoleWrapper);

        List<String> roleKeys = new ArrayList<>();
        if (!empRoles.isEmpty()) {
            List<Long> roleIds = empRoles.stream()
                    .map(SysEmpRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            roleKeys = roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toList());
        }
        if (roleKeys.isEmpty()) {
            roleKeys.add("USER");
        }

        String token = jwtUtil.generateToken(employee.getId(), employee.getEmpName());
        redisTemplate.opsForValue().set("token:" + employee.getId(), token, 7200, TimeUnit.SECONDS);

        // 将角色信息存入Redis，供权限校验使用
        String rolesKey = "roles:" + employee.getId();
        redisTemplate.opsForValue().set(rolesKey, roleKeys, 7200, TimeUnit.SECONDS);

        // 记录在线用户
        String ip = getClientIp(request);
        String browser = getBrowser(request);
        onlineUserService.userLogin(employee.getId(), employee.getEmpName(), ip, browser);

        // 记录登录日志
        recordLoginLog(employee.getId(), username, request, 1, "登录成功");
        log.info("User login successful: username={}, empId={}", username, employee.getId());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(token);
        vo.setRefreshToken(token);
        vo.setExpires(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        vo.setUsername(employee.getEmpCode());
        vo.setNickname(employee.getEmpName());
        vo.setAvatar(employee.getAvatar() != null ? employee.getAvatar() : "");
        vo.setRoles(roleKeys);
        vo.setPermissions(List.of("*:*:*"));
        return vo;
    }

    @Override
    public void logout(Long empId) {
        redisTemplate.delete("token:" + empId);
        redisTemplate.delete("roles:" + empId);
        onlineUserService.userLogout(empId);
        log.info("User logout: empId={}", empId);
    }

    @Override
    public void register(SysEmployee employee) {
        employee.setPassword(BCrypt.hashpw(employee.getPassword()));
        employeeMapper.insert(employee);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(refreshToken);
            Long empId = claims.get("empId", Long.class);
            String empName = claims.get("empName", String.class);
            String newToken = jwtUtil.generateToken(empId, empName);
            redisTemplate.opsForValue().set("token:" + empId, newToken, 7200, TimeUnit.SECONDS);
            // 续期角色信息
            redisTemplate.expire("roles:" + empId, 7200, TimeUnit.SECONDS);
            LoginVO vo = new LoginVO();
            vo.setAccessToken(newToken);
            vo.setRefreshToken(newToken);
            vo.setExpires(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
            return vo;
        } catch (Exception e) {
            throw new BusinessException("refreshToken 无效");
        }
    }

    private void recordLoginLog(Long empId, String username, HttpServletRequest request, int status, String message) {
        if (request == null) return;
        OaLoginLog log = new OaLoginLog();
        log.setEmpId(empId);
        log.setUsername(username);
        log.setIp(getClientIp(request));
        log.setBrowser(getBrowser(request));
        log.setOs(getOs(request));
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getBrowser(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "unknown";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        if (ua.contains("Edge")) return "Edge";
        return "Other";
    }

    private String getOs(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "Mac";
        if (ua.contains("Linux")) return "Linux";
        return "Other";
    }
}
