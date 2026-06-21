package cn.oa.tools.ppt;

import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFFreeformShape;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.StrokeStyle;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OaDefensePptBuilder {

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path OUTPUT = ROOT.resolve("OA_System_Defense_PPT.pptx");
    private static final Path IMG_DIR = ROOT.resolve("系统演示图片");

    private static final Color P = new Color(0x66, 0x7E, 0xEA);
    private static final Color PD = new Color(0x4F, 0x46, 0xE5);
    private static final Color PU = new Color(0x76, 0x4B, 0xA2);
    private static final Color W = new Color(0xFF, 0xFF, 0xFF);
    private static final Color T = new Color(0x1E, 0x29, 0x3B);
    private static final Color TM = new Color(0x47, 0x55, 0x69);
    private static final Color TS = new Color(0x94, 0xA3, 0xB8);
    private static final Color BD = new Color(0x0F, 0x0C, 0x29);
    private static final Color BM = new Color(0x1A, 0x1A, 0x2E);
    private static final Color BG = new Color(0xF8, 0xF9, 0xFC);
    private static final Color G = new Color(0x05, 0x96, 0x69);
    private static final Color O = new Color(0xEA, 0x58, 0x0C);
    private static final Color R = new Color(0xDC, 0x26, 0x26);
    private static final Color TE = new Color(0x0D, 0x94, 0x88);

    private final XMLSlideShow ppt = new XMLSlideShow();

    public static void main(String[] args) throws Exception {
        new OaDefensePptBuilder().build();
    }

    private void build() throws Exception {
        ppt.setPageSize(new Dimension(inches(13.333), inches(7.5)));

        cover();
        overview();
        architecture();
        workflowHighlight();
        workflowDesigner();
        dashboard();
        attendance();
        messageCenter();
        workbench();
        employeeManage();
        roleManage();
        leaveApply();
        login();
        workflowRouting();
        redisAndAuth();
        databaseAndEngineering();
        summary();
        thanks();

        try (FileOutputStream out = new FileOutputStream(OUTPUT.toFile())) {
            ppt.write(out);
        }

        System.out.println("Generated: " + OUTPUT);
        System.out.println("Slides: " + ppt.getSlides().size());
    }

    private void cover() {
        XSLFSlide slide = slide();
        background(slide, BM);
        addRect(slide, 0, 7.2, 13.333, 0.05, P);
        addText(slide, 1.2, 2.15, 10.9, 0.9,
                "企业 OA 办公自动化系统", 28, true, W, TextParagraph.TextAlign.CENTER);
        addText(slide, 1.2, 3.05, 10.9, 0.5,
                "基于 Spring Boot 3.4 + Vue 3 的企业级办公管理平台",
                14, false, new Color(180, 180, 200), TextParagraph.TextAlign.CENTER);
        addRect(slide, 4.35, 3.95, 4.6, 0.03, P);
        addText(slide, 2.9, 4.25, 7.4, 1.1,
                "答辩人：[姓名]\n指导老师：[教师姓名]\n2026 年 6 月",
                12, false, new Color(150, 150, 175), TextParagraph.TextAlign.CENTER);
        addText(slide, 0.8, 6.45, 11.8, 0.3,
                "Corporate OA Office Automation System", 10, false, TS, TextParagraph.TextAlign.CENTER);
    }

    private void overview() {
        XSLFSlide slide = sectionSlide("项目概述",
                "面向企业审批、考勤、协同与管理的一体化办公平台");

        addRound(slide, 0.8, 1.35, 5.8, 2.8, BG);
        addText(slide, 1.0, 1.52, 5.2, 0.25, "系统定位", 14, true, T, TextParagraph.TextAlign.LEFT);
        addText(slide, 1.0, 1.85, 5.2, 1.9,
                "面向中大型组织日常办公场景设计。\n" +
                        "覆盖 8 大业务域和 50+ 功能模块。\n" +
                        "将审批、考勤、消息、人事、工作台等高频办公流程\n" +
                        "统一沉淀为线上化、可追踪的业务流程。",
                11, false, TM, TextParagraph.TextAlign.LEFT);

        addRound(slide, 6.9, 1.35, 5.6, 2.8, P);
        addText(slide, 7.15, 1.52, 5.0, 0.25, "核心目标", 14, true, W, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 7.15, 1.85, 5.0, 1.8, List.of(
                "基于自研工作流引擎实现审批流程可配置",
                "通过角色、菜单、权限码实现 RBAC 访问控制",
                "通过审计字段、逻辑删除、日志实现数据可追溯",
                "采用前后端分离架构与统一 API 契约"
        ), 11, W);

        addMetricCardRow(slide, List.of(
                new Metric("50+", "功能模块"),
                new Metric("8", "业务域"),
                new Metric("7+", "审批类型"),
                new Metric("5", "后端核心模块"),
                new Metric("3", "使用端形态")
        ), 0.8, 4.65, 2.35, 1.05);

        addTechBands(slide, List.of(
                new LabeledText("后端", "Java 17、Spring Boot 3.4、MyBatis-Plus、MySQL、Redis"),
                new LabeledText("前端", "Vue 3、TypeScript、Vite 6、Element Plus、Pinia、ECharts"),
                new LabeledText("工程化", "Maven 多模块、Flyway 迁移、WebSocket、Excel 导出")
        ), 0.8, 6.0, 3.95, 0.78);
    }

    private void architecture() {
        XSLFSlide slide = sectionSlide("技术架构",
                "前后端分离架构，分层清晰，请求链路完整");

        addRound(slide, 0.8, 1.35, 12.0, 1.0, BG);
        addFlowNode(slide, 1.1, 1.6, 1.8, 0.45, "浏览器 / H5", P);
        addArrowText(slide, 2.95, 1.73, "REST API");
        addFlowNode(slide, 4.0, 1.6, 2.0, 0.45, "Vue 3 前端", TE);
        addArrowText(slide, 6.1, 1.73, "Vite 代理");
        addFlowNode(slide, 7.15, 1.6, 2.1, 0.45, "Spring Boot", O);
        addArrowText(slide, 9.4, 1.73, "持久化");
        addFlowNode(slide, 10.45, 1.6, 1.9, 0.45, "MySQL / Redis", PU);

        addRound(slide, 0.8, 2.7, 5.8, 3.85, BG);
        addText(slide, 1.0, 2.9, 5.0, 0.25, "后端分层", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 1.0, 3.25, 5.0, 2.8, List.of(
                "oa-common：响应封装、异常处理、JWT、拦截器",
                "oa-model：实体、DTO、VO 与工作流模型",
                "oa-mapper：MyBatis-Plus Mapper 与 SQL 映射",
                "oa-service：业务编排、事务控制与回调逻辑",
                "oa-web：控制器、启动配置与 WebSocket 入口"
        ), 11, TM);

        addRound(slide, 6.9, 2.7, 5.9, 3.85, BG);
        addText(slide, 7.1, 2.9, 5.2, 0.25, "典型请求链路", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 7.1, 3.25, 5.2, 2.8, List.of(
                "Axios 请求 -> Vite 代理 -> 后端 Controller",
                "RateLimitInterceptor 负责登录 IP 限流",
                "AuthInterceptor 解析 JWT 并校验 Redis Token",
                "Service 层完成业务编排、事务和工作流回调",
                "Mapper 层写入 MySQL，并按场景更新 Redis"
        ), 11, TM);
    }

    private void workflowHighlight() {
        XSLFSlide slide = sectionSlide("核心亮点：自研工作流引擎",
                "工作流是本项目最核心的技术亮点，也是答辩重点");

        addRound(slide, 0.8, 1.35, 5.9, 4.8, BG);
        addText(slide, 1.0, 1.55, 5.1, 0.25, "为什么不直接用 Flowable / Camunda？", 14, true, T, TextParagraph.TextAlign.LEFT);
        addTable(slide, 1.0, 1.95, 5.25, 2.0,
                List.of("对比项", "Flowable", "自研方案"),
                List.of(
                        List.of("依赖规模", "~5 MB", "~2000 行聚焦 OA 场景"),
                        List.of("数据表", "30+", "4 张核心工作流表"),
                        List.of("学习成本", "需要理解 BPMN", "JSON 图结构更直观"),
                        List.of("定制方式", "偏 JavaDelegate", "按业务策略扩展"),
                        List.of("部署方式", "偏引擎中心", "直接随当前服务运行")
                ),
                List.of(1.3, 1.65, 2.3));

        addRound(slide, 1.0, 4.2, 5.25, 1.5, P);
        addText(slide, 1.15, 4.35, 4.95, 1.15,
                "项目没有追求“大而全”的 BPM 能力，\n" +
                        "而是只实现 OA 场景真正高频的 20% 核心能力：\n" +
                        "审批节点、条件路由、审批人解析、委派、回调与任务控制。",
                11, false, W, TextParagraph.TextAlign.LEFT);

        addRound(slide, 6.95, 1.35, 5.75, 2.25, P);
        addText(slide, 7.15, 1.55, 5.1, 0.25, "引擎核心组成", 14, true, W, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 7.15, 1.9, 5.0, 1.45, List.of(
                "流程定义：基于 JSON 图结构描述节点、边和条件",
                "流程实例：与业务单据绑定的运行时快照",
                "审批任务：待办、已办、会签、或签、委派",
                "运行时引擎：解析、路由、推进下一节点",
                "业务回调：审批结束后通过事件驱动业务联动"
        ), 10, W);

        addRound(slide, 6.95, 3.95, 5.75, 2.2, BG);
        addText(slide, 7.15, 4.15, 5.0, 0.25, "工作流覆盖的业务类型", 14, true, T, TextParagraph.TextAlign.LEFT);
        addTagRow(slide, List.of(
                new Tag("请假", P), new Tag("出差", TE), new Tag("报销", O),
                new Tag("采购", PU), new Tag("加班", G), new Tag("外出", TM), new Tag("借支", R)
        ), 7.15, 4.55, 1.35, 0.35, 4);

        addText(slide, 0.8, 6.5, 12.0, 0.35,
                "答辩强调点：一套通用引擎支撑多类审批业务，同时保持轻量、可配置、易讲清。",
                10, false, TM, TextParagraph.TextAlign.LEFT);
    }

    private void workflowDesigner() throws IOException {
        XSLFSlide slide = imageFeatureSlide(
                "核心业务：流程设计器",
                "一图一页，重点展示管理员如何零代码配置审批流程",
                "流程自定义.png",
                List.of(
                        "通过可视化界面配置审批步骤和流程走向",
                        "支持金额分级、时长分级等模板化流程生成",
                        "审批策略支持直属上级、角色、角色链、指定人员",
                        "保存前会校验环路、连通性和审批人配置完整性"
                ),
                "这页是最强的业务证明，因为它体现的不是写死流程，而是可配置的平台能力。"
        );
        addCallout(slide, 9.35, 1.55, 3.3, 0.9, "讲解建议", "管理员配置一次流程，多类审批表单都可以复用同一套引擎。", P, W);
        addCallout(slide, 9.35, 2.65, 3.3, 0.95, "价值点", "这里让系统从普通 CRUD 管理系统，上升为可复用的平台能力。", BG, T);
        addCallout(slide, 9.35, 3.82, 3.3, 1.15, "路由逻辑", "流程条件可以依赖金额、天数、小时数和发起人级别。", BG, T);
    }

    private void dashboard() throws IOException {
        imageFeatureSlide(
                "核心业务：数据看板",
                "系统不仅能处理事务，还能把业务数据转成可视化结果",
                "数据看板1.png",
                List.of(
                        "基于 ECharts 展示考勤、活跃度、负载等运营指标",
                        "支持趋势图、热力图、散点图、排行等图表形式",
                        "价值主要面向管理层，而不仅仅是普通业务录入",
                        "把底层业务记录转化为可见的管理决策信息"
                ),
                "答辩时可以强调：这个页面体现了项目不止有表单录入，还具备数据价值输出。"
        );
    }

    private void attendance() throws IOException {
        imageFeatureSlide(
                "核心业务：考勤打卡",
                "表面是简单打卡页，背后其实有完整的业务规则支撑",
                "考勤打卡.png",
                List.of(
                        "同一页面支持上班打卡和下班打卡",
                        "迟到、早退状态由考勤组时间规则自动判定",
                        "考勤结果与请假、出差审批结果联动",
                        "配合记录查询和导出支撑后续统计分析"
                ),
                "这页适合讲“前台操作简单，后台规则不少”，比如迟到判断、请假联动、记录统计。"
        );
    }

    private void messageCenter() throws IOException {
        imageFeatureSlide(
                "核心业务：消息中心",
                "消息模块是完整协同能力的一部分，不是占位页",
                "消息中心.png",
                List.of(
                        "结合消息落库和实时通知通道实现可靠送达",
                        "未读、已读状态便于后续跟踪和管理",
                        "既可用于公告，也可用于提醒和流程通知",
                        "配合 WebSocket 与待办模块实现及时提醒"
                ),
                "可以把它和审批提醒、公告通知联系起来，说明系统不仅有审批，还有协同办公能力。"
        );
    }

    private void workbench() throws IOException {
        imageFeatureSlide(
                "核心业务：工作台",
                "工作台是用户日常入口，体现产品级的整合能力",
                "工作台.png",
                List.of(
                        "聚合待办、快捷入口、个人日程和状态卡片",
                        "降低员工高频操作时的跳转成本",
                        "把审批、考勤、协同模块集中到一个入口展示",
                        "比单独展示表单更能体现产品完整性"
                ),
                "这一页适合讲“系统入口设计”，说明产品思维而不只是单模块堆砌。"
        );
    }

    private void employeeManage() throws IOException {
        imageFeatureSlide(
                "核心业务：员工管理",
                "管理端页面，体现组织、档案和账号治理能力",
                "员工管理.png",
                List.of(
                        "支持员工账号维护和基础人事信息管理",
                        "与部门、角色、权限分配能力直接关联",
                        "是审批路由和 RBAC 控制的基础主数据",
                        "体现系统管理员能力，而不仅仅是员工使用能力"
                ),
                "这页可以顺势引出：很多审批策略都依赖员工、部门、角色这类基础主数据。"
        );
    }

    private void roleManage() throws IOException {
        imageFeatureSlide(
                "核心业务：角色与权限管理",
                "通过真实管理页面来讲 RBAC，会比单纯讲概念更直观",
                "角色管理.png",
                List.of(
                        "角色是用户、菜单和操作权限之间的桥梁",
                        "后端把路由角色控制和接口拦截校验结合起来",
                        "能力不只是前端隐藏菜单，而是完整的后端治理",
                        "很适合引出注解式鉴权的技术说明"
                ),
                "这页建议配合讲一句：页面控制只是体验层，真正的权限校验仍在后端拦截器和注解上完成。"
        );
    }

    private void leaveApply() throws IOException {
        imageFeatureSlide(
                "核心业务：请假申请",
                "用最典型的审批表单来展示业务表单如何接入工作流引擎",
                "请假申请.png",
                List.of(
                        "采集请假类型、时间范围、原因等结构化数据",
                        "提交后会触发 workflow 的 startProcess 流程启动",
                        "审批通过后可以自动扣减假期余额并更新考勤状态",
                        "是“业务表单 + 工作流 + 回调联动”的典型案例"
                ),
                "这页最适合讲完整业务链：填表 -> 发起流程 -> 审批通过 -> 扣减余额 / 标记考勤。"
        );
    }

    private void login() throws IOException {
        imageFeatureSlide(
                "功能细节：登录体验",
                "登录页兼具前端交互亮点和认证体系入口价值",
                "登录页.png",
                List.of(
                        "眼球跟踪动画让页面更有记忆点",
                        "验证码、BCrypt、JWT 构成第一道安全防线",
                        "Axios 拦截器和 Token 续期补全认证生命周期",
                        "适合快速展示前端交互打磨与体验质量"
                ),
                "如果时间紧，这页讲短一点，主要作为视觉亮点和安全体系的引子。"
        );
    }

    private void workflowRouting() {
        XSLFSlide slide = sectionSlide("工作流运行机制：路由、会签与委派",
                "业务表单提交之后，引擎内部到底做了什么");

        addRound(slide, 0.8, 1.35, 4.0, 2.3, BG);
        addText(slide, 1.0, 1.55, 3.5, 0.25, "条件路由", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 1.0, 1.9, 3.5, 1.3, List.of(
                "4 个维度：金额、天数、小时数、角色级别",
                "支持 >、>=、<、<=、==、!= 以及逻辑组合",
                "流程快照保证运行中的实例不受后续编辑影响"
        ), 10, TM);

        addRound(slide, 4.95, 1.35, 3.95, 2.3, P);
        addText(slide, 5.15, 1.55, 3.35, 0.25, "会签与或签", 14, true, W, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 5.15, 1.9, 3.3, 1.3, List.of(
                "会签：所有子任务都完成后才能推进",
                "或签：第一个通过的人会取消其余待办",
                "通过父子任务模型管理多人审批执行"
        ), 10, W);

        addRound(slide, 9.0, 1.35, 3.8, 2.3, BG);
        addText(slide, 9.2, 1.55, 3.2, 0.25, "委派与超时升级", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 9.2, 1.9, 3.2, 1.3, List.of(
                "委派用于审批人不在岗时的任务重定向",
                "超时升级可自动推送给更高层级审批人",
                "这两类能力都属于引擎运行时，而不是分散在业务代码里"
        ), 10, TM);

        addRound(slide, 0.8, 4.05, 12.0, 2.25, BD);
        addText(slide, 1.0, 4.25, 11.4, 0.25, "简化运行时链路", 13, true, W, TextParagraph.TextAlign.LEFT);
        addText(slide, 1.0, 4.6, 11.2, 1.4,
                "员工提交请假单 -> 工作流引擎加载最新启用流程定义 -> 从 JSON 图结构生成可执行路径 -> 按条件过滤节点 -> 为解析出的审批人创建待办任务 -> 审批人处理任务 -> 引擎决定通过、驳回、退回或委派 -> 业务回调更新假期余额、考勤与通知。",
                11, false, new Color(200, 210, 220), TextParagraph.TextAlign.LEFT);
    }

    private void redisAndAuth() {
        XSLFSlide slide = sectionSlide("Redis + JWT 认证体系",
                "兼顾无状态身份认证和服务端可控性的实用组合");

        addRound(slide, 0.8, 1.35, 5.9, 4.95, BG);
        addText(slide, 1.0, 1.55, 5.2, 0.25, "Redis 的使用场景", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 1.0, 1.9, 5.1, 3.4, List.of(
                "JWT Token 存储与强制失效",
                "基于 INCR + EXPIRE 的登录 IP 限流",
                "在线用户心跳与强制下线管理",
                "验证码存储与一次性校验",
                "Token 自动续期",
                "工作流并发控制中的分布式锁"
        ), 11, TM);

        addRound(slide, 7.0, 1.35, 5.8, 2.2, P);
        addText(slide, 7.2, 1.55, 5.1, 0.25, "为什么 JWT 还需要 Redis？", 14, true, W, TextParagraph.TextAlign.LEFT);
        addText(slide, 7.2, 1.95, 5.0, 1.1,
                "JWT 很适合做无状态认证，但一旦签发就很难在服务端主动失效。\n" +
                        "Redis 给系统提供了服务端“总开关”：管理员踢人下线、修改密码后旧 Token 失效、会话治理都依赖它。",
                11, false, W, TextParagraph.TextAlign.LEFT);

        addRound(slide, 7.0, 3.85, 5.8, 2.45, BG);
        addText(slide, 7.2, 4.05, 5.0, 0.25, "RBAC + 注解式鉴权", 14, true, T, TextParagraph.TextAlign.LEFT);
        addText(slide, 7.2, 4.45, 5.0, 1.3,
                "前端路由用角色元信息控制页面访问。\n" +
                        "后端拦截器负责解析 JWT 并验证 Redis Token。\n" +
                        "Controller 方法再通过权限注解完成最终访问控制。",
                11, false, TM, TextParagraph.TextAlign.LEFT);
    }

    private void databaseAndEngineering() {
        XSLFSlide slide = sectionSlide("工程支撑：数据库、迁移与导出",
                "项目不仅实现了功能，也体现了较完整的工程化意识");

        addRound(slide, 0.8, 1.35, 5.8, 2.2, BG);
        addText(slide, 1.0, 1.55, 5.0, 0.25, "数据库组织方式", 14, true, T, TextParagraph.TextAlign.LEFT);
        addTable(slide, 1.0, 1.95, 5.2, 1.2,
                List.of("域", "前缀", "示例"),
                List.of(
                        List.of("系统域", "sys_", "employee、dept、role、menu"),
                        List.of("业务域", "oa_", "leave、attendance、message"),
                        List.of("工作流", "wf_", "definition、instance、task"),
                        List.of("报表/预警", "rpt_", "alert rule、alert log")
                ),
                List.of(1.2, 1.0, 3.0));

        addRound(slide, 0.8, 3.95, 5.8, 2.2, BG);
        addText(slide, 1.0, 4.15, 5.0, 0.25, "审计字段与数据可追溯", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 1.0, 4.5, 5.0, 1.2, List.of(
                "通用字段包括 create_time、update_time、create_by、update_by、del_flag",
                "逻辑删除避免业务记录被直接物理清除",
                "操作日志为后续回溯和责任认定提供依据"
        ), 10, TM);

        addRound(slide, 7.0, 1.35, 5.8, 2.2, BG);
        addText(slide, 7.2, 1.55, 5.0, 0.25, "Flyway 迁移", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 7.2, 1.95, 5.0, 1.2, List.of(
                "版本化 SQL 脚本可在启动时自动执行",
                "特别适合工作流表结构演进和种子数据管理",
                "有利于团队协作和部署行为保持一致"
        ), 10, TM);

        addRound(slide, 7.0, 3.95, 5.8, 2.2, BG);
        addText(slide, 7.2, 4.15, 5.0, 0.25, "统一导出策略", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 7.2, 4.5, 5.0, 1.2, List.of(
                "Excel 文件统一采用 业务名_时间戳 的命名方式",
                "适用于请假、出差、报销、采购、加班、考勤等导出场景",
                "提升后台使用体验和运维一致性"
        ), 10, TM);
    }

    private void summary() {
        XSLFSlide slide = slide();
        background(slide, BM);
        addText(slide, 0.8, 0.4, 10.0, 0.45, "项目亮点总结", 24, true, W, TextParagraph.TextAlign.LEFT);
        addText(slide, 0.8, 0.82, 12.0, 0.25,
                "用一页帮助老师快速记住这套系统最重要的能力点", 11, false,
                new Color(180, 180, 200), TextParagraph.TextAlign.LEFT);

        List<Card> cards = List.of(
                new Card("自研工作流", "用可配置 JSON 图结构替代重量级 BPM 平台", P),
                new Card("Redis 多场景复用", "一套中间件服务认证、限流、验证码和锁控制", TE),
                new Card("JWT + RBAC", "身份认证和角色/菜单/权限链路完整闭环", O),
                new Card("业务覆盖面广", "审批、考勤、消息、看板、人事等模块完整", PU),
                new Card("数据可视化能力", "看板和导出能力提升了管理价值", G),
                new Card("工程化支撑", "迁移、审计、日志和多模块分层较完整", R)
        );

        double x = 0.8;
        double y = 1.4;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            addRound(slide, x, y, 3.85, 2.1, BG);
            addRect(slide, x, y, 3.85, 0.05, card.color);
            addText(slide, x + 0.18, y + 0.18, 3.3, 0.28, card.title, 13, true, card.color, TextParagraph.TextAlign.LEFT);
            addText(slide, x + 0.18, y + 0.55, 3.3, 1.15, card.body, 10, false, TM, TextParagraph.TextAlign.LEFT);
            if (i % 3 == 2) {
                x = 0.8;
                y += 2.45;
            } else {
                x += 4.2;
            }
        }

        addText(slide, 0.8, 6.8, 11.8, 0.25,
                "答辩建议：先重点讲工作流引擎，再用一图一页的业务页快速证明系统完整性。",
                10, false, new Color(190, 195, 215), TextParagraph.TextAlign.CENTER);
    }

    private void thanks() {
        XSLFSlide slide = slide();
        background(slide, BM);
        addRect(slide, 0, 7.2, 13.333, 0.05, P);
        addText(slide, 1.0, 2.4, 11.0, 0.9, "感谢聆听", 34, true, W, TextParagraph.TextAlign.CENTER);
        addText(slide, 1.0, 3.35, 11.0, 0.4, "欢迎老师提问与指正", 18, false,
                new Color(180, 180, 200), TextParagraph.TextAlign.CENTER);
        addText(slide, 2.0, 4.55, 9.2, 0.8,
                "企业 OA 办公自动化系统\n基于 Spring Boot 3.4 + Vue 3\n答辩人：[姓名] | 指导老师：[教师姓名] | 2026 年 6 月",
                11, false, new Color(150, 150, 175), TextParagraph.TextAlign.CENTER);
    }

    private XSLFSlide imageFeatureSlide(String title, String subtitle, String imageName,
                                        List<String> bullets, String footer) throws IOException {
        XSLFSlide slide = sectionSlide(title, subtitle);

        addImageCard(slide, 0.45, 1.25, 8.55, 5.45, imageName);

        addRound(slide, 9.15, 1.35, 3.75, 4.95, BG);
        addText(slide, 9.38, 1.58, 3.2, 0.25, "本页展示重点", 14, true, T, TextParagraph.TextAlign.LEFT);
        addBulletList(slide, 9.35, 1.95, 3.15, 3.2, bullets, 10, TM);

        addRound(slide, 0.55, 6.85, 12.3, 0.35, BG);
        addText(slide, 0.8, 6.9, 11.8, 0.22, footer, 9, false, T, TextParagraph.TextAlign.LEFT);
        return slide;
    }

    private XSLFSlide sectionSlide(String title, String subtitle) {
        XSLFSlide slide = slide();
        background(slide, W);
        addRect(slide, 0, 0, 13.333, 0.05, P);
        addText(slide, 0.8, 0.38, 10.2, 0.35, title, 22, true, T, TextParagraph.TextAlign.LEFT);
        addText(slide, 0.8, 0.78, 12.0, 0.22, subtitle, 10, false, TS, TextParagraph.TextAlign.LEFT);
        return slide;
    }

    private void addTechBands(XSLFSlide slide, List<LabeledText> items, double x, double y, double w, double h) {
        double currentX = x;
        for (LabeledText item : items) {
            addRound(slide, currentX, y, w, h, BG);
            addText(slide, currentX + 0.18, y + 0.12, w - 0.35, 0.18, item.label, 9, true, P, TextParagraph.TextAlign.LEFT);
            addText(slide, currentX + 0.18, y + 0.32, w - 0.35, 0.3, item.text, 8, false, TM, TextParagraph.TextAlign.LEFT);
            currentX += 4.15;
        }
    }

    private void addMetricCardRow(XSLFSlide slide, List<Metric> metrics, double x, double y, double w, double h) {
        double currentX = x;
        for (Metric metric : metrics) {
            addRound(slide, currentX, y, w, h, BG);
            addText(slide, currentX, y + 0.2, w, 0.28, metric.value, 16, true, P, TextParagraph.TextAlign.CENTER);
            addText(slide, currentX, y + 0.55, w, 0.18, metric.label, 8, false, TS, TextParagraph.TextAlign.CENTER);
            currentX += 2.45;
        }
    }

    private void addTagRow(XSLFSlide slide, List<Tag> tags, double startX, double startY, double w, double h, int perRow) {
        for (int i = 0; i < tags.size(); i++) {
            Tag tag = tags.get(i);
            double x = startX + (i % perRow) * (w + 0.12);
            double y = startY + (i / perRow) * (h + 0.12);
            addRound(slide, x, y, w, h, tag.color);
            addText(slide, x, y + 0.04, w, 0.2, tag.label, 8, true, W, TextParagraph.TextAlign.CENTER);
        }
    }

    private void addFlowNode(XSLFSlide slide, double x, double y, double w, double h, String text, Color color) {
        addRound(slide, x, y, w, h, color);
        addText(slide, x, y + 0.07, w, 0.2, text, 10, true, W, TextParagraph.TextAlign.CENTER);
    }

    private void addArrowText(XSLFSlide slide, double x, double y, String text) {
        addText(slide, x, y, 0.8, 0.2, "→", 18, true, TS, TextParagraph.TextAlign.CENTER);
        addText(slide, x - 0.1, y + 0.24, 1.0, 0.18, text, 8, false, TS, TextParagraph.TextAlign.CENTER);
    }

    private void addCallout(XSLFSlide slide, double x, double y, double w, double h,
                            String title, String body, Color fill, Color textColor) {
        addRound(slide, x, y, w, h, fill);
        addText(slide, x + 0.18, y + 0.12, w - 0.35, 0.18, title, 10, true, textColor, TextParagraph.TextAlign.LEFT);
        addText(slide, x + 0.18, y + 0.33, w - 0.35, h - 0.38, body, 8, false, textColor, TextParagraph.TextAlign.LEFT);
    }

    private void addImageCard(XSLFSlide slide, double x, double y, double w, double h, String imageName) throws IOException {
        addRound(slide, x, y, w, h, BG);
        addRect(slide, x, y, w, 0.05, P);
        Path image = IMG_DIR.resolve(imageName);
        if (!Files.exists(image)) {
            addText(slide, x + 0.2, y + 0.2, w - 0.4, h - 0.4,
                    "Missing image: " + imageName, 16, true, R, TextParagraph.TextAlign.CENTER);
            return;
        }

        Rectangle fitted = fitImage(image.toFile(), x + 0.12, y + 0.12, w - 0.24, h - 0.24);
        XSLFPictureData pictureData;
        try (FileInputStream in = new FileInputStream(image.toFile())) {
            pictureData = ppt.addPicture(in, PictureData.PictureType.PNG);
        }
        slide.createPicture(pictureData).setAnchor(fitted);
    }

    private Rectangle fitImage(File imageFile, double x, double y, double w, double h) throws IOException {
        var image = javax.imageio.ImageIO.read(imageFile);
        double srcW = image.getWidth();
        double srcH = image.getHeight();
        double boxW = inches(w);
        double boxH = inches(h);
        double scale = Math.min(boxW / srcW, boxH / srcH);
        int finalW = (int) Math.round(srcW * scale);
        int finalH = (int) Math.round(srcH * scale);
        int left = inches(x) + (int) Math.round((boxW - finalW) / 2);
        int top = inches(y) + (int) Math.round((boxH - finalH) / 2);
        return new Rectangle(left, top, finalW, finalH);
    }

    private void addBulletList(XSLFSlide slide, double x, double y, double w, double h, List<String> lines, int fontSize, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(rect(x, y, w, h));
        box.setInsets(new Insets2D(0, 0, 0, 0));
        boolean first = true;
        for (String line : lines) {
            XSLFTextParagraph p = first ? box.getTextParagraphs().get(0) : box.addNewTextParagraph();
            first = false;
            p.setSpaceAfter(4.0);
            p.setLeftMargin(16.0);
            p.setIndent(-10.0);
            p.setBullet(true);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(line);
            r.setFontFamily("Microsoft YaHei");
            r.setFontColor(color);
            r.setFontSize((double) fontSize);
        }
    }

    private void addTable(XSLFSlide slide, double x, double y, double w, double h,
                          List<String> headers, List<List<String>> rows, List<Double> colWidthsInches) {
        XSLFTable table = slide.createTable();
        table.setAnchor(rect(x, y, w, h));

        XSLFTableRow headerRow = table.addRow();
        headerRow.setHeight(inchesToPoints(0.35));
        for (int i = 0; i < headers.size(); i++) {
            XSLFTableCell cell = headerRow.addCell();
            formatCell(cell, headers.get(i), P, W, true, 9);
            table.setColumnWidth(i, inches(colWidthsInches.get(i)));
        }

        for (List<String> rowData : rows) {
            XSLFTableRow row = table.addRow();
            row.setHeight(inchesToPoints(0.32));
            for (String cellText : rowData) {
                XSLFTableCell cell = row.addCell();
                formatCell(cell, cellText, W, T, false, 8);
            }
        }
    }

    private void formatCell(XSLFTableCell cell, String text, Color fill, Color textColor, boolean bold, int fontSize) {
        cell.setFillColor(fill);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setLeftInset(3.0);
        cell.setRightInset(3.0);
        cell.setTopInset(2.0);
        cell.setBottomInset(2.0);
        cell.setBorderColor(XSLFTableCell.BorderEdge.bottom, BG);
        cell.setBorderColor(XSLFTableCell.BorderEdge.top, BG);
        cell.setBorderColor(XSLFTableCell.BorderEdge.left, BG);
        cell.setBorderColor(XSLFTableCell.BorderEdge.right, BG);

        XSLFTextParagraph p = cell.getTextParagraphs().isEmpty()
                ? cell.addNewTextParagraph()
                : cell.getTextParagraphs().get(0);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text);
        r.setFontFamily("Microsoft YaHei");
        r.setFontColor(textColor);
        r.setFontSize((double) fontSize);
        r.setBold(bold);
    }

    private XSLFSlide slide() {
        return ppt.createSlide();
    }

    private void background(XSLFSlide slide, Color color) {
        slide.getBackground().setFillColor(color);
    }

    private void addRect(XSLFSlide slide, double x, double y, double w, double h, Color color) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.RECT);
        shape.setAnchor(rect(x, y, w, h));
        shape.setFillColor(color);
        shape.setLineColor(color);
    }

    private void addRound(XSLFSlide slide, double x, double y, double w, double h, Color color) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.ROUND_RECT);
        shape.setAnchor(rect(x, y, w, h));
        shape.setFillColor(color);
        shape.setLineColor(color);
    }

    private void addText(XSLFSlide slide, double x, double y, double w, double h,
                         String text, int fontSize, boolean bold, Color color, TextParagraph.TextAlign align) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(rect(x, y, w, h));
        box.setInsets(new Insets2D(0, 0, 0, 0));
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            XSLFTextParagraph p = i == 0 ? box.getTextParagraphs().get(0) : box.addNewTextParagraph();
            p.setTextAlign(align);
            p.setSpaceAfter(0.0);
            p.setLineSpacing(110.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(lines[i]);
            r.setFontFamily("Microsoft YaHei");
            r.setFontSize((double) fontSize);
            r.setFontColor(color);
            r.setBold(bold);
        }
    }

    private Rectangle rect(double x, double y, double w, double h) {
        return new Rectangle(inches(x), inches(y), inches(w), inches(h));
    }

    private int inches(double value) {
        return (int) Math.round(value * Units.EMU_PER_INCH);
    }

    private double inchesToPoints(double inches) {
        return inches * 72;
    }

    private record Metric(String value, String label) {}
    private record LabeledText(String label, String text) {}
    private record Tag(String label, Color color) {}
    private record Card(String title, String body, Color color) {}
}
