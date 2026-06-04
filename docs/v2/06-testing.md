# 06 - Corporate OA System v2 测试规范

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`

---

## 1. 测试金字塔

```
       ╱╲
      ╱  ╲          E2E (5%)
     ╱    ╲         - 关键业务场景端到端
    ╱──────╲        - Playwright
   ╱        ╲       - 慢, 脆弱, 但真实
  ╱──────────╲      集成测试 (25%)
 ╱            ╲     - Controller + Service + Mapper
╱──────────────╲    - Testcontainers 真实数据库
                  单元测试 (70%)
                  - Service 业务逻辑
                  - JUnit 5 + Mockito
                  - 快速, 大量
```

**v2 目标**：
- 单元测试覆盖率 > 80%（Service 层）
- 集成测试覆盖所有 Controller happy path
- E2E 覆盖所有 P0 业务场景

---

## 2. 测试分层

### 2.1 单元测试（Unit Test）

**目标**：
- 测试单个类的方法逻辑
- 隔离外部依赖（数据库/Redis/HTTP）
- 快速执行（< 1s/test）

**工具**：
- JUnit 5（`@Test`、`@ParameterizedTest`、`@Nested`）
- Mockito（`@Mock`、`@InjectMocks`）
- AssertJ（流式断言）
- JsonPath（JSON 断言）
- Awaitility（异步断言）

**位置**：`src/test/java/cn/oa/{module}/`

**示例**：

```java
@ExtendWith(MockitoExtension.class)
class HrLeaveServiceImplTest {
    
    @Mock private HrLeaveMapper leaveMapper;
    @Mock private HrLeaveBalanceService balanceService;
    @Mock private WfEngineService wfEngine;
    @InjectMocks private HrLeaveServiceImpl leaveService;
    
    @Test
    void shouldCreateLeaveWhenBalanceSufficient() {
        // Given
        HrLeaveCreateDTO dto = HrLeaveCreateDTO.builder()
            .leaveType("ANNUAL")
            .startTime(LocalDateTime.of(2026, 6, 10, 9, 0))
            .endTime(LocalDateTime.of(2026, 6, 12, 18, 0))
            .leavePeriod("FULL")
            .reason("回家")
            .build();
        
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setTotalDays(new BigDecimal("10.0"));
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setFrozenDays(BigDecimal.ZERO);
        
        when(balanceService.getBalance(anyLong(), eq("ANNUAL"), anyInt()))
            .thenReturn(balance);
        when(leaveMapper.insert(any(HrLeave.class)))
            .thenReturn(1);
        when(wfEngine.startProcess(anyString(), anyLong(), any()))
            .thenReturn(100L);
        
        // When
        LeaveCreateResult result = leaveService.create(dto, 123L);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isPositive();
        assertThat(result.getDays()).isEqualByComparingTo("3.0");
        verify(leaveMapper).insert(any(HrLeave.class));
        verify(wfEngine).startProcess(eq("HR_LEAVE"), anyLong(), any());
    }
    
