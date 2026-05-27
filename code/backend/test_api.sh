#!/bin/bash
# ============================================================
#  OA系统 API自动化测试脚本
#  后端地址: http://localhost:8080
#  用法: bash test_api.sh [BASE_URL]
# ============================================================

BASE="${1:-http://localhost:8080}"
PASS=0
FAIL=0
TOTAL=0
RESULTS=""
TOKEN=""
REFRESH_TOKEN=""
EMP_ID=""

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ============================================================
#  工具函数
# ============================================================

# JSON值提取（不依赖jq，使用grep/sed）
json_val() {
    local json="$1"
    local key="$2"
    echo "$json" | grep -o "\"$key\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"//" | sed 's/"$//'
}

json_num() {
    local json="$1"
    local key="$2"
    echo "$json" | grep -o "\"$key\"[[:space:]]*:[[:space:]]*[-0-9]*" | head -1 | sed "s/.*\"$key\"[[:space:]]*:[[:space:]]*//"
}

# 测试单个API端点
test_api() {
    local method="$1"
    local path="$2"
    local token="$3"
    local body="$4"
    local expect_code="$5"
    local desc="$6"

    TOTAL=$((TOTAL + 1))

    local header_args=""
    if [ -n "$token" ]; then
        header_args="-H \"Authorization: Bearer $token\""
    fi

    local method_args=""
    if [ "$method" = "POST" ]; then
        method_args="-X POST"
        if [ -n "$body" ]; then
            method_args="$method_args -H \"Content-Type: application/json\" -d '$body'"
        fi
    elif [ "$method" = "PUT" ]; then
        method_args="-X PUT"
        if [ -n "$body" ]; then
            method_args="$method_args -H \"Content-Type: application/json\" -d '$body'"
        fi
    elif [ "$method" = "DELETE" ]; then
        method_args="-X DELETE"
    fi

    local cmd="curl -s -w '\\n%{http_code}' $header_args $method_args \"$BASE$path\""
    local result
    result=$(eval "$cmd" 2>/dev/null)
    local http_code
    http_code=$(echo "$result" | tail -1)
    local body_result
    body_result=$(echo "$result" | sed '$d')

    # 检查业务code（JSON中的code字段）
    local biz_code
    biz_code=$(json_num "$body_result" "code")

    local passed=false
    if [ "$http_code" = "$expect_code" ]; then
        passed=true
        # 如果HTTP状态码匹配，再检查业务code是否为0（成功）
        if [ -n "$biz_code" ] && [ "$biz_code" != "0" ] && [ "$biz_code" != "401" ]; then
            # 业务code不为0，算失败（除非预期就是非0）
            passed=false
        fi
    fi

    if $passed; then
        PASS=$((PASS + 1))
        RESULTS="${RESULTS}\n${GREEN}[PASS]${NC} $method $path - $desc"
        echo -e "  ${GREEN}[PASS]${NC} $desc"
    else
        FAIL=$((FAIL + 1))
        RESULTS="${RESULTS}\n${RED}[FAIL]${NC} $method $path - $desc (expected HTTP $expect_code, got $http_code, biz_code=$biz_code)"
        echo -e "  ${RED}[FAIL]${NC} $desc (HTTP=$http_code, biz_code=$biz_code)"
    fi
}

# 仅测试HTTP状态码（用于预期可能失败的接口）
test_api_http() {
    local method="$1"
    local path="$2"
    local token="$3"
    local body="$4"
    local expect_code="$5"
    local desc="$6"

    TOTAL=$((TOTAL + 1))

    local header_args=""
    if [ -n "$token" ]; then
        header_args="-H \"Authorization: Bearer $token\""
    fi

    local method_args=""
    if [ "$method" = "POST" ]; then
        method_args="-X POST"
        if [ -n "$body" ]; then
            method_args="$method_args -H \"Content-Type: application/json\" -d '$body'"
        fi
    fi

    local cmd="curl -s -w '\\n%{http_code}' $header_args $method_args \"$BASE$path\""
    local result
    result=$(eval "$cmd" 2>/dev/null)
    local http_code
    http_code=$(echo "$result" | tail -1)

    if [ "$http_code" = "$expect_code" ]; then
        PASS=$((PASS + 1))
        RESULTS="${RESULTS}\n${GREEN}[PASS]${NC} $method $path - $desc"
        echo -e "  ${GREEN}[PASS]${NC} $desc"
    else
        FAIL=$((FAIL + 1))
        RESULTS="${RESULTS}\n${RED}[FAIL]${NC} $method $path - $desc (expected HTTP $expect_code, got $http_code)"
        echo -e "  ${RED}[FAIL]${NC} $desc (HTTP=$http_code)"
    fi
}

