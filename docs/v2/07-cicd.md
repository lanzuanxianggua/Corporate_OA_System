# 07 - Corporate OA System v2 CI/CD

> 版本: v2.0-draft
> 日期: 2026-06-04
> 状态: **Phase 1 设计中**
> 前置阅读: `00-index.md`、`01-architecture.md`、`06-testing.md`

---

## 1. 流水线总览

```
┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌────────────┐
│  本地开发  │ → │   PR 检查  │ → │  CI 主干   │ → │  Staging   │ → │  Production│
│            │   │            │   │            │   │            │   │            │
│ - 格式化   │   │ - Lint     │   │ - 单元测试 │   │ - 集成测试 │   │ - 蓝绿部署 │
│ - 单元测试 │   │ - 单元测试 │   │ - 构建 jar │   │ - 烟测     │   │ - 监控     │
│ - 提交     │   │ - CodeQL   │   │ - 镜像构建 │   │ - UAT      │   │ - 回滚预案 │
└────────────┘   └────────────┘   └────────────┘   └────────────┘   └────────────┘
     husky         GitHub           GitHub          K8s/Compose       K8s/Compose
     10s           Actions          Actions         5min              2min
```

---

## 2. 本地开发

### 2.1 工具链

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17 | 后端 |
| Maven | 3.9+ | 后端构建 |
| Node | 20 LTS | 前端 |
| pnpm | 9+ | 前端包管理 |
| Docker | 24+ | 容器 |
| docker-compose | 2.x | 本地多服务 |
| Husky | 9+ | Git hooks |
| Commitlint | 19+ | 提交规范 |
| pre-commit | 3.x | 提交前检查 |

### 2.2 本地服务（docker-compose.dev.yml）

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: oa-mysql
    ports: ['3306:3306']
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: oa_system_v2
    volumes:
      - mysql-data:/var/lib/mysql
      - ./code/backend/sql/v2/migration:/docker-entrypoint-initdb.d
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: oa-redis
    ports: ['6379:6379']
    volumes:
      - redis-data:/data

  mailhog:
    image: mailhog/mailhog
    container_name: oa-mailhog
    ports: ['1025:1025', '8025:8025']

volumes:
  mysql-data:
  redis-data:
```

### 2.3 启动命令

```bash
# 启动依赖
docker compose -f docker-compose.dev.yml up -d

# 后端开发
cd code/backend
mvn -pl oa-platform-web -am spring-boot:run

# 前端开发
cd code/frontend
pnpm dev
```

### 2.4 提交规范

**Commitlint 配置**（`.commitlintrc.cjs`）：

```js
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'docs', 'style', 'refactor',
      'perf', 'test', 'build', 'ci', 'chore', 'revert'
    ]],
    'subject-max-length': [2, 'always', 100],
  },
};
```

**提交格式**：
```
<type>(<scope>): <subject>

<body>

<footer>
```

**示例**：
```
feat(hr-leave): 添加请假撤回接口

- POST /api/v1/hr-leave/leaves/{id}/actions/revoke
- 仅 DRAFT/RUNNING 状态可撤回
- 撤回后状态变为 REVOKED
- 关联工作流实例终止

Closes #123
```

**type 含义**：
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式
- `refactor`: 重构
- `perf`: 性能
- `test`: 测试
- `build`: 构建
- `ci`: CI/CD
- `chore`: 杂项
- `revert`: 回滚

**scope 范围**（按模块）：
- `hr-leave` `hr-attendance` `hr-employee` ...
- `workflow` `platform-common` `platform-security` ...
- `frontend` `database` `docs`

### 2.5 Pre-commit 钩子

```yaml
# .husky/pre-commit
- name: Java 格式化
  run: mvn spotless:apply
  
- name: 前端 Lint
  run: cd code/frontend && pnpm lint:fix
  
- name: 前端格式化
  run: cd code/frontend && pnpm format
```

### 2.6 Pre-push 钩子

```yaml
# .husky/pre-push
- name: 单元测试
  run: mvn test -pl oa-platform-common,oa-hr-leave
  
- name: 前端单元测试
  run: cd code/frontend && pnpm test:unit
```

---

## 3. PR 检查（Pull Request）

### 3.1 GitHub Actions 配置

`.github/workflows/pr-check.yml`：

```yaml
name: PR Check

on:
  pull_request:
    branches: [main, develop]

