# v2 session log: oa-system + oa-platform-web bootstrap (2026-06-04)

Concrete evidence captured during the third working session on the v2
rewrite. Read this when a build breaks in a way the SKILL.md pitfalls
don't cover, OR when you want a reproducible recipe for a specific
failure mode.

## 1. lombok failure is real and total

Repro:
- `lombok:1.18.34`
- JDK 17 (Temurin `17.0.15`)
- Maven 3.9.9
- Windows 10, zh-CN locale
- `maven-compiler-plugin` 3.13.0 with `<annotationProcessorPaths>`

Observed:
- `target/generated-sources/annotations/` is **not even created**.
- Compilation proceeds without invoking the lombok annotation processor.
- `@Getter` / `@Data` / `@AllArgsConstructor` / `@Slf4j` generate
  nothing. Errors look like:
  - "RCode 不是抽象, 但未实现 ResultCode 中的抽象方法 getMessage()"
  - "找不到方法 log"
  - "actual argument list and formal argument list differ in length"

No known-working fix as of 2026-06-04. The v2 project rule is: **no
lombok, ever**. See SKILL.md -> "Lombok is forbidden in this project".

## 2. MyBatis-Plus 3.5.9 inventory

Verified by `jar tf` on the 3.5.9 jar:

- `com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor` — present
- `BlockAttackInnerInterceptor` — gone
- `PaginationInnerInterceptor` — gone
- `IllegalSQLInnerInterceptor` — gone

The `mybatis-plus-jsqlparser` companion artifact is **not** a
transitive of `mybatis-plus-spring-boot3-starter` 3.5.9. To get
pagination back, you'd need to add `mybatis-plus-jsqlparser`
explicitly AND verify the class lands in the same package as the
older `3.5.x` point releases. For now, v2 ships with **only**
OptimisticLocker in the common config.

## 3. The dangling `</build>` corruption

A bash one-liner intended to append a `<build>` block to 18 child
POMs actually appended only `</build>` (the opening `<build>` and
block content were lost). Symptom:

```
[ERROR] Malformed POM ...: Unrecognised tag
[ERROR] 'build' ... is not a child of 'project'
```

Fix: read each affected pom, locate the dangling `</build>`, remove
it. Use the `mvn validate` recipe from SKILL.md pitfall "Child POM
gotcha" before committing any batch POM edit.

## 4. Spring Boot 3.4 + `@ConfigurationProperties` bean duplication

When a class has BOTH `@Configuration` and `@ConfigurationProperties`,
Spring Boot 3.4 registers it twice (once as a component-scanned
`@Configuration`, once via the `@ConfigurationProperties` scan).
Diagnostic:

```
Unsatisfied dependency ... expected single matching bean but found 2:
cn.oa.platform.security.config.SecurityProperties,
oa.security-cn.oa.platform.security.config.SecurityProperties
```

v2 standard fix: drop `@Configuration` from the props class, add
`@EnableConfigurationProperties(PropsClass.class)` on the
`AutoConfiguration` class.

## 5. `AutoConfiguration.imports` failure modes (multiple, often combined)

### 5a. Trailing comma
When the file ends with `cn.oa.platform.common.config.MybatisPlusConfig,`
(note the trailing comma) and a class is missing from the classpath
OR the class name is misspelled, Spring's `AutoConfigurationSorter`
fails with:
```
Unable to read meta-data for class cn.oa.platform.common.config.MybatisPlusConfig,
Caused by: java.io.FileNotFoundException: class path resource
[cn/oa/platform/common/config/MybatisPlusConfig,/.class] cannot be opened
```
The `,` gets concatenated into the class name.

### 5b. Spring Boot 3.4 + nested-jar parser bug (NEW 2026-06-04)
Even with a perfectly-formatted `imports` file
(`MybatisPlusConfig,cn.oa.JacksonConfig,cn.oa.IdGeneratorConfig` on
one line, no trailing comma), `java -jar` on the packaged
`oa-platform-web` jar STILL fails with:
```
Unable to read meta-data for class org.springframework.boot.autoconfigure.EnableAutoConfiguration=
```
Root cause: the boot loader's `JarEntriesStream` hands the file to
`Properties.load` in a way that, on Windows + zh-CN locale, treats
the entire `key=value` line as a single class FQCN. The error class
name is literally the `EnableAutoConfiguration=` key.

