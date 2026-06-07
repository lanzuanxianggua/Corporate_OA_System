package cn.oa.system.service;

import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.context.UserContext;
import cn.oa.platform.common.exception.BizException;
import cn.oa.platform.security.config.SecurityProperties;
import cn.oa.platform.security.jwt.JwtUtil;
import cn.oa.platform.security.password.BCryptPasswordEncoder;
import cn.oa.platform.security.password.PasswordEncoder;
import cn.oa.system.dto.ChangePasswordReq;
import cn.oa.system.dto.LoginReq;
import cn.oa.system.entity.SysDept;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.entity.SysPermission;
import cn.oa.system.entity.SysRole;
import cn.oa.system.exception.AuthDomainException;
import cn.oa.system.mapper.SysDeptMapper;
import cn.oa.system.mapper.SysEmpMapper;
import cn.oa.system.mapper.SysEmpRoleMapper;
import cn.oa.system.mapper.SysPermissionMapper;
import cn.oa.system.mapper.SysRoleMapper;
import cn.oa.system.mapper.SysRolePermissionMapper;
import cn.oa.system.vo.LoginResp;
import cn.oa.system.vo.MenuTreeVO;
import cn.oa.system.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证服务.
 *
 * <p>v2 Phase 1:
 * <ul>
 *   <li>保留 v1 兼容方法 (findByUsername/findById/findRolesByEmpId/findPermCodesByEmpId/recordLogin)</li>
 *   <li>扩展登录全链路: login / refreshToken / getCurrentUser / getCurrentUserMenus / changePassword / logout</li>
 *   <li>密码仍为明文比较 (BCrypt 切换待 oa-platform-security 新增 PasswordEncoder)</li>
 *   <li>暂不实现: captcha / Redis 黑名单 (需新增 CaptchaService + RedisConfig)</li>
 * </ul>
 */
@Slf4j
@Service
public class AuthService {

    private final SysEmpMapper empMapper;
    private final SysEmpRoleMapper empRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysDeptMapper deptMapper;
    private final JwtUtil jwtUtil;
    private final SecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;
    /**
     * 图形验证码服务 (可选). 注入为 {@code null} 时, 登录路径跳过 captcha 校验
     * (便于在测试 profile 排除 Redis / 单测不依赖 Redis 的场景). 运行时 (dev/prod)
     * 由 Spring 自动注入 CaptchaServiceImpl, 此时登录必须传 {@code captchaKey}+{@code captchaCode}.
     */
    private final CaptchaService captchaService;

    /**
     * 主构造器 (Spring 注入用).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(SysEmpMapper empMapper,
                       SysEmpRoleMapper empRoleMapper,
                       SysRoleMapper roleMapper,
                       SysRolePermissionMapper rolePermMapper,
                       SysPermissionMapper permissionMapper,
                       SysDeptMapper deptMapper,
                       JwtUtil jwtUtil,
                       SecurityProperties securityProperties,
                       PasswordEncoder passwordEncoder,
                       @org.springframework.beans.factory.annotation.Autowired(
                               required = false) CaptchaService captchaService) {
        this.empMapper = empMapper;
        this.empRoleMapper = empRoleMapper;
        this.roleMapper = roleMapper;
        this.rolePermMapper = rolePermMapper;
        this.permissionMapper = permissionMapper;
        this.deptMapper = deptMapper;
        this.jwtUtil = jwtUtil;
        this.securityProperties = securityProperties;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
    }

    /**
     * 兼容旧测试桩的次构造器 (4 参), 仅注入旧字段, passwordEncoder 传 null.
     * <p>仅供 {@code AuthServiceTest} 4 参老用例使用; 新代码请用主构造器.
     * <p>{@code passwordEncoder=null} 时, {@link #matchesPassword(String, String)} 走纯明文相等分支,
     * 与 v2 Phase 1 行为完全一致.
     */
    public AuthService(SysEmpMapper empMapper,
                       SysEmpRoleMapper empRoleMapper,
                       SysRoleMapper roleMapper,
                       SysRolePermissionMapper rolePermMapper) {
        this(empMapper, empRoleMapper, roleMapper, rolePermMapper,
                null, null, null, null, null, null);
    }

    // ===================== 兼容旧接口 (保留 v1 行为, 内部已切换 selectByUsername) =====================