jobs:
  backend-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      
      - name: Cache Maven
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Spotless Check
        run: mvn spotless:check
      
      - name: Checkstyle
        run: mvn checkstyle:check
  
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17, distribution: temurin }
      - uses: actions/cache@v4
        with: { path: ~/.m2, key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }} }
      
      - name: Unit Test
        run: mvn -pl '!frontend' -Dtest='*Test,!IT' test
      
      - name: Coverage Report
        run: mvn -pl '!frontend' jacoco:report
      
      - name: Coverage Gate
        run: |
          COVERAGE=$(grep -o 'covered.*covered' target/site/jacoco/index.html | head -1)
          echo "Coverage: $COVERAGE"
          # 校验不通过则 fail
          mvn -pl '!frontend' jacoco:check
  
  frontend-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with: { version: 9 }
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      
      - name: Install
        run: cd code/frontend && pnpm install --frozen-lockfile
      
      - name: Lint
        run: cd code/frontend && pnpm lint
      
      - name: Type Check
        run: cd code/frontend && pnpm type-check
  
  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with: { version: 9 }
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      
      - name: Install
        run: cd code/frontend && pnpm install --frozen-lockfile
      
      - name: Unit Test
        run: cd code/frontend && pnpm test:unit --coverage
      
      - name: Coverage Gate
        run: cd code/frontend && pnpm test:coverage:check
  
  codeql:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: github/codeql-action/init@v3
        with: { languages: java, javascript, typescript }
      - uses: github/codeql-action/analyze@v3
  
  sonar:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: sonarsource/sonarqube-scan-action@v2
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
  
  build:
    runs-on: ubuntu-latest
    needs: [backend-lint, backend-test, frontend-lint, frontend-test, codeql, sonar]
    steps:
      - uses: actions/checkout@v4
      - name: Build
        run: mvn -pl '!frontend' -DskipTests package
      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: backend-jar
          path: code/backend/oa-platform-web/target/*.jar
```

### 3.2 PR 必填项

- [ ] 至少 1 个 Reviewer approve
- [ ] 所有 CI 检查通过
- [ ] 分支与主干无冲突
- [ ] 描述清晰（what & why）
- [ ] 关联 Issue（如有）
- [ ] 无 force-push
- [ ] Commit message 符合规范

---

## 4. CI 主干（持续集成）

### 4.1 触发条件

- Push 到 `main` / `develop`
- PR 合并

### 4.2 流水线

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      # 完整构建
      - name: Build with Maven
        run: mvn -pl '!frontend' -DskipTests package
      
      # 单元测试 + 集成测试
      - name: Unit + Integration Test
        run: mvn -pl '!frontend' verify
      
      # 前端构建
      - name: Build Frontend
        run: |
          cd code/frontend
          pnpm install --frozen-lockfile
          pnpm build
      
      # 镜像构建并推送
      - name: Build & Push Backend Image
        run: |
          docker build -t ${{ secrets.REGISTRY }}/oa-backend:${{ github.sha }} code/backend
          docker push ${{ secrets.REGISTRY }}/oa-backend:${{ github.sha }}
      
      - name: Build & Push Frontend Image
        run: |
          docker build -t ${{ secrets.REGISTRY }}/oa-frontend:${{ github.sha }} code/frontend
          docker push ${{ secrets.REGISTRY }}/oa-frontend:${{ github.sha }}
      
      # 部署到 Staging
      - name: Deploy to Staging
        if: github.ref == 'refs/heads/develop'
        run: |
          kubectl set image deployment/oa-backend backend=${{ secrets.REGISTRY }}/oa-backend:${{ github.sha }} -n staging
          kubectl set image deployment/oa-frontend frontend=${{ secrets.REGISTRY }}/oa-frontend:${{ github.sha }} -n staging
```

### 4.3 测试报告

- **Allure Report** 自动生成
- **JaCoCo** 后端覆盖率
- **Codecov** 前后端统一覆盖率
- **SonarQube** 代码质量
- **CodeQL** 安全扫描

---

## 5. 镜像构建

### 5.1 后端 Dockerfile

**多阶段构建**（`code/backend/Dockerfile`）：

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY oa-platform-common/pom.xml oa-platform-common/
COPY oa-platform-security/pom.xml oa-platform-security/
COPY oa-workflow/pom.xml oa-workflow/
# ... 其他模块 pom
COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl oa-platform-web -am -DskipTests package

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /build/oa-platform-web/target/oa-platform-web-*.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

### 5.2 前端 Dockerfile

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /build
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

# Stage 2: Runtime
FROM nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1
```

### 5.3 docker-compose.yml（生产）

```yaml
version: '3.8'

services:
  backend:
    image: ${REGISTRY}/oa-backend:${TAG}
    restart: unless-stopped
    ports: ['8080:8080']
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/oa_system_v2?useSSL=true
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      OA_AUTH_JWT_SECRET: ${JWT_SECRET}
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_healthy }
    healthcheck:
      test: ['CMD', 'wget', '--quiet', '--tries=1', '--spider', 'http://localhost:8080/actuator/health']
      interval: 30s
      timeout: 3s
      retries: 3
  
  frontend:
    image: ${REGISTRY}/oa-frontend:${TAG}
    restart: unless-stopped
    ports: ['80:80', '443:443']
    volumes:
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - backend
  
  mysql:
    image: mysql:8.0
    restart: unless-stopped
    volumes:
      - mysql-data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: oa_system_v2
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ['CMD', 'mysqladmin', 'ping']
      interval: 10s
  
  redis:
    image: redis:7-alpine
    restart: unless-stopped
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ['CMD', 'redis-cli', 'ping']
      interval: 10s

volumes:
  mysql-data:
  redis-data:
```

---

## 6. Staging 部署

### 6.1 触发

- Push to `develop` 分支
- 手动触发（GitHub Actions UI）

### 6.2 流程

1. 镜像推送至 Registry
2. Helm/K8s 滚动更新
3. 等待 1 分钟
4. 跑烟测（10 个关键接口）
5. 通知 Slack

### 6.3 烟测脚本

```bash
# scripts/smoke-test.sh
#!/bin/bash
set -e

BASE_URL=${STAGING_URL:-https://staging.oa.example.com}
TOKEN=$(curl -s -X POST "$BASE_URL/api/v1/platform/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"smoke_test","password":"test123"}' | jq -r '.data.accessToken')

# 1. 健康检查
curl -s "$BASE_URL/actuator/health" | jq -e '.status == "UP"' > /dev/null

# 2. 获取当前用户
curl -s "$BASE_URL/api/v1/platform/auth/me" -H "Authorization: Bearer $TOKEN" | jq -e '.code == 0' > /dev/null

# 3. 查询请假列表
curl -s "$BASE_URL/api/v1/hr-leave/leaves" -H "Authorization: Bearer $TOKEN" | jq -e '.code == 0' > /dev/null

# 4. 提交请假
RESULT=$(curl -s -X POST "$BASE_URL/api/v1/hr-leave/leaves" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"leaveType":"ANNUAL","startTime":"2026-07-01T09:00:00","endTime":"2026-07-02T18:00:00","leavePeriod":"FULL","reason":"smoke"}')
echo $RESULT | jq -e '.code == 0' > /dev/null

# 5. 我的待办
curl -s "$BASE_URL/api/v1/workflow/tasks?assignee=me" -H "Authorization: Bearer $TOKEN" | jq -e '.code == 0' > /dev/null

echo "✓ Smoke test passed"
```

---

## 7. Production 部署

### 7.1 触发

- 手动触发（GitHub Actions UI）
- 需维护者 approval

### 7.2 流程

1. 选择发布版本（Git Tag）
2. **蓝绿部署**（无停机）
3. 等待 5 分钟观察
4. 切流量
5. 保留旧版本 24h（便于回滚）

### 7.3 蓝绿部署

```bash
# scripts/blue-green-deploy.sh
#!/bin/bash
set -e

TAG=$1
ENV=${2:-prod}

# 当前颜色
CURRENT=$(kubectl get service oa-backend -o jsonpath='{.spec.selector.color}' -n $ENV)
NEW=$([ "$CURRENT" = "blue" ] && echo "green" || echo "blue")

echo "Deploying $TAG to $NEW..."

# 部署新版本
kubectl set image deployment/oa-backend-$NEW backend=$REGISTRY/oa-backend:$TAG -n $ENV
kubectl rollout status deployment/oa-backend-$NEW -n $ENV --timeout=5m

# 切流量
kubectl patch service oa-backend -p '{"spec":{"selector":{"color":"'$NEW'"}}}' -n $ENV

echo "✓ Deployed to $NEW"
echo "Old $CURRENT kept for 24h, rollback: kubectl patch service oa-backend -p '{\"spec\":{\"selector\":{\"color\":\"'$CURRENT'\"}}}' -n $ENV"
```

### 7.4 回滚预案

**自动回滚触发**：
- 部署后 5 分钟内 5xx 错误率 > 1%
- 健康检查连续 3 次失败
- 关键接口 P99 > 5s

**手动回滚**：
```bash
# 列出最近版本
kubectl rollout history deployment/oa-backend -n prod

# 回滚到上一版本
kubectl rollout undo deployment/oa-backend -n prod

# 回滚到指定版本
kubectl rollout undo deployment/oa-backend --to-revision=5 -n prod
```

---

## 8. 监控与告警

### 8.1 Prometheus 指标

`/actuator/prometheus` 暴露：
- JVM 指标（heap/threads/GC）
- HTTP 指标（请求数/响应时间/状态码）
- 数据库指标（连接池/HikariCP）
- 自定义业务指标

### 8.2 Grafana 仪表板

- **应用总览**：QPS/错误率/响应时间
- **业务指标**：请假数/审批时长/活跃用户
- **基础设施**：CPU/内存/磁盘/网络

### 8.3 告警规则

```yaml
# alert.rules.yml
groups:
  - name: oa-system
    rules:
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.01
        for: 5m
        labels: { severity: critical }
        annotations:
          summary: "错误率过高 (>1%)"
      
      - alert: SlowResponse
        expr: |
          histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)) > 2
        for: 10m
        labels: { severity: warning }
        annotations:
          summary: "P99 响应时间 > 2s"
      
      - alert: HighJvmMemory
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels: { severity: warning }
      
      - alert: DbConnectionExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels: { severity: critical }
```

### 8.4 告警渠道

- **Slack** 通知频道 `#oa-alerts`
- **PagerDuty** P0 级（24/7）
- **邮件** 备份

---

## 9. 数据库迁移（Flyway）

### 9.1 集成

```xml
<!-- pom.xml (oa-platform-web) -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>10.20.0</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
    <version>10.20.0</version>
</dependency>
```

### 9.2 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
```

### 9.3 迁移脚本命名

```
V{version}__{description}.sql
```

**版本号**：
- 主版本：100-200（按 Phase 段划分）
- 修复版本：5xx

**示例**：
- `V100__init_platform.sql`
- `V101__init_workflow.sql`
- `V102__init_hr_leave.sql`
- `V205__add_employee_index.sql`（修复）

### 9.4 CI 验证

- 启动时 `validate-on-migrate: true`
- 测试库先跑：必须成功
- 生产应用启动前必须跑：失败则启动失败

### 9.5 回滚

Flyway 不支持 down 迁移（默认）。回滚策略：
- 创建新的 `V{next}__rollback_xxx.sql` 反向脚本
- 手动执行 SQL（不通过应用）

---

## 10. Secrets 管理

### 10.1 GitHub Secrets

- `REGISTRY` Docker Registry 地址
- `SONAR_TOKEN` SonarQube Token
- `CODECOV_TOKEN` Codecov Token
- `KUBE_CONFIG_STAGING` K8s 配置
- `KUBE_CONFIG_PROD` K8s 配置

### 10.2 应用 Secrets

- **DB 密码**：K8s Secret
- **JWT Secret**：K8s Secret
- **第三方 API Key**：K8s Secret

```bash
# 创建 Secret
kubectl create secret generic oa-secrets \
  --from-literal=db-password=xxx \
  --from-literal=jwt-secret=xxx \
  -n oa-system
```

### 10.3 加密备份

所有 Secret 必须加密存储（Sealed Secrets / External Secrets Operator）。

---

## 11. 总结

v2 CI/CD 核心：
1. **本地优先**：Husky + Commitlint + docker-compose
2. **PR Gate**：lint + 单元测试 + CodeQL + Sonar
3. **主干集成**：构建 + 集成测试 + 镜像 + 部署
4. **蓝绿部署**：零停机发布
5. **自动回滚**：基于监控告警
6. **可观测**：Prometheus + Grafana + 告警
7. **可审计**：Allure + Sonar + Codecov

不实现：
- Canary 部署（v3 考虑）
- Feature Flag（v3 考虑）
- A/B 测试（v3 考虑）