# ============================================================
#  Step 1: 检查后端连通性
# ============================================================
echo -e "${CYAN}=========================================="
echo "  OA系统 API自动化测试"
echo -e "==========================================${NC}"
echo "后端地址: $BASE"
echo ""

echo -e "${YELLOW}[1/5] 检查后端连通性...${NC}"
health=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/auth/captcha" 2>/dev/null)
if [ "$health" != "200" ]; then
    echo -e "${RED}FATAL: 后端不可达 ($BASE 返回 HTTP $health)${NC}"
    echo "请确认后端已启动: java -jar oa-web/target/oa-web-1.0.0.jar"
    exit 1
fi
echo -e "  ${GREEN}后端连通正常${NC}"
echo ""

# ============================================================
#  Step 2: 获取验证码并登录
# ============================================================
echo -e "${YELLOW}[2/5] 获取验证码并登录...${NC}"

# 获取验证码
captcha_resp=$(curl -s "$BASE/api/auth/captcha")
uuid=$(json_val "$captcha_resp" "uuid")
img=$(json_val "$captcha_resp" "img")
biz_code=$(json_num "$captcha_resp" "code")

if [ "$biz_code" = "0" ] && [ -n "$uuid" ]; then
    echo -e "  ${GREEN}验证码获取成功${NC} (uuid: ${uuid:0:8}...)"
else
    echo -e "${RED}FATAL: 验证码获取失败: $captcha_resp${NC}"
    exit 1
fi

# 通过redis-cli直接读取验证码答案（key: captcha:{uuid}）
REDIS_CLI="${REDIS_CLI:-/c/Program Files/Redis/redis-cli.exe}"

echo -e "  读取验证码答案..."
LOGIN_SUCCESS=false

# 最多尝试3次获取验证码并读取答案
for attempt in 1 2 3; do
    captcha_resp=$(curl -s "$BASE/api/auth/captcha")
    uuid=$(json_val "$captcha_resp" "uuid")
    biz_code=$(json_num "$captcha_resp" "code")

    if [ "$biz_code" != "0" ] || [ -z "$uuid" ]; then
        echo -e "  ${YELLOW}验证码获取失败(第${attempt}次)，重试...${NC}"
        sleep 1
        continue
    fi

    # 从Redis直接读取答案
    captcha_answer=$("$REDIS_CLI" GET "captcha:$uuid" 2>/dev/null | tr -d '\r\n"')

    if [ -n "$captcha_answer" ]; then
        login_resp=$(curl -s -X POST "$BASE/login" \
            -H "Content-Type: application/json" \
            -d "{\"username\":\"admin\",\"password\":\"123456\",\"captchaUuid\":\"$uuid\",\"captchaCode\":\"$captcha_answer\"}" 2>/dev/null)

        resp_code=$(json_num "$login_resp" "code")

        if [ "$resp_code" = "0" ]; then
            TOKEN=$(json_val "$login_resp" "accessToken")
            REFRESH_TOKEN=$(json_val "$login_resp" "refreshToken")
            EMP_ID=$(json_num "$login_resp" "empId")
            echo -e "  ${GREEN}登录成功${NC} (验证码答案: $captcha_answer, empId: $EMP_ID)"
            LOGIN_SUCCESS=true
            break
        else
            msg=$(json_val "$login_resp" "message")
            echo -e "  ${YELLOW}登录失败(第${attempt}次): $msg，重试...${NC}"
        fi
    else
        echo -e "  ${YELLOW}Redis中未找到验证码答案(第${attempt}次)，重试...${NC}"
    fi
    sleep 2
