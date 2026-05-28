package cn.oa.service.impl;

import lombok.extern.slf4j.Slf4j;

import cn.oa.common.exception.BusinessException;
import cn.oa.common.utils.BrowserUtil;
import cn.oa.common.utils.IpUtil;
import cn.oa.common.utils.JwtUtil;
import cn.oa.entity.*;
import cn.oa.vo.LoginVO;
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

    @Override
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
        String ip = IpUtil.getClientIp(request);
        String ua = request != null ? request.getHeader("User-Agent") : null;
        String browser = BrowserUtil.getBrowser(ua);
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

            // Verify the refresh token matches the one stored in Redis
            Object storedToken = redisTemplate.opsForValue().get("token:" + empId);
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new BusinessException("refreshToken 已失效，请重新登录");
            }

            String newToken = jwtUtil.generateToken(empId, empName);
            redisTemplate.opsForValue().set("token:" + empId, newToken, 7200, TimeUnit.SECONDS);
            // Renew roles TTL
            redisTemplate.expire("roles:" + empId, 7200, TimeUnit.SECONDS);
            LoginVO vo = new LoginVO();
            vo.setAccessToken(newToken);
            vo.setRefreshToken(newToken);
            vo.setExpires(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("refreshToken 无效");
        }
    }

    private void recordLoginLog(Long empId, String username, HttpServletRequest request, int status, String message) {
        if (request == null) return;
        String ua = request.getHeader("User-Agent");
        OaLoginLog log = new OaLoginLog();
        log.setEmpId(empId);
        log.setUsername(username);
        log.setIp(IpUtil.getClientIp(request));
        log.setBrowser(BrowserUtil.getBrowser(ua));
        log.setOs(BrowserUtil.getOs(ua));
        log.setStatus(status);
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}