    public SysEmp findByUsername(String username) {
        // 优先用新方法 (带 del_flag=0 过滤); 失败时回退到旧 Lambda (兼容测试桩)
        SysEmp emp = empMapper.selectByUsername(username);
        if (emp != null) {
            return emp;
        }
        return empMapper.selectOne(new LambdaQueryWrapper<SysEmp>()
                .eq(SysEmp::getUsername, username)
                .last("LIMIT 1"));
    }

    public SysEmp findById(Long empId) {
        return empMapper.selectById(empId);
    }

    public List<String> findRolesByEmpId(Long empId) {
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(r -> "ROLE_" + r.getRoleCode())
                .collect(Collectors.toList());
    }

    public List<String> findPermCodesByEmpId(Long empId) {
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return rolePermMapper.selectPermCodesByRoleIds(roleIds);
    }

    @Transactional
    public void recordLogin(Long empId, String ip) {
        SysEmp emp = empMapper.selectById(empId);
        if (emp != null) {
            empMapper.updateLastLogin(empId, LocalDateTime.now(), ip, emp.getVersion());
            return;
        }
        // 兼容旧测试桩 (mock 不返回 emp 时)
        SysEmp update = new SysEmp();
        update.setId(empId);
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(ip);
        empMapper.updateById(update);
    }

    // ===================== v2 新增: 登录/刷新/登出/当前用户/菜单/改密 =====================

    /**
     * 登录.
     */
    @Transactional
    public LoginResp login(LoginReq req, String clientIp) {
        // 0) 图形验证码 (v2 Phase 2: 可选. captchaService 注入时强制校验, 未注入时跳过 - 保持单测零回归)
        if (captchaService != null) {
            captchaService.validate(req.getCaptchaKey(), req.getCaptchaCode());
        }
        SysEmp emp = empMapper.selectByUsername(req.getUsername());
        if (emp == null) {
            log.warn("登录失败 - 用户不存在: {}", req.getUsername());
            throw AuthDomainException.userNotFound();
        }
        if (!"ACTIVE".equals(emp.getStatus())) {
            log.warn("登录失败 - 账号非 ACTIVE: {} status={}", req.getUsername(), emp.getStatus());
            throw AuthDomainException.accountDisabled();
        }
        if (!matchesPassword(req.getPassword(), emp.getPassword(), emp.getId(), true)) {
            log.warn("登录失败 - 密码错误: {}", req.getUsername());
            throw AuthDomainException.passwordInvalid();
        }

        List<SysRole> roles = roleMapper.selectByEmpId(emp.getId());
        List<String> roleCodes = roles.stream()
                .map(r -> "ROLE_" + r.getRoleCode())
                .toList();
        List<String> permCodes = permCodesByEmpId(emp.getId());

        String access = jwtUtil.generateAccessToken(emp.getId(), emp.getUsername(), roleCodes, permCodes);
        String refresh = jwtUtil.generateRefreshToken(emp.getId(), emp.getUsername());

        empMapper.updateLastLogin(emp.getId(), LocalDateTime.now(), clientIp, emp.getVersion());

        UserInfoVO userInfo = buildUserInfo(emp, roles, permCodes);

        long expiresIn = securityProperties.getJwt().getAccessTtlSeconds();
        LoginResp resp = new LoginResp();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        resp.setExpiresIn(expiresIn);
        resp.setTokenType("Bearer");
        resp.setUserInfo(userInfo);
        log.info("登录成功: empId={}, username={}", emp.getId(), emp.getUsername());
        return resp;
    }

    /**
     * 刷新 access token.
     */
    public LoginResp refreshToken(String refreshToken) {
        var claims = jwtUtil.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw AuthDomainException.tokenInvalid();
        }
        Long empId = claims.get("uid", Long.class);
        String username = claims.get("uname", String.class);

        SysEmp emp = empMapper.selectById(empId);
        if (emp == null || !"ACTIVE".equals(emp.getStatus())) {
            throw AuthDomainException.userNotFound();
        }
        List<SysRole> roles = roleMapper.selectByEmpId(empId);
        List<String> roleCodes = roles.stream()
                .map(r -> "ROLE_" + r.getRoleCode())
                .toList();
        List<String> permCodes = permCodesByEmpId(empId);