    @Test
    void shouldThrowInsufficientBalanceWhenNotEnough() {
        // Given
        HrLeaveCreateDTO dto = HrLeaveCreateDTO.builder()
            .leaveType("ANNUAL")
            .days(new BigDecimal("100"))
            .build();
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setTotalDays(new BigDecimal("10.0"));
        
        when(balanceService.getBalance(anyLong(), eq("ANNUAL"), anyInt()))
            .thenReturn(balance);
        
        // When + Then
        assertThatThrownBy(() -> leaveService.create(dto, 123L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("余额不足");
    }
    
    @Test
    void shouldThrowOverlapWhenDatesOverlapped() {
        // Given
        HrLeaveCreateDTO dto = HrLeaveCreateDTO.builder()
            .leaveType("ANNUAL")
            .startTime(LocalDateTime.of(2026, 6, 10, 9, 0))
            .endTime(LocalDateTime.of(2026, 6, 12, 18, 0))
            .build();
        
        when(leaveMapper.countOverlap(anyLong(), any(), any(), any()))
            .thenReturn(1L);
        
        // When + Then
        assertThatThrownBy(() -> leaveService.create(dto, 123L))
            .isInstanceOf(BizException.class)
            .hasMessageContaining("日期重叠");
    }
}
```

### 2.2 集成测试（Integration Test）

**目标**：
- 测试多个组件协作
- 真实数据库/Redis（Testcontainers）
- 较慢（每个测试 1-5s）

**工具**：
- Spring Boot Test（`@SpringBootTest`）
- Testcontainers MySQL/Redis
- MockMvc（Web 模拟）
- RestAssured（HTTP 测试）

**位置**：`src/test/java/cn/oa/{module}/api/`（命名 `*IT.java`）

**示例**：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class HrLeaveApiIT {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("oa_test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JwtTestHelper jwtHelper;
    @Autowired private HrLeaveMapper leaveMapper;
    
    @Test
    void shouldCreateLeaveAndStartWorkflow() {
        // Given
        String token = jwtHelper.generateToken(123L, "EMPLOYEE");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        HrLeaveCreateDTO dto = HrLeaveCreateDTO.builder()
            .leaveType("ANNUAL")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(3))
            .leavePeriod("FULL")
            .reason("回家")
            .build();
        
        // When
        ResponseEntity<R<LeaveCreateResult>> response = restTemplate
            .exchange("/api/v1/hr-leave/leaves", HttpMethod.POST,
                new HttpEntity<>(dto, headers),
                new ParameterizedTypeReference<>() {});
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getData().getId()).isPositive();
        
        // Verify database
        HrLeave saved = leaveMapper.selectById(response.getBody().getData().getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("RUNNING");
    }
    
    @Test
    void shouldRejectInsufficientBalance() {
        // Given: no balance
        String token = jwtHelper.generateToken(456L, "EMPLOYEE");
        // ... (no balance set up)
        
        // When + Then
        // 422 + HL002
    }
}
```

### 2.3 端到端测试（E2E）

**目标**：
- 模拟真实用户操作
- 验证前后端集成
- 慢（每场景 10-60s）

**工具**：
- Playwright
- 后端启动在 `:8080`
- 前端启动在 `:3000`
- 数据库: Testcontainers

**位置**：`code/frontend/e2e/`

**示例**：

```ts
// e2e/hr-leave.spec.ts
import { test, expect } from '@playwright/test';

test.describe('HR 请假 - 端到端', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'employee01');
    await page.fill('input[name="password"]', 'test123');
    await page.click('button[type="submit"]');
    await page.waitForURL('/');
  });
  
  test('员工提交请假 - 经理审批 - 完成', async ({ page, context }) => {
    // 1. 员工提交请假
    await page.click('text=HR 请假');
    await page.click('text=请假申请');
    await page.click('text=新建请假');
    
    await page.locator('select[name="leaveType"]').selectOption('ANNUAL');
    await page.locator('input[name="startTime"]').fill('2026-07-01 09:00:00');
    await page.locator('input[name="endTime"]').fill('2026-07-03 18:00:00');
    await page.locator('textarea[name="reason"]').fill('回家探亲');
    await page.click('button:has-text("提交")');
    
    await expect(page.locator('text=提交成功')).toBeVisible();
    
    // 2. 切换到经理账号审批
    const managerPage = await context.newPage();
    await managerPage.goto('/login');
    await managerPage.fill('input[name="username"]', 'manager01');
    await managerPage.fill('input[name="password"]', 'test123');
    await managerPage.click('button[type="submit"]');
    
    await managerPage.click('text=待办');
    await managerPage.click('text=请假申请');
    await managerPage.click('text=通过');
    await managerPage.click('button:has-text("确认")');
    
    // 3. 验证员工看到审批通过
    await page.bringToFront();
    await page.reload();
    await expect(page.locator('text=已通过').first()).toBeVisible();
  });
  
  test('余额不足时提示', async ({ page }) => {
    // ... 期望看到"余额不足"提示
  });
  
  test('日期重叠时拒绝', async ({ page }) => {
    // ... 期望看到"日期重叠"提示
  });
});
```

---

## 3. 工具与库

### 3.1 后端测试栈

| 工具 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10+ | 测试框架 |
| Mockito | 5.x | 模拟 |
| AssertJ | 3.25+ | 断言 |
| Testcontainers | 1.20+ | 真实容器 |
| Spring Boot Test | 3.4+ | 集成测试 |
| RestAssured | 5.5+ | HTTP 客户端测试 |
| WireMock | 3.x | HTTP 模拟 |
| JaCoCo | 0.8.12 | 覆盖率 |
| Allure | 2.27+ | 测试报告 |

### 3.2 前端测试栈

| 工具 | 版本 | 用途 |
|------|------|------|
| Vitest | 2.x | 单元测试 |
| @vue/test-utils | 2.x | 组件测试 |
| @playwright/test | 1.x | E2E |
| @testing-library/vue | 8.x | 组件测试 |
| happy-dom | 15.x | DOM 模拟 |
| Allure | - | 报告 |

---

## 4. 覆盖率要求

### 4.1 覆盖率指标

| 模块 | 单元 | 集成 | E2E | 总目标 |
|------|------|------|-----|--------|
| oa-platform-common | 90% | 50% | - | 85% |
| oa-platform-security | 90% | 80% | - | 85% |
| oa-workflow | 80% | 70% | 30% | 75% |
| 业务模块 Service | 80% | 60% | - | 75% |
| 业务模块 Controller | - | 80% | - | 80% |
| 业务模块 Mapper | - | 70% | - | 70% |
| 整体 | 80% | 60% | 30% | 75% |

### 4.2 JaCoCo 配置

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                    <limit>
                        <counter>BRANCH</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.70</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### 4.3 覆盖率不达标处理

CI 流水线必须 fail if coverage < 80%。

**豁免规则**：
- 生成的代码（lombok、mapstruct）不计入
- 配置类（`@Configuration`）不强制
- 异常分支（无法触发的）可排除

---

## 5. 测试数据管理

### 5.1 测试数据原则

- **每个测试独立**：不依赖其他测试的数据
- **可重复执行**：每次执行结果一致
- **快速创建**：用工厂方法或 fixtures
- **不污染生产**：用 Testcontainers 隔离

### 5.2 TestDataFactory

```java
public class HrLeaveTestDataFactory {
    
    public static HrLeaveCreateDTO createValidDto() {
        return HrLeaveCreateDTO.builder()
            .leaveType("ANNUAL")
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(3))
            .leavePeriod("FULL")
            .reason("测试请假")
            .build();
    }
    
    public static HrLeave createValidLeave(Long empId) {
        HrLeave leave = new HrLeave();
        leave.setEmpId(empId);
        leave.setLeaveType("ANNUAL");
        leave.setStatus(LeaveStatus.DRAFT.name());
        leave.setDays(new BigDecimal("3.0"));
        return leave;
    }
    
    public static HrLeaveBalance createValidBalance(Long empId, String leaveType) {
        HrLeaveBalance balance = new HrLeaveBalance();
        balance.setEmpId(empId);
        balance.setLeaveType(leaveType);
        balance.setYear(LocalDate.now().getYear());
        balance.setTotalDays(new BigDecimal("10.0"));
        balance.setUsedDays(BigDecimal.ZERO);
        balance.setFrozenDays(BigDecimal.ZERO);
        balance.setRemainingDays(new BigDecimal("10.0"));
        balance.setStatus("ACTIVE");
        return balance;
    }
}
```

### 5.3 SQL Fixtures

**位置**：`src/test/resources/data/`

**加载**：
```java
@Sql(scripts = "/data/clean.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/data/hr-leave-init.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/data/clean.sql", executionPhase = AFTER_TEST_METHOD)
```

---

## 6. Mock 策略

### 6.1 必须 Mock
- 外部 HTTP（用 WireMock）
- 第三方 SDK
- 时间（用 `Clock` 抽象）
- 当前用户（用 `UserContext.set`）

### 6.2 不要 Mock
- 数据库（用 Testcontainers 真实库）
- Redis（用 Testcontainers 真实 Redis）
- 工作流引擎（用真实实现）

### 6.3 Mock 工具选择

| 场景 | 工具 |
|------|------|
| 类/接口 | Mockito |
| 静态方法 | Mockito 5+ `mockStatic` |
| 第三方 HTTP | WireMock |
| 时间 | `Clock` 注入 |
| 当前用户 | `UserContext` ThreadLocal |

---

## 7. CI 中的测试

### 7.1 测试阶段

```yaml
# .github/workflows/ci.yml
jobs:
  test:
    steps:
      - name: Unit Test
        run: mvn -pl '!frontend' test
        
      - name: Integration Test (Testcontainers)
        run: mvn -pl '!frontend' verify -Pintegration-test
        
      - name: Frontend Unit Test
        run: cd code/frontend && pnpm test:unit
        
      - name: E2E Test
        run: |
          docker compose -f docker-compose.test.yml up -d
          cd code/frontend && pnpm test:e2e
          docker compose -f docker-compose.test.yml down
          
      - name: Coverage Report
        run: mvn jacoco:report
        
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v4
```

### 7.2 测试报告

- **后端**：Surefire + Allure
- **前端**：Vitest + Allure
- **E2E**：Playwright HTML 报告
- **统一展示**：Allure Report 服务

---

## 8. 性能测试

### 8.1 JMeter 脚本

**位置**：`code/perf/jmeter/`

**覆盖场景**：
- 登录：1000 并发
- 我的待办：500 并发
- 提交请假：100 并发（写）
- 请假列表：1000 并发

**性能指标**：
- P50 < 200ms
- P95 < 500ms
- P99 < 1s
- 错误率 < 0.1%

### 8.2 Locust 脚本（备选）

```python
# perf/locustfile.py
from locust import HttpUser, task, between

class OaUser(HttpUser):
    wait_time = between(1, 3)
    
    def on_start(self):
        self.client.post("/api/v1/platform/auth/login", json={
            "username": "perf_test",
            "password": "test123"
        })
    
    @task(3)
    def get_my_leaves(self):
        self.client.get("/api/v1/hr-leave/leaves?empId=me")
    
    @task(1)
    def create_leave(self):
        self.client.post("/api/v1/hr-leave/leaves", json={
            "leaveType": "ANNUAL",
            "startTime": "2026-07-01T09:00:00",
            "endTime": "2026-07-03T18:00:00",
            "leavePeriod": "FULL",
            "reason": "性能测试"
        })
```

---

## 9. 测试规范与约定

### 9.1 命名

- 测试类：`{ClassUnderTest}Test`（单元）/ `{ClassUnderTest}IT`（集成）
- 测试方法：`should{ExpectedBehavior}When{Condition}`
- 数据工厂：`{Entity}TestDataFactory`
- Mock 类：`Mock{Name}`

### 9.2 注解顺序

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("请假 Service 单元测试")
class HrLeaveServiceImplTest {
    
    @Nested
    @DisplayName("创建请假")
    class CreateLeave {
        
        @Test
        @DisplayName("余额充足时创建成功")
        @Tag("smoke")
        void shouldCreateLeaveWhenBalanceSufficient() {
            // ...
        }
    }
}
```

### 9.3 Given-When-Then

```java
@Test
void should() {
    // Given (准备)
    // ...
    
    // When (执行)
    // ...
    
    // Then (断言)
    assertThat(...);
}
```

### 9.4 一个测试一个断言

- 多个相关断言放在一个 `assertAll` 里
- 不相关的拆成多个测试

### 9.5 测试不能依赖外部服务

- 不连生产库
- 不连外部 HTTP
- 不依赖文件系统特定路径

---

## 10. 缺陷管理

### 10.1 缺陷分级

| 级别 | 描述 | SLA |
|------|------|-----|
| P0 | 阻塞主流程，数据丢失/损坏 | 24h |
| P1 | 主要功能不可用 | 3d |
| P2 | 次要功能不可用 | 7d |
| P3 | 体验问题 | 14d |
| P4 | 文案/建议 | 30d |

### 10.2 缺陷追踪

- GitHub Issues
- 标签：bug, P0, P1, P2, P3, P4
- 关联 PR
- 测试用例（防止回归）

---

## 11. 持续测试

### 11.1 预提交测试

- Husky 钩子：pre-commit 跑 lint
- 不跑测试（太慢）

### 11.2 推送测试

- GitHub Actions 触发
- 跑单元测试 + 集成测试
- ~10-15 分钟

### 11.3 PR 合并测试

- 必须所有检查通过
- 至少 1 个 reviewer approve
- 覆盖率不下降

### 11.4 Nightly 测试

- 每日 0:00 跑
- 包括性能测试
- 报告发到 Slack

---

## 12. 总结

v2 测试规范核心：
1. **金字塔比例**：70% 单元 + 25% 集成 + 5% E2E
2. **覆盖率**：Service 层 > 80%
3. **真实环境**：Testcontainers
4. **自动化**：CI 跑全量
5. **可读性**：Given-When-Then + 描述性方法名
6. **性能**：JMeter/Locust 关键接口
7. **报告**：Allure
