"""Test form submission endpoints"""
import json, urllib.request, redis, os, sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
os.environ['PYTHONIOENCODING'] = 'utf-8'

def get_token():
    r = redis.Redis(host='127.0.0.1', port=6379, db=0)
    resp = urllib.request.urlopen('http://localhost:8080/api/auth/captcha')
    data = json.loads(resp.read())
    uuid = data['data']['uuid']
    answer = r.get(f'captcha:{uuid}').decode().strip('"').strip()
    login_data = json.dumps({'username':'admin','password':'admin123','captchaUuid':uuid,'captchaCode':answer}).encode()
    req = urllib.request.Request('http://localhost:8080/api/auth/login', data=login_data, headers={'Content-Type':'application/json'})
    return json.loads(urllib.request.urlopen(req).read())['data']['accessToken']

def api_post(url, data, token):
    headers = {'Content-Type':'application/json','Authorization':f'Bearer {token}'}
    req = urllib.request.Request(f'http://localhost:8080{url}', data=json.dumps(data).encode(), headers=headers, method='POST')
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read())

token = get_token()
print(f'[OK] Token: {token[:30]}...\n')

print('===== Form Submit Tests =====')

# Leave submit
data = {"leaveType":"1","startTime":"2026-07-01 09:00:00","endTime":"2026-07-02 18:00:00","days":2,"reason":"测试请假"}
r = api_post('/api/leave/submit', data, token)
print(f'  Leave submit: code={r.get("code")}, msg={r.get("message","")}')

# Business trip submit
data = {"destination":"广州","startTime":"2026-07-05 09:00:00","endTime":"2026-07-06 18:00:00","purpose":"测试出差"}
r = api_post('/api/business-trip/submit', data, token)
print(f'  Trip submit: code={r.get("code")}, msg={r.get("message","")}')

# Overtime submit
data = {"overtimeDate":"2026-07-01","startTime":"2026-07-01 18:00:00","endTime":"2026-07-01 21:00:00","hours":3,"reason":"测试加班"}
r = api_post('/api/overtime/submit', data, token)
print(f'  Overtime submit: code={r.get("code")}, msg={r.get("message","")}')

# Expense submit
data = {"title":"测试经费","category":"office","amount":1000,"description":"测试报销"}
r = api_post('/api/expense/submit', data, token)
print(f'  Expense submit: code={r.get("code")}, msg={r.get("message","")}')

# Purchase submit
data = {"itemName":"测试采购","quantity":5,"amount":500,"reason":"测试采购"}
r = api_post('/api/purchase/submit', data, token)
print(f'  Purchase submit: code={r.get("code")}, msg={r.get("message","")}')

# Loan submit
data = {"loanAmount":3000,"loanReason":"测试借款","repaymentPlan":"下月还"}
r = api_post('/api/loan/submit', data, token)
print(f'  Loan submit: code={r.get("code")}, msg={r.get("message","")}')

# Schedule add
data = {"title":"测试日程","startTime":"2026-07-01 09:00:00","endTime":"2026-07-01 10:00:00"}
r = api_post('/api/schedule', data, token)
print(f'  Schedule add: code={r.get("code")}, msg={r.get("message","")}')

print('\n===== Verify after submit =====')

# Leave page
req = urllib.request.Request('http://localhost:8080/api/leave/page?pageNum=1&pageSize=5', headers={'Authorization':f'Bearer {token}'})
r = json.loads(urllib.request.urlopen(req).read())
print(f'  Leave list: total={r.get("data",{}).get("total",0)}, records={len(r.get("data",{}).get("list",[]))}')

# Workflow pending
req = urllib.request.Request('http://localhost:8080/api/workflow/task/pending?pageNum=1&pageSize=5', headers={'Authorization':f'Bearer {token}'})
r = json.loads(urllib.request.urlopen(req).read())
print(f'  Pending tasks: total={r.get("data",{}).get("total",0)}, records={len(r.get("data",{}).get("list",[]))}')

# Schedule list
req = urllib.request.Request('http://localhost:8080/api/schedule/page?pageNum=1&pageSize=5', headers={'Authorization':f'Bearer {token}'})
r = json.loads(urllib.request.urlopen(req).read())
print(f'  Schedule list: total={r.get("data",{}).get("total",0)}, records={len(r.get("data",{}).get("list",[]))}')

print('\n[OK] All form submission tests completed')