        String access = jwtUtil.generateAccessToken(empId, username, roleCodes, permCodes);

        UserInfoVO userInfo = buildUserInfo(emp, roles, permCodes);
        LoginResp resp = new LoginResp();
        resp.setAccessToken(access);
        resp.setRefreshToken(refreshToken);  // 简化: 不轮换
        resp.setExpiresIn(securityProperties.getJwt().getAccessTtlSeconds());
        resp.setTokenType("Bearer");
        resp.setUserInfo(userInfo);
        return resp;
    }

    /**
     * 登出 (v2 Phase 1 简化: 仅清空 ThreadLocal, 不写 Redis 黑名单).
     */
    public void logout() {
        UserContext.clear();
    }

    /**
     * 获取当前登录用户完整信息.
     */
    public UserInfoVO getCurrentUser() {
        UserContext.UserInfo ctx = UserContext.get();
        if (ctx == null) {
            throw new BizException(RCode.UNAUTHORIZED, "未登录");
        }
        SysEmp emp = empMapper.selectById(ctx.getEmpId());
        if (emp == null) {
            throw AuthDomainException.userNotFound();
        }
        List<SysRole> roles = roleMapper.selectByEmpId(emp.getId());
        List<String> permCodes = permCodesByEmpId(emp.getId());
        return buildUserInfo(emp, roles, permCodes);
    }

    /**
     * 获取当前用户菜单树.
     */
    public List<MenuTreeVO> getCurrentUserMenus() {
        UserContext.UserInfo ctx = UserContext.get();
        if (ctx == null) {
            throw new BizException(RCode.UNAUTHORIZED, "未登录");
        }
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(ctx.getEmpId());
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysPermission> permissions = permissionMapper.selectMenusByRoleIds(roleIds);
        return buildMenuTree(permissions);
    }

    /**
     * 修改密码.
     */
    @Transactional
    public void changePassword(ChangePasswordReq req) {
        UserContext.UserInfo ctx = UserContext.get();
        if (ctx == null) {
            throw new BizException(RCode.UNAUTHORIZED, "未登录");
        }
        SysEmp emp = empMapper.selectById(ctx.getEmpId());
        if (emp == null) {
            throw AuthDomainException.userNotFound();
        }
        if (!matchesPassword(req.getOldPassword(), emp.getPassword(), emp.getId(), true)) {
            throw AuthDomainException.passwordInvalid();
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BizException(RCode.BAD_REQUEST, "两次输入的密码不一致");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8) {
            throw new BizException(RCode.PASSWORD_INVALID, "新密码长度不能少于 8 位");
        }
        // v2 Phase 2: BCrypt 哈希写入 (PasswordEncoder 由 SecurityAutoConfiguration 注入)
        String encoded = passwordEncoder != null
                ? passwordEncoder.encode(req.getNewPassword())
                : req.getNewPassword();
        empMapper.updatePassword(emp.getId(), encoded);
        log.info("密码已修改: empId={}", emp.getId());
    }

    // ===================== 内部工具 =====================

    private List<String> permCodesByEmpId(Long empId) {
        List<Long> roleIds = empRoleMapper.selectRoleIdsByEmpId(empId);
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        return rolePermMapper.selectPermCodesByRoleIds(roleIds);
    }

    private UserInfoVO buildUserInfo(SysEmp emp, List<SysRole> roles, List<String> permCodes) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(emp.getId());
        vo.setEmpCode(emp.getEmpCode());
        vo.setUsername(emp.getUsername());
        vo.setRealName(emp.getRealName());
        vo.setAvatar(emp.getAvatar());
        vo.setDeptId(emp.getDeptId());
        if (emp.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(emp.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getDeptName());
            }
        }
        vo.setDataScope(resolveDataScope(roles));
        vo.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        vo.setPermissions(permCodes);
        return vo;
    }

    /**
     * 数据权限取最大范围.
     */
    private String resolveDataScope(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return "SELF";
        }
        Map<String, Integer> priority = new HashMap<>();
        priority.put("ALL", 5);
        priority.put("DEPT_DOWN", 4);
        priority.put("COMPANY", 3);
        priority.put("DEPT", 2);
        priority.put("SELF", 1);
        return roles.stream()
                .map(SysRole::getDataScope)
                .filter(s -> s != null && !s.isBlank())
                .max(Comparator.comparingInt(s -> priority.getOrDefault(s, 0)))
                .orElse("SELF");
    }

    /**
     * 菜单树构建 (按 sort 升序, 同 sort 按 id 升序).
     */
    private List<MenuTreeVO> buildMenuTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, MenuTreeVO> nodeMap = new HashMap<>();
        for (SysPermission p : permissions) {
            MenuTreeVO node = new MenuTreeVO();
            node.setId(p.getId());
            node.setParentId(p.getParentId());
            node.setName(p.getPermName());
            node.setPath(p.getPath());
            node.setIcon(p.getIcon());
            node.setPermCode(p.getPermCode());
            node.setSort(p.getSortOrder());
            node.setType(p.getPermType());
            node.setHidden(Boolean.FALSE);
            nodeMap.put(p.getId(), node);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (SysPermission p : permissions) {
            MenuTreeVO node = nodeMap.get(p.getId());
            if (p.getParentId() == null || p.getParentId() == 0L) {
                roots.add(node);
            } else {
                MenuTreeVO parent = nodeMap.get(p.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        Comparator<MenuTreeVO> sorter = Comparator
                .comparing(MenuTreeVO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MenuTreeVO::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        roots.sort(sorter);
        for (MenuTreeVO root : roots) {
            sortChildren(root, sorter);
        }
        return roots;
    }

    private void sortChildren(MenuTreeVO node, Comparator<MenuTreeVO> sorter) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            return;
        }
        node.getChildren().sort(sorter);
        for (MenuTreeVO child : node.getChildren()) {
            sortChildren(child, sorter);
        }
    }

    /**
     * v2 Phase 2: 密码双轨校验.
     *
     * <p>判定规则 (按以下顺序):
     * <ol>
     *   <li>{@code hashed} 为空 → 不匹配 (false)</li>
     *   <li>已为 BCrypt 哈希 ({@code $2a$}/{@code $2b$}/{@code $2y$} 开头) → 走 {@link PasswordEncoder#matches}</li>
     *   <li>旧明文 → {@code hashed.equals(raw)} 判定; 若命中 + {@code rehashOnSuccess=true}
     *       + {@code enableLazyRehash=true} + {@code passwordEncoder != null} → 同步回写 BCrypt 哈希
     *       (保持登录响应不阻塞, 但写库会稍微拖慢, 与 Lazy Rehash 语义一致)</li>
     *   <li>{@code passwordEncoder == null} (兼容 4 参构造) → 仅明文相等, 行为与 v2 Phase 1 一致</li>
     * </ol>
     */
    private boolean matchesPassword(String raw, String hashed, Long empId, boolean rehashOnSuccess) {
        if (hashed == null) {
            return false;
        }
        // 1) BCrypt 哈希
        if (BCryptPasswordEncoder.looksHashed(hashed)) {
            if (passwordEncoder == null) {
                return false;
            }
            try {
                return passwordEncoder.matches(raw, hashed);
            } catch (RuntimeException ex) {
                log.warn("BCrypt matches 异常, 视作不匹配: {}", ex.getMessage());
                return false;
            }
        }
        // 2) 旧明文
        boolean plainMatch = hashed.equals(raw);
        if (!plainMatch) {
            return false;
        }
        // 3) 明文命中: 触发 Lazy Rehash
        if (rehashOnSuccess && empId != null && passwordEncoder != null
                && securityProperties != null
                && securityProperties.getBcrypt() != null
                && securityProperties.getBcrypt().isEnableLazyRehash()) {
            try {
                String encoded = passwordEncoder.encode(raw);
                empMapper.updatePassword(empId, encoded);
                log.info("Lazy Rehash: 员工 {} 明文密码已升级为 BCrypt 哈希", empId);
            } catch (RuntimeException ex) {
                // 异步回写失败不阻塞登录, 仅记录日志
                log.warn("Lazy Rehash 回写失败 empId={}: {}", empId, ex.getMessage());
            }
        }
        return true;
    }

    /**
     * 兼容旧签名: 不触发 Lazy Rehash (供测试或仅读场景).
     */
    private boolean matchesPassword(String raw, String hashed) {
        return matchesPassword(raw, hashed, null, false);
    }
}