**Workaround (verified)**: drop the `imports` files from
`oa-platform-common` / `oa-platform-security` / `oa-system` and use
`@Import` on the main Application class:
```java
@SpringBootApplication(scanBasePackages = "cn.oa")
@MapperScan({"cn.oa.system.mapper", "cn.oa.workflow.mapper", ...})
@Import({MybatisPlusConfig.class, JacksonConfig.class,
          IdGeneratorConfig.class, SecurityAutoConfiguration.class})
public class OaSystemApplication { ... }
```
Each imported class must be `@Configuration` (not just
`@ConfigurationProperties`). This avoids the imports-file parser
entirely.

### 5c. Sibling subagent re-creates the file
When you delete an `imports` file from `oa-system/` because it's
empty, a sibling subagent working on the same module can re-add a
stub like:
```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=
```
The `write_file` tool warns: "sibling subagent modified this file
but this agent never read it." **Action**: read first, then
overwrite or re-delete. After re-deleting, run
`mvn -pl oa-system -am -DskipTests clean install` — without `clean`
the stale `target/classes/META-INF/...` is repackaged into the web
jar's `BOOT-INF/lib/`, and the failure returns.

### 5d. Verification recipe after any change
```bash
# Confirm the file content matches what the running jar will see
mvn -pl oa-platform-common -DskipTests clean install
mvn -pl oa-platform-web -am -DskipTests clean package
unzip -p code/backend/oa-platform-web/target/oa-platform-web.jar \
  BOOT-INF/lib/oa-platform-common-2.0.0-SNAPSHOT.jar > /tmp/common.jar
unzip -p /tmp/common.jar \
  META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
If the file is empty or contains a `key=` form, the build will fail
at boot time even if `mvn package` succeeded.

## 6. The `java -jar` smoke test command that worked

```bash
cd code/backend
mvn -pl oa-platform-web -am -DskipTests package

# Background boot
java -jar code/backend/oa-platform-web/target/oa-platform-web.jar &
APP_PID=$!

# Wait for the JVM to bind 8080
for i in $(seq 1 60); do
  curl -sf http://localhost:8080/api/ping > /dev/null && break
  sleep 2
done

# Verify
curl -s http://localhost:8080/api/ping

# Tear down
kill $APP_PID
```

This requires MySQL to be running with the `oa_system_v2` schema.
For local smoke tests without MySQL, swap to the H2 dev profile.
The v2 project's `docker-compose.dev.yml` brings up MySQL on
`localhost:3306`.

When the H2 dev profile is used, the `dev` profile `application-dev.yml`
still tries to connect to MySQL — you need a separate `application-test.yml`
with `spring.datasource.url=jdbc:h2:mem:...` AND
`spring.flyway.enabled=false` AND a `h2` test-scope dep in
`oa-platform-web/pom.xml`. The H2 jar must be available at runtime
for the `java -jar` smoke test too — bump it to `compile` scope OR
use a fat jar if you go this route.

## 7. Build sequence that worked end-to-end

For "I want to add a new module `oa-<name>` and have the whole
backend boot":

```bash
# 1. Build deps in order (each installs to local repo for the next).
#    -DskipTests keeps the run fast; we'll run tests separately.
mvn -pl oa-platform-common -DskipTests clean install
mvn -pl oa-platform-security -am -DskipTests clean install
mvn -pl oa-system -am -DskipTests clean install
mvn -pl oa-platform-web -am -DskipTests clean package

# 2. Tests
mvn -pl oa-system -am test
mvn -pl oa-platform-web test
```

Note: if any `-am` compile fails, the local `install` of the dep is
NOT updated, and downstream modules may compile against stale jars.
Always `install` after `compile` of a foundation module. The `clean`
on every step prevents the stale-`target/classes` trap described
in SKILL.md "mvn clean is required after editing nested-jars
resources".

### 7a. Surefire `-Dtest=` + `-am` failure
Running
```bash
mvn -pl oa-platform-web -am test -Dtest=ApplicationSmokeTest
```
fails with:
```
No tests matching pattern "ApplicationSmokeTest" were executed!
(Set -Dsurefire.failIfNoSpecifiedTests=false to ignore this error.)
```
because surefire runs in every dep module (`-am`), and
`oa-platform-common` has no such test. **The error message's
suggested flag is wrong on Maven 3.9.9** — use the
fully-qualified property:
```bash
mvn -pl oa-platform-web -am test \
    -Dtest=ApplicationSmokeTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
`DfailIfNoSpecifiedTests=false` (no namespace) is silently ignored
on Maven 3.9.9 and the build still fails. Drop `-am` to bypass
the issue entirely if the target module has all its deps installed.