done

if ! $LOGIN_SUCCESS; then
    echo -e "${RED}FATAL: 无法登录，跳过认证相关测试${NC}"
    echo -e "${YELLOW}将继续测试公开接口...${NC}"
    echo ""
fi

# ============================================================
#  Step 3: 测试公开接口（无需认证）
# ============================================================
echo ""
echo -e "${YELLOW}[3/5] 测试公开接口...${NC}"

# 验证码
test_api GET "/api/auth/captcha" "" "" "200" "获取验证码"

# 登录错误处理
echo -e "  ${CYAN}测试登录错误处理...${NC}"
captcha_resp=$(curl -s "$BASE/api/auth/captcha")
bad_uuid=$(json_val "$captcha_resp" "uuid")
test_api_http POST "/login" "" "{\"username\":\"admin\",\"password\":\"wrongpassword\",\"captchaUuid\":\"$bad_uuid\",\"captchaCode\":\"99999\"}" "200" "错误密码登录返回200(业务失败)"

# 无效验证码登录
test_api_http POST "/login" "" "{\"username\":\"admin\",\"password\":\"123456\",\"captchaUuid\":\"invalid-uuid\",\"captchaCode\":\"1\"}" "200" "无效验证码登录返回200(业务失败)"

echo ""

# ============================================================
#  Step 4: 测试认证接口
# ============================================================
echo -e "${YELLOW}[4/5] 测试认证接口...${NC}"

