#!/bin/bash
# ============================================================
# OA System - Backend API Automated Test Script
# Usage: bash tests/api-test.sh
# ============================================================

BASE_URL="http://localhost:8080"
REDIS_CLI="${REDIS_CLI:-C:/Program Files/Redis/redis-cli.exe}"
PASS=0
FAIL=0
ERRORS=()

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'

log_pass() { echo -e "  ${GREEN}[PASS]${NC} $1"; ((PASS++)); }
log_fail() { echo -e "  ${RED}[FAIL]${NC} $1"; ((FAIL++)); ERRORS+=("$1"); }
log_section() { echo -e "\n${YELLOW}===== $1 =====${NC}"; }

json_str() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
json_num() { echo "$1" | grep -o "\"$2\":[0-9-]*" | head -1 | cut -d':' -f2; }

login() {
    local username=$1
    local cr=$(curl -s "$BASE_URL/api/auth/captcha")
    local uuid=$(json_str "$cr" "uuid")
    local cap=$("$REDIS_CLI" GET "captcha:$uuid" 2>/dev/null | tr -d '"' | tr -d '\r\n')
    if [ -z "$cap" ]; then
      echo "  [DEBUG] uuid=$uuid cap=EMPTY redis-cli failed or key not found" >&2
    fi
    local lr=$(curl -s -X POST "$BASE_URL/login" -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"123456\",\"captchaCode\":\"$cap\",\"captchaUuid\":\"$uuid\"}")
    local token=$(json_str "$lr" "accessToken")
    if [ -z "$token" ]; then
      echo "  [DEBUG] login=$username response=$(echo "$lr" | head -c 200)" >&2
    fi
    echo "$token"
}

check() {
    local resp=$1 expected=${2:-0}
    local code=$(json_num "$resp" "code")
    [ "$code" = "$expected" ]
}

assert_ok() {
    local label=$1 resp=$2 expected=${3:-0}
    if check "$resp" "$expected"; then
        log_pass "$label"
    else
        log_fail "$label"
    fi
}

# ============================================================
log_section "1. Health & Authorization"

resp=$(curl -s "$BASE_URL/api/auth/captcha")
if check "$resp"; then
    log_pass "Captcha"
else
    log_fail "Captcha - backend not running?"
    exit 1
fi

TOKEN_USER=$(login "wujiu")
if [ -n "$TOKEN_USER" ]; then
    log_pass "Login USER (wujiu)"
else
    log_fail "Login USER"
fi

TOKEN_ADMIN=$(login "admin")
if [ -n "$TOKEN_ADMIN" ]; then
    log_pass "Login ADMIN (admin)"
else
    log_fail "Login ADMIN"
fi

# ============================================================
log_section "2. Core Pages (GET)"

assert_ok "Employee page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/employee/page?pageNum=1&pageSize=5")"

assert_ok "Attendance today" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/attendance/today")"

assert_ok "Leave page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/leave/page?pageNum=1&pageSize=5")"

assert_ok "Overtime page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/overtime/page?pageNum=1&pageSize=5")"

assert_ok "Business-trip page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/business-trip/page?pageNum=1&pageSize=5")"

assert_ok "Outing page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/outing/page?pageNum=1&pageSize=5")"

assert_ok "Purchase page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/purchase/page?pageNum=1&pageSize=5")"

assert_ok "Expense page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/expense/page?pageNum=1&pageSize=5")"

assert_ok "Loan page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/loan/page?pageNum=1&pageSize=5")"

# ============================================================
log_section "3. Application Submits (POST)"

H="Content-Type: application/json"

assert_ok "Leave submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/leave/submit" \
    -d '{"leaveType":1,"startTime":"2026-09-01 09:00:00","endTime":"2026-09-01 18:00:00","reason":"auto-test"}')"

assert_ok "Overtime submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/overtime/submit" \
    -d '{"overtimeDate":"2026-09-10","startTime":"2026-09-10 18:00:00","endTime":"2026-09-10 21:00:00","hours":3,"reason":"auto-test"}')"