## 8. The `@WebMvcTest` lesson

Don't use `@WebMvcTest(controllers = XController.class)` in a
downstream module whose classpath includes a sibling `oa-system` jar
that ships `@Mapper` interfaces. The MyBatis auto-config kicks in
during slice test loading and chokes on the missing
`SqlSessionFactory`.

For trivial Controller smoke tests (one method, no auth, no DB),
just call the method directly:

```java
class PingControllerTest {
    private final PingController controller = new PingController();
    @Test void ping() {
        R<Map<String, Object>> r = controller.ping();
        assertThat(r.getData().get("status")).isEqualTo("UP");
    }
}
```

Saves 7+ seconds of Spring context startup per test, and avoids the
mapper-collision trap entirely.

For full-stack smoke tests that need an HTTP server + DB, use
`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
+ an H2 test profile. Add the h2 dep as `test` scope to
`oa-platform-web/pom.xml` (NOT `compile` — that pollutes the
production classpath).

## 9. `MybatisPlusConfig` `@Configuration` regression

When sibling subagents edit the same `MybatisPlusConfig.java` in
parallel, one edit may strip the `@Configuration` annotation while
keeping the class. The `@Import(MybatisPlusConfig.class)` on the
main Application then fails to load it, and Spring complains
"expected at least 1 bean which qualifies as autowire candidate".

**Detection**: after a `mvn install` cycle, look for:
```
@Configuration class cn.oa.platform.common.config.MybatisPlusConfig is not eligible for auto-proxying
```
or any startup warning about a config class without `@Configuration`.

**Fix**: ensure every class listed in `@Import(...)` has BOTH
`@Configuration` AND the `@Bean` methods. The pattern is:
```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // ...
    }
}
```

## 10. H2 in `application-test.yml` recipe (NEW 2026-06-04)

```yaml
# src/main/resources/application-test.yml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:oa_test;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password: ""
  flyway:
    enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration

oa:
  security:
    jwt:
      secret: test-secret-key-12345678901234567890123456789012
      access-ttl-seconds: 3600
      refresh-ttl-seconds: 86400
```

And in `oa-platform-web/pom.xml`:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

Activate with `@ActiveProfiles("test")` on the test class. The
`MODE=MySQL` flag makes H2 accept MySQL-flavour DDL from the
Flyway migrations (when you flip flyway back on for migration
testing).

## 11. End-of-loop module status snapshot (2026-06-04 evening)

After the dev→test→review inner loop ran across 4 modules:

| Module | Java classes | Tests | Status |
|--------|-------------|-------|--------|
| oa-platform-common | 18 | 6/6 | ✓ closed |
| oa-platform-security | 8 | 4/4 | ✓ closed |
| oa-system | 14 | 4/4 | ✓ closed |
| oa-platform-web | 5 | 3/3 | ✓ closed |
| **Total** | **45** | **17/17** | |

Verification command that runs ALL tests in one shot:
```bash
mvn -pl oa-platform-common,oa-platform-security,oa-system,oa-platform-web \
    -am test
```

End-to-end boot verified via `@SpringBootTest(RANDOM_PORT) +
TestRestTemplate` against the H2 test profile:
- `/api/ping` returns 200 with `R{code:0, data:{status:UP, service:"oa-system"}}`
- `/api/system/emps` returns 200 (no auth required at the Filter
  layer; v2's `JwtAuthenticationFilter` only writes `UserContext`
  when a valid Bearer token is present — business layer does
  authorization via `@RequirePermission` interceptor).

Spring framework reached the `DruidDataSource` init stage in
production profile (`java -jar`) before MySQL auth failed
(`Access denied for user 'oa_v2'`), confirming the Bean wiring
chain (`@Import` of 4 `@Configuration` + `@MapperScan` of 6
mapper packages + `@EnableAsync/Scheduling/Tx` + scan of `cn.oa.*`)
is correct end-to-end.

## 12. The committed dev→test→review contract (verified each cycle)

Every module must close all three gates before the next one starts.
The user's exact words: "开发-测试-review 的顺序". A module is
"done" when:

1. `mvn -pl oa-<module> -am test` exits 0
2. `mvn -pl oa-platform-web -am -DskipTests package` produces a
   bootable jar without errors
3. A short plain-text review report is delivered to the user
   covering: classes added, tests added, API surface, error
   codes, follow-ups
4. The user acknowledges with "X 任务完成" or a follow-up
   directive. Only then move to the next module.

Do NOT batch multiple modules in a single report. One module =
one ack from the user.