if [ -n "$TOKEN" ]; then
    # ---- 认证与用户 ----
    echo -e "  ${CYAN}--- 认证与用户 ---${NC}"
    test_api GET "/mine" "$TOKEN" "" "200" "获取当前用户信息"
    test_api GET "/get-async-routes" "$TOKEN" "" "200" "获取动态路由"
    test_api GET "/roles" "$TOKEN" "" "200" "获取角色列表"

    # ---- 员工管理 ----
    echo -e "  ${CYAN}--- 员工管理 ---${NC}"
    test_api GET "/api/employee/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "员工列表(分页)"
    if [ -n "$EMP_ID" ] && [ "$EMP_ID" != "0" ]; then
        test_api GET "/api/employee/$EMP_ID" "$TOKEN" "" "200" "员工详情"
    fi

    # ---- 部门管理 ----
    echo -e "  ${CYAN}--- 部门管理 ---${NC}"
    test_api GET "/api/dept/tree" "$TOKEN" "" "200" "部门树"

    # ---- 考勤管理 ----
    echo -e "  ${CYAN}--- 考勤管理 ---${NC}"
    test_api GET "/api/attendance/today" "$TOKEN" "" "200" "今日考勤"
    test_api GET "/api/attendance/history?startDate=2026-05-01&endDate=2026-05-25" "$TOKEN" "" "200" "考勤历史"
    test_api GET "/api/attendance/admin/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "管理员-考勤分页"

    # ---- 考勤组 ----
    test_api GET "/api/attendance-group/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "考勤组列表"

    # ---- 请假管理 ----
    echo -e "  ${CYAN}--- 请假管理 ---${NC}"
    test_api GET "/api/leave/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "请假列表"
    test_api GET "/api/leave-balance/my" "$TOKEN" "" "200" "我的假期余额"
    test_api GET "/api/leave-balance/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "假期余额管理"

    # ---- 审批流程（出差/外出/采购/经费） ----
    echo -e "  ${CYAN}--- 审批流程 ---${NC}"
    test_api GET "/api/business-trip/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "出差列表"
    test_api GET "/api/outing/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "外出列表"
    test_api GET "/api/purchase/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "采购列表"
    test_api GET "/api/expense/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "经费列表"

    # ---- 加班/借支 ----
    echo -e "  ${CYAN}--- 加班与借支 ---${NC}"
    test_api GET "/api/overtime/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "加班列表"
    test_api GET "/api/loan/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "借支列表"

    # ---- 公告管理 ----
    echo -e "  ${CYAN}--- 公告管理 ---${NC}"
    test_api GET "/api/notice/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "公告列表"

    # ---- 文档管理 ----
    echo -e "  ${CYAN}--- 文档管理 ---${NC}"
    test_api GET "/api/document/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "文档列表"

    # ---- 消息中心 ----
    echo -e "  ${CYAN}--- 消息中心 ---${NC}"
    test_api GET "/api/message/unread-count" "$TOKEN" "" "200" "未读消息数"
    test_api GET "/api/message/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "消息列表"

    # ---- 日程管理 ----
    echo -e "  ${CYAN}--- 日程管理 ---${NC}"
    test_api GET "/api/schedule/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "日程列表"

    # ---- 薪资管理 ----
    echo -e "  ${CYAN}--- 薪资管理 ---${NC}"
    test_api GET "/api/salary/structure/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "薪资结构"
    test_api GET "/api/salary/record/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "薪资记录"
    test_api GET "/api/salary/my" "$TOKEN" "" "200" "我的薪资"

    # ---- 员工档案 ----
    echo -e "  ${CYAN}--- 员工档案 ---${NC}"
    test_api GET "/api/emp-archive/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "员工档案列表"

    # ---- 待办事项 ----
    echo -e "  ${CYAN}--- 待办事项 ---${NC}"
    test_api GET "/api/todo/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "待办列表"
    test_api GET "/api/todo/count" "$TOKEN" "" "200" "待办计数"

    # ---- 工作流 ----
    echo -e "  ${CYAN}--- 工作流 ---${NC}"
    test_api GET "/api/workflow/definition/list" "$TOKEN" "" "200" "流程定义列表"
    test_api GET "/api/workflow/task/pending?pageNum=1&pageSize=10" "$TOKEN" "" "200" "待办任务"
    test_api GET "/api/workflow/history?businessType=LEAVE&businessId=1" "$TOKEN" "" "200" "流程历史(请假)"

    # ---- 数据看板 ----
    echo -e "  ${CYAN}--- 数据看板 ---${NC}"
    test_api GET "/api/statistics/dashboard" "$TOKEN" "" "200" "数据看板"

    # ---- 报表 ----
    echo -e "  ${CYAN}--- 报表 ---${NC}"
    test_api GET "/api/report/personal/attendance-summary?month=2026-05" "$TOKEN" "" "200" "个人出勤统计"
    test_api GET "/api/report/personal/attendance-trend?month=2026-05" "$TOKEN" "" "200" "个人出勤趋势"
    test_api GET "/api/report/personal/leave-summary?month=2026-05" "$TOKEN" "" "200" "个人请假统计"
    test_api GET "/api/report/personal/monthly-compare?month=2026-05" "$TOKEN" "" "200" "个人月度对比"
    test_api GET "/api/report/admin/attendance-summary?month=2026-05" "$TOKEN" "" "200" "管理员出勤统计"
    test_api GET "/api/report/admin/dept-compare?month=2026-05" "$TOKEN" "" "200" "部门出勤对比"
    test_api GET "/api/report/admin/attendance-trend?month=2026-05" "$TOKEN" "" "200" "管理员出勤趋势"
    test_api GET "/api/report/admin/leave-analysis?month=2026-05" "$TOKEN" "" "200" "请假分析"
    test_api GET "/api/report/admin/employee-ranking?month=2026-05" "$TOKEN" "" "200" "员工出勤排名"
    test_api GET "/api/report/admin/today-overview" "$TOKEN" "" "200" "管理员今日概览"

    # ---- 系统监控（需管理员权限） ----
    echo -e "  ${CYAN}--- 系统监控 ---${NC}"
    test_api POST "/online-logs" "$TOKEN" '{"page":1,"pageSize":10}' "200" "在线用户日志"
    test_api POST "/login-logs" "$TOKEN" '{"page":1,"pageSize":10}' "200" "登录日志"
    test_api POST "/operation-logs" "$TOKEN" '{"page":1,"pageSize":10}' "200" "操作日志"
    test_api POST "/system-logs" "$TOKEN" '{"page":1,"pageSize":10}' "200" "系统日志"

    # ---- 系统管理 ----
    echo -e "  ${CYAN}--- 系统管理 ---${NC}"
    test_api POST "/user" "$TOKEN" '{"page":1,"pageSize":10}' "200" "用户列表"
    test_api GET "/list-all-role" "$TOKEN" "" "200" "所有角色"
    test_api GET "/mine-logs" "$TOKEN" "" "200" "我的操作日志"

    # ---- 菜单管理 ----
    echo -e "  ${CYAN}--- 菜单管理 ---${NC}"
    test_api GET "/api/menu/tree" "$TOKEN" "" "200" "菜单树"

    # ---- 会议管理 ----
    echo -e "  ${CYAN}--- 会议管理 ---${NC}"
    test_api GET "/api/meeting/room/list" "$TOKEN" "" "200" "会议室列表"
    test_api GET "/api/meeting/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "会议列表"

    # ---- 资产管理 ----
    echo -e "  ${CYAN}--- 资产管理 ---${NC}"
    test_api GET "/api/asset/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "资产列表"

    # ---- 合同管理 ----
    echo -e "  ${CYAN}--- 合同管理 ---${NC}"
    test_api GET "/api/contract/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "合同列表"
    test_api GET "/api/contract/expiring" "$TOKEN" "" "200" "即将到期合同"

    # ---- 预算管理 ----
    echo -e "  ${CYAN}--- 预算管理 ---${NC}"
    test_api GET "/api/budget/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "预算列表"

    # ---- 岗位管理 ----
    echo -e "  ${CYAN}--- 岗位管理 ---${NC}"
    test_api GET "/api/post/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "岗位列表"
    test_api GET "/api/post/list" "$TOKEN" "" "200" "岗位全部列表"

    # ---- 字典管理 ----
    echo -e "  ${CYAN}--- 字典管理 ---${NC}"
    test_api GET "/api/dict/type/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "字典类型列表"
    test_api GET "/api/dict/data/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "字典数据列表"

    # ---- 系统配置 ----
    echo -e "  ${CYAN}--- 系统配置 ---${NC}"
    test_api GET "/api/config/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "系统配置列表"

    # ---- 告警管理 ----
    echo -e "  ${CYAN}--- 告警管理 ---${NC}"
    test_api GET "/api/alert/rule/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "告警规则列表"
    test_api GET "/api/alert/log/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "告警日志列表"

    # ---- 操作日志（新接口） ----
    echo -e "  ${CYAN}--- 操作日志 ---${NC}"
    test_api GET "/api/operation-log/page?pageNum=1&pageSize=10" "$TOKEN" "" "200" "操作日志(分页)"

    # ---- Token刷新 ----
    echo -e "  ${CYAN}--- Token管理 ---${NC}"
    if [ -n "$REFRESH_TOKEN" ]; then
        test_api POST "/refresh-token" "" "{\"refreshToken\":\"$REFRESH_TOKEN\"}" "200" "刷新Token"
    fi

    # ---- 认证失败测试 ----
    echo -e "  ${CYAN}--- 认证失败测试 ---${NC}"
    test_api_http GET "/mine" "invalid-token-12345" "" "401" "无效Token请求(HTTP 401)"

else
    echo -e "  ${YELLOW}跳过认证接口测试（未获取到Token）${NC}"
fi

echo ""

# ============================================================
#  Step 5: 测试报告
# ============================================================
echo -e "${CYAN}[5/5] 生成测试报告...${NC}"
echo ""
echo -e "${CYAN}=========================================="
echo "        OA系统 API自动化测试报告"
echo -e "==========================================${NC}"
echo "测试时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "后端地址: $BASE"
echo "------------------------------------------"
echo -e "总计: $TOTAL 个测试"
echo -e "通过: ${GREEN}$PASS${NC}"
echo -e "失败: ${RED}$FAIL${NC}"

if [ "$TOTAL" -gt 0 ]; then
    rate=$(awk "BEGIN {printf \"%.1f\", $PASS * 100 / $TOTAL}")
    echo -e "成功率: ${rate}%"
fi

echo "=========================================="
echo -e "$RESULTS"
echo -e "==========================================${NC}"

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo -e "${RED}存在失败测试项，请检查以上输出。${NC}"
    exit 1
else
    echo ""
    echo -e "${GREEN}全部测试通过！${NC}"
    exit 0
fi
