"""Comprehensive API test for mobile app backend endpoints"""
import json, urllib.request, base64, redis, time, sys, os

# Set UTF-8 for console
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
os.environ['PYTHONIOENCODING'] = 'utf-8'

def get_token():
    """Login and get token"""
    r = redis.Redis(host='127.0.0.1', port=6379, db=0)
    resp = urllib.request.urlopen('http://localhost:8080/api/auth/captcha')
    data = json.loads(resp.read())
    uuid = data['data']['uuid']
    answer = r.get(f'captcha:{uuid}').decode().strip('"').strip()

    login_data = json.dumps({'username': 'admin', 'password': 'admin123', 'captchaUuid': uuid, 'captchaCode': answer}).encode()
    req = urllib.request.Request('http://localhost:8080/api/auth/login', data=login_data, headers={'Content-Type': 'application/json'})
    resp2 = urllib.request.urlopen(req)
    token_data = json.loads(resp2.read())
    return token_data['data']['accessToken']

def api(method, url, data=None, token=None):
    """Make an API call and return (status, body)"""
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = f'Bearer {token}'

    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(f'http://localhost:8080{url}', data=body, headers=headers, method=method)

    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())
    except Exception as e:
        return 0, {'error': str(e)}

token = get_token()
print(f'[OK] Login success, token={token[:30]}...\n')

results = {'pass': 0, 'fail': 0, 'warn': 0}

def test(name, url, method='GET', data=None):
    """Test an API endpoint"""
    status, body = api(method, f'/api{url}', data, token)

    if status == 0:
        print(f'  [FAIL] {name}: Network error - {body.get("error","")}')
        results['fail'] += 1
        return

    if body.get('code') == 0:
        print(f'  [PASS] {name}')
        results['pass'] += 1
    else:
        print(f'  [WARN] {name}: code={body.get("code")}, msg={body.get("message","")}')
        results['warn'] += 1

print('===== 1. Authentication =====')
test('Captcha', '/auth/captcha')

print('\n===== 2. Home / Dashboard =====')
test('Today attendance', '/attendance/today')
test('Todo count', '/todo/count')

print('\n===== 3. Attendance =====')
test('Clock in', '/attendance/clock-in', 'POST')
time.sleep(1)
test('Clock out', '/attendance/clock-out', 'POST')
test('Attendance history', '/attendance/history?startDate=2026-06-01&endDate=2026-06-30')

print('\n===== 4. Leave =====')
test('Leave list', '/leave/page?pageNum=1&pageSize=10')
test('Leave balance', '/leave-balance/my')

print('\n===== 5. Business forms =====')
test('Business trip list', '/business-trip/page?pageNum=1&pageSize=10')
test('Outing list', '/outing/page?pageNum=1&pageSize=10')
test('Overtime list', '/overtime/page?pageNum=1&pageSize=10')
test('Expense list', '/expense/page?pageNum=1&pageSize=10')
test('Purchase list', '/purchase/page?pageNum=1&pageSize=10')
test('Loan list', '/loan/page?pageNum=1&pageSize=10')

print('\n===== 6. Todo =====')
test('Todo list', '/todo/page?pageNum=1&pageSize=10')

print('\n===== 7. Workflow =====')
test('Definition list', '/workflow/definition/list')
test('Pending tasks', '/workflow/task/pending?pageNum=1&pageSize=10')
test('Handled tasks', '/workflow/task/handled?pageNum=1&pageSize=10')

print('\n===== 8. Message & Notice =====')
test('Message list', '/message/page?pageNum=1&pageSize=10')
test('Unread count', '/message/unread-count')
test('Notice list', '/notice/page?pageNum=1&pageSize=10')

print('\n===== 9. Document =====')
test('Document list', '/document/page?pageNum=1&pageSize=10')

print('\n===== 10. Schedule =====')
test('Schedule list', '/schedule/page?pageNum=1&pageSize=10')

print('\n===== 11. Employee =====')
test('Employee info', f'/employee/{1}')

print('\n===== 12. Report =====')
test('Attendance summary', '/report/personal/attendance-summary?period=month&month=2026-06')
test('Leave summary', '/report/personal/leave-summary?period=month&month=2026-06')

print(f'\n=========== Test Results ===========')
print(f'  PASS: {results["pass"]}')
print(f'  WARN: {results["warn"]}')
print(f'  FAIL: {results["fail"]}')
print(f'  Total: {results["pass"] + results["warn"] + results["fail"]}')
print(f'====================================')
