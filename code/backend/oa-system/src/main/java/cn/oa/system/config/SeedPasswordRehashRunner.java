package cn.oa.system.config;

import cn.oa.platform.security.password.BCryptPasswordEncoder;
import cn.oa.platform.security.password.PasswordEncoder;
import cn.oa.system.entity.SysEmp;
import cn.oa.system.mapper.SysEmpMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时种子密码重 hash Runner.
 *
 * <p>默认 <b>关闭</b> ({@code oa.security.bcrypt.rehash-seed-on-startup=false}).
 * 推荐依赖 {@link cn.oa.system.service.AuthService} 登录路径的 Lazy Rehash,
 * 即用户在首次登录时自动将明文密码升级为 BCrypt 哈希, 零停机零阻塞.
 *
 * <p>本 Runner 仅在运维明确开启 {@code rehash-seed-on-startup=true} 时执行,
 * 用于在系统初始化阶段一次性把 5 个 seed 用户密码全部升级为 BCrypt.
 *
 * <p>并发保护: 多副本同时启动时, 对每条记录逐条 UPDATE, 走 MySQL 行锁;
 * 由于 5 个 seed 用户几乎不并发, 无需分布式锁.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "oa.security.bcrypt", name = "rehash-seed-on-startup", havingValue = "true")
public class SeedPasswordRehashRunner implements CommandLineRunner {

    /** 5 个 seed 用户 (与 V900 + V910 一致). */
    private static final List<String> SEED_USERS = List.of("admin", "hr01", "mgr01", "emp01", "fin01");

    private final SysEmpMapper empMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${oa.security.bcrypt.rehash-default-password:admin123}")
    private String defaultPassword;

    public SeedPasswordRehashRunner(SysEmpMapper empMapper, PasswordEncoder passwordEncoder) {
        this.empMapper = empMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("[SeedRehash] 启动种子密码 BCrypt 升级...");
        List<SysEmp> emps = empMapper.selectByUsernames(SEED_USERS);
        if (emps == null || emps.isEmpty()) {
            log.warn("[SeedRehash] 未找到任何 seed 用户, 跳过");
            return;
        }
        int rehashed = 0;
        for (SysEmp emp : emps) {
            String current = emp.getPassword();
            if (BCryptPasswordEncoder.looksHashed(current)) {
                log.info("[SeedRehash] 已是 BCrypt, 跳过: {}", emp.getUsername());
                continue;
            }
            String encoded = passwordEncoder.encode(defaultPassword);
            int updated = empMapper.updatePassword(emp.getId(), encoded);
            if (updated > 0) {
                rehashed++;
                log.info("[SeedRehash] 用户 {} 密码已升级为 BCrypt", emp.getUsername());
            } else {
                log.warn("[SeedRehash] 用户 {} 升级失败, updated={}", emp.getUsername(), updated);
            }
        }
        log.info("[SeedRehash] 完成: 共升级 {} 个用户", rehashed);
    }
}
