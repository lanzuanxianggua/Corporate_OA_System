#!/bin/bash
# ============================================================
# OA System - Complete Test Runner
# Runs: Backend API tests, Frontend UI tests, Mobile UI tests
# Usage: bash tests/run-all-tests.sh [api|frontend|mobile|all]
# ============================================================

set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TEST_DIR="$ROOT_DIR/tests"

TARGET=${1:-all}

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  OA System Automated Test Suite${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Check prerequisites
check_backend() {
    echo -e "${YELLOW}Checking backend (port 8080)...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/captcha 2>/dev/null | grep -q "200"; then
        echo -e "  ${GREEN}Backend: RUNNING${NC}"
        return 0
    else
        echo -e "  ${RED}Backend: NOT RUNNING${NC}"
        return 1
    fi
}

check_frontend() {
    echo -e "${YELLOW}Checking frontend (port 8848)...${NC}"
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8848 2>/dev/null | grep -q "200"; then
        echo -e "  ${GREEN}Frontend: RUNNING${NC}"
        return 0
    else
        echo -e "  ${RED}Frontend: NOT RUNNING${NC}"
        return 1
    fi
}

check_mobile() {
    echo -ne "${YELLOW}Checking mobile H5...${NC} "
    for port in 5173 5174 5175 3000; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:$port 2>/dev/null | grep -q "200"; then
            echo -e "${GREEN}RUNNING on port $port${NC}"
            return 0
        fi
    done
    echo -e "${RED}NOT RUNNING${NC}"
    return 1
}

TOTAL_PASS=0
TOTAL_FAIL=0

# ============================================================
# 1. Backend API Tests
# ============================================================
run_api_tests() {
    echo -e "\n${YELLOW}━━━ 1. Backend API Tests ━━━${NC}"
    if ! check_backend; then
        echo -e "  ${RED}SKIP: Backend not running${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
        return
    fi
    echo ""
    if bash "$TEST_DIR/api-test.sh"; then
        echo -e "  ${GREEN}API Tests: PASSED${NC}"
        TOTAL_PASS=$((TOTAL_PASS+1))
    else
        echo -e "  ${RED}API Tests: FAILED${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
    fi
}

# ============================================================
# 2. Frontend UI Tests
# ============================================================
run_frontend_tests() {
    echo -e "\n${YELLOW}━━━ 2. Frontend UI Tests ━━━${NC}"
    if ! check_frontend; then
        echo -e "  ${RED}SKIP: Frontend not running${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
        return
    fi
    echo ""
    cd "$ROOT_DIR"
    if npx playwright test "$TEST_DIR/frontend-ui-test.spec.ts" --config="$TEST_DIR/playwright.config.ts" 2>&1; then
        echo -e "  ${GREEN}Frontend UI Tests: PASSED${NC}"
        TOTAL_PASS=$((TOTAL_PASS+1))
    else
        echo -e "  ${RED}Frontend UI Tests: FAILED${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
    fi
}

# ============================================================
# 3. Mobile UI Tests
# ============================================================
run_mobile_tests() {
    echo -e "\n${YELLOW}━━━ 3. Mobile UI Tests ━━━${NC}"
    if ! check_mobile; then
        echo -e "  ${RED}SKIP: Mobile H5 not running${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
        return
    fi
    echo ""
    cd "$ROOT_DIR"
    if npx playwright test "$TEST_DIR/mobile-ui-test.spec.ts" --config="$TEST_DIR/playwright.config.ts" 2>&1; then
        echo -e "  ${GREEN}Mobile UI Tests: PASSED${NC}"
        TOTAL_PASS=$((TOTAL_PASS+1))
    else
        echo -e "  ${RED}Mobile UI Tests: FAILED${NC}"
        TOTAL_FAIL=$((TOTAL_FAIL+1))
    fi
}

# ============================================================
# Run
# ============================================================
case "$TARGET" in
    api)
        run_api_tests
        ;;
    frontend)
        run_frontend_tests
        ;;
    mobile)
        run_mobile_tests
        ;;
    all)
        run_api_tests
        run_frontend_tests
        run_mobile_tests
        ;;
    *)
        echo "Usage: bash tests/run-all-tests.sh [api|frontend|mobile|all]"
        exit 1
        ;;
esac

# ============================================================
# Summary
# ============================================================
echo -e "\n${CYAN}============================================${NC}"
echo -e "${CYAN}  FINAL SUMMARY${NC}"
echo -e "${CYAN}============================================${NC}"
echo -e "  ${GREEN}PASSED:${NC} $TOTAL_PASS  ${RED}FAILED:${NC} $TOTAL_FAIL"

if [ $TOTAL_FAIL -eq 0 ]; then
    echo -e "\n${GREEN}ALL TEST SUITES PASSED${NC}"
    exit 0
else
    echo -e "\n${RED}$TOTAL_FAIL TEST SUITE(S) FAILED${NC}"
    exit 1
fi