assert_ok "Business-trip submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/business-trip/submit" \
    -d '{"destination":"BJ","purpose":"meeting","startTime":"2026-09-15 09:00:00","endTime":"2026-09-17 18:00:00"}')"

assert_ok "Outing submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/outing/submit" \
    -d '{"reason":"visit","destination":"SH","startTime":"2026-09-20 09:00:00","endTime":"2026-09-20 18:00:00"}')"

assert_ok "Purchase submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/purchase/submit" \
    -d '{"itemName":"laptop","quantity":1,"amount":5000,"reason":"auto-test"}')"

assert_ok "Expense submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/expense/submit" \
    -d '{"title":"travel","amount":1000,"category":"travel","description":"auto-test"}')"

assert_ok "Loan submit" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_USER" -H "$H" "$BASE_URL/api/loan/submit" \
    -d '{"loanAmount":2000,"loanReason":"auto-test","repaymentPlan":"3m"}')"

# ============================================================
log_section "4. Admin & Workflow"

assert_ok "Workflow pending tasks" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/workflow/task/pending?pageNum=1&pageSize=10")"

assert_ok "Workflow definitions" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/workflow/definition/list")"

assert_ok "Dashboard stats" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/statistics/dashboard?period=today")"

assert_ok "Notice page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/notice/page?pageNum=1&pageSize=5")"

assert_ok "Contract page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/contract/page?pageNum=1&pageSize=5")"

assert_ok "Asset page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/asset/page?pageNum=1&pageSize=5")"

assert_ok "Salary page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/salary/structure/page?pageNum=1&pageSize=5")"

assert_ok "Message page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/message/page?pageNum=1&pageSize=5")"

assert_ok "Todo page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/todo/page?pageNum=1&pageSize=5")"

assert_ok "Dept tree" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/dept/tree")"

assert_ok "Schedule page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/schedule/page?pageNum=1&pageSize=5")"

assert_ok "Document page" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/document/page?pageNum=1&pageSize=5")"

# ============================================================
log_section "5. Additional Endpoints"

assert_ok "Workflow CC my" "$(curl -s -H "Authorization: Bearer $TOKEN_USER" "$BASE_URL/api/workflow/cc/my?pageNum=1&pageSize=5")"

assert_ok "Workflow delegation my" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/workflow/delegation/my")"

assert_ok "Meeting room list" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/meeting/room/list")"

assert_ok "Meeting page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/meeting/page?pageNum=1&pageSize=5")"

assert_ok "Employee page (all)" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/employee/page?pageNum=1&pageSize=50")"

assert_ok "Dict types" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/dict/type/page?pageNum=1&pageSize=10")"

assert_ok "Config page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/config/page?pageNum=1&pageSize=10")"

assert_ok "Post page" "$(curl -s -H "Authorization: Bearer $TOKEN_ADMIN" "$BASE_URL/api/post/page?pageNum=1&pageSize=10")"

assert_ok "Role list (POST)" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_ADMIN" -H "$H" "$BASE_URL/role")"

assert_ok "Menu tree (POST)" "$(curl -s -X POST -H "Authorization: Bearer $TOKEN_ADMIN" -H "$H" "$BASE_URL/menu")"

# ============================================================
log_section "SUMMARY"
echo -e "  ${GREEN}PASS:${NC} $PASS  ${RED}FAIL:${NC} $FAIL  ${CYAN}TOTAL:${NC} $((PASS+FAIL))"
if [ ${#ERRORS[@]} -gt 0 ]; then
    echo -e "\n  ${RED}Failed:${NC}"
    for e in "${ERRORS[@]}"; do echo -e "    - $e"; done
fi
if [ $FAIL -eq 0 ]; then
    echo -e "\n${GREEN}ALL TESTS PASSED${NC}"
    exit 0
else
    echo -e "\n${RED}$FAIL FAILED${NC}"
    exit 1
fi
