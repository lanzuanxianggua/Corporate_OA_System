#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

pages = [
    (11, "为什么选择自研", "为什么自研而非开源", [
        ("开源方案问题", ["Activiti/Flowable 重量级", "Camunda 企业版收费", "通用引擎适配成本高"]),
        ("自研优势", ["轻量级 4张核心表", "完全掌控路由逻辑", "Spring Event 解耦", "学习成本低"])
    ]),
    (12, "数据库设计", "数据库设计与版本管理", [
        ("Flyway版本", ["V100 平台基础", "V200 工作流引擎", "V900 种子数据", "V930+ 业务模块"]),
        ("设计规范", ["表名蛇形复数", "Snowflake主键", "审计字段自动填充", "逻辑删除+乐观锁"])
    ]),
    (13, "核心表结构", "核心业务表设计", [
        ("统计", ["总计60+张业务表", "平台层5张", "工作流层4张", "业务层50+张"])
    ]),
    (14, "安全架构", "安全架构设计", [
        ("自研方案", ["JWT HS256认证", "Filter+Interceptor", "@RequirePermission", "UserContext ThreadLocal"]),
        ("优势", ["无Spring Security复杂性", "避免DI循环依赖", "完全掌控认证流程"])
    ]),
    (15, "权限体系", "权限体系设计", [
        ("功能权限", ["权限码 module:resource:action", "角色关联权限", "用户关联角色"]),
        ("数据权限5级", ["SELF本人", "DEPT本部门", "DEPT_DOWN下级", "COMPANY公司", "ALL全部"])
    ]),
    (16, "技术亮点", "项目技术亮点", [
        ("自研工作流", "4维分层路由"),
        ("统一审批", "共享流程引擎"),
        ("自研安全", "JWT拦截器"),
        ("多模块", "18模块横向隔离")
    ]),
    (17, "工程实践", "工程实践亮点", [
        ("开发规范", ["统一响应R<T>", "TraceID追踪", "审计字段填充", "MapStruct映射"]),
        ("质量保障", ["Flyway版本管理", "GitHub Actions CI/CD", "Spotless格式化", "单元+集成测试"])
    ]),
    (18, "系统规模", "系统规模与完成度", [
        ("代码规模", ["后端8000+行Java", "前端6000+行TS", "60+张表", "100+ API"]),
        ("完成度", ["8大业务域完成", "工作流完整", "全链路打通", "前后端对接"])
    ]),
    (19, "系统工作台", "系统演示:工作台首页", [
        ("功能", ["待办任务中心", "快捷入口", "最近访问", "消息通知", "数据看板"])
    ]),
    (20, "审批流程", "系统演示:审批流程", [
        ("关键功能", ["流程进度可视化", "审批意见填写", "任务委派转交", "流程撤回", "消息推送"])
    ]),
    (21, "项目总结", "项目总结", [
        ("完成成果", ["完整OA系统", "8大业务域", "自研工作流", "100+ API"]),
        ("技术收获", ["企业架构设计", "全栈开发", "多模块管理", "CI/CD实践"])
    ]),
    (22, "未来展望", "未来展望", [
        ("功能扩展", ["移动端支持", "BI数据分析", "流程可视化配置", "多租户SaaS"]),
        ("技术优化", ["Redis缓存", "ES全文检索", "消息队列", "微服务化"])
    ])
]

SVG_TEMPLATE = '''<svg viewBox="0 0 1280 720" width="1280" height="720" xmlns="http://www.w3.org/2000/svg">
<defs><linearGradient id="hg" x1="0%%" y1="0%%" x2="100%%" y2="0%%"><stop offset="0%%" stop-color="#1565C0"/><stop offset="100%%" stop-color="#4CAF50"/></linearGradient></defs>
<rect width="1280" height="720" fill="#FFF"/><rect width="1280" height="80" fill="url(#hg)"/>
<text x="60" y="50" font-family="Microsoft YaHei" font-size="36" font-weight="700" fill="#FFF">%s</text>
%s
<text x="1220" y="705" font-family="Microsoft YaHei" font-size="12" fill="#ADB5BD" text-anchor="end">%d</text>
</svg>'''

for num, slug, title, sections in pages:
    y = 140
    content_svg = ""
    for i, sec in enumerate(sections):
        if isinstance(sec, tuple) and len(sec) == 2:
            if isinstance(sec[1], list):
                sec_title, items = sec
                content_svg += f'<rect x="80" y="{y}" width="560" height="480" fill="#F5F7FA" rx="12"/>'
                content_svg += f'<text x="360" y="{y+50}" font-family="Microsoft YaHei" font-size="22" font-weight="600" fill="#1565C0" text-anchor="middle">{sec_title}</text>'
                item_y = y + 100
                for item in items:
                    content_svg += f'<text x="120" y="{item_y}" font-family="Microsoft YaHei" font-size="18" fill="#1D1D1F">• {item}</text>'
                    item_y += 35
                y += 0 if i == 0 else 0
                if i == 0 and len(sections) > 1:
                    content_svg += f'<rect x="660" y="140" width="560" height="480" fill="#E8F5E9" rx="12"/>'
                    if isinstance(sections[1], tuple):
                        sec2_title, items2 = sections[1]
                        content_svg += f'<text x="940" y="190" font-family="Microsoft YaHei" font-size="22" font-weight="600" fill="#4CAF50" text-anchor="middle">{sec2_title}</text>'
                        item_y2 = 240
                        for item2 in items2:
                            content_svg += f'<text x="700" y="{item_y2}" font-family="Microsoft YaHei" font-size="18" fill="#1D1D1F">• {item2}</text>'
                            item_y2 += 35
                    break
            else:
                content_svg += f'<rect x="{80 + i*300}" y="140" width="280" height="480" fill="#E3F2FD" rx="12"/>'
                content_svg += f'<text x="{220 + i*300}" y="200" font-family="Microsoft YaHei" font-size="20" font-weight="600" fill="#1565C0" text-anchor="middle">{sec[0]}</text>'
                content_svg += f'<text x="{220 + i*300}" y="280" font-family="Microsoft YaHei" font-size="18" fill="#1D1D1F" text-anchor="middle">{sec[1]}</text>'

    svg_content = SVG_TEMPLATE % (title, content_svg, num)
    filename = f"{num:02d}_{slug}.svg"
    with open(f"svg_output/{filename}", "w", encoding="utf-8") as f:
        f.write(svg_content)
    print(f"Generated {filename}")
