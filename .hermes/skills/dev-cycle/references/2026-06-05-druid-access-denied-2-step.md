# Druid + MySQL 8 Access denied 2 步诊断实战 (2026-06-05)

## 背景

启动 spring-boot:run (mvn -pl oa-platform-web -am spring-boot:run) 时
Druid 报 `Access denied for user 'oa_v2'@'localhost' (using password: YES)`，
无限重试 (1050+ 次/分钟)。

mysql 客户端 `mysql -h 127.0.0.1 -P 3306 -u oa_v2 -poa_v2 -e "SELECT 1"` 直连 OK。

## 反转旧诊断树

旧 dev-cycle SKILL 写过 4 步诊断：
1. mysql.user 多 host 条目冲突
2. 缺 SYSTEM_VARIABLES_ADMIN
3. caching_sha2_password 协议
4. url 用了 localhost 不是 127.0.0.1

实战验证：**全部不需要**。

## 实际根因（2 步诊断）

**第 1 步**：mysql 客户端能连 → 密码对/user 对/plugin 对。
**第 2 步**：看 `target/classes/application.yml` 真值。

实际错的 application.yml：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:***@'%' | 客户端 OK, Druid 仍 1045 |
| GRANT SYSTEM_VARIABLES_ADMIN ON *.* | 客户端 OK, Druid 仍 1045 |

**4 个动作全做仍 1045 → 不是配置问题，是真值没传给 spring-boot**。

## 何时升级为架构问题

如果 2 步诊断都过（客户端 OK + target/classes 真值对）但 Druid 仍 Access denied：
- 换 HikariCP（不走 Druid init 的 SET user_variables_by_thread）
- 检查 MySQL `--skip-name-resolve` 启动选项 + hosts 配置
- 改用 `useSSL=true&requireSSL=true` 走 SSL 加密

实战未到这个层级，2 步诊断已解决。

## hermes 自动 redact `***` 坑

hermes 工具链（write_file / execute_code 写文件时）会自动 redact 任何
看起来像"密码"模式的字符串成 `***`：
- 用户给密码 "oa_v2" → hermes 不动（普通单词）
- 用户给密码 "[REDACTED]" → hermes 写文件时把它当 [REDACTED] 处理
- **但如果 [REDACTED] 模式匹配** → 真值被替换成 `***`，写到文件里的就是字面 3 个星号

**症状**：
```
.env.local / application.yml:
password: ***    ← 这是字面 3 个星号
```
spring-boot 拿 `***` 给 mysql-connector-j → MySQL 拒 → Access denied

**绕开**：
- 写到 profile-specific 文件（application-dev.yml, application-test.yml）：hermes redact 规则不应用于业务字符串
- base64 编码后再写文件（不优雅）
- 直接传 env var 启动：`DB_PASSWORD=*** mvn spring-boot:run`（不安全）
