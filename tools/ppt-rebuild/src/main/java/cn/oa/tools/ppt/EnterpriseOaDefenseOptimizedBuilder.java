package cn.oa.tools.ppt;

import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EnterpriseOaDefenseOptimizedBuilder {

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path OUTPUT = ROOT.resolve("企业OA办公系统项目答辩_重新优化版_v3.pptx");
    private static final Path IMG_DIR = ROOT.resolve("系统演示图片");

    private static final Color NAVY = new Color(0x18, 0x2A, 0x4D);
    private static final Color BLUE = new Color(0x2F, 0x63, 0xD8);
    private static final Color SKY = new Color(0x4F, 0x8D, 0xFF);
    private static final Color LIGHT = new Color(0xF7, 0xFA, 0xFE);
    private static final Color TEXT = new Color(0x17, 0x24, 0x3A);
    private static final Color SUB = new Color(0x5E, 0x6B, 0x81);
    private static final Color MUTED = new Color(0x91, 0x9D, 0xB3);
    private static final Color LINE = new Color(0xD9, 0xE4, 0xF5);
    private static final Color GREEN = new Color(0x11, 0x8A, 0x63);
    private static final Color ORANGE = new Color(0xE2, 0x73, 0x18);
    private static final Color PURPLE = new Color(0x6E, 0x53, 0xC9);
    private static final Color RED = new Color(0xCF, 0x4B, 0x4B);
    private static final Color WHITE = Color.WHITE;

    private final XMLSlideShow ppt = new XMLSlideShow();

    public static void main(String[] args) throws Exception {
        new EnterpriseOaDefenseOptimizedBuilder().build();
    }

    private void build() throws Exception {
        ppt.setPageSize(new Dimension(pointsInt(13.333), pointsInt(7.5)));

        cover();
        projectOverview();
        architecture();
        workflowHighlight();
        workflowDesigner();
        login();
        workbench();
        leave();
        todo();
        attendance();
        message();
        schedule();
        dashboard();
        employee();
        rolePermission();
        redis();
        security();
        summary();

        try (FileOutputStream out = new FileOutputStream(OUTPUT.toFile())) {
            ppt.write(out);
        }

        System.out.println("Generated: " + OUTPUT);
        System.out.println("Slides: " + ppt.getSlides().size());
    }

    private void cover() throws IOException {
        XSLFSlide slide = slide();
        bg(slide, WHITE);
        ribbon(slide);
        text(slide, 0.9, 0.8, 3.8, 1.0, "企业OA办公系统", 28, true, NAVY, TextParagraph.TextAlign.LEFT);
        text(slide, 0.9, 1.55, 4.0, 0.6, "设计与实现", 28, true, BLUE, TextParagraph.TextAlign.LEFT);
        text(slide, 0.9, 2.5, 5.6, 0.9,
                "基于 Spring Boot 3.4 + Vue 3 的企业级办公管理平台",
                16, false, SUB, TextParagraph.TextAlign.LEFT);
        text(slide, 0.9, 3.4, 5.6, 1.1,
                "围绕审批、考勤、协同、权限与数据可视化等核心场景，\n构建一套功能完整、架构清晰、可扩展的企业办公系统。",
                13, false, TEXT, TextParagraph.TextAlign.LEFT);
        metricPill(slide, 0.9, 5.1, 1.25, 0.42, "8 大业务域", BLUE);
        metricPill(slide, 2.25, 5.1, 1.45, 0.42, "50+ 功能模块", GREEN);
        metricPill(slide, 3.85, 5.1, 1.8, 0.42, "自研工作流引擎", PURPLE);
        text(slide, 0.9, 6.45, 2.4, 0.25, "2026", 26, true, NAVY, TextParagraph.TextAlign.LEFT);
        imageCard(slide, 7.3, 0.95, 5.15, 5.8, "工作台.png", false);
    }

    private void projectOverview() {
        XSLFSlide slide = section("项目概述", "打造高效协同的数字化办公平台");
        twoColumnIntro(slide,
                "项目定位",
                "面向企业内部高频办公业务，围绕审批、考勤、消息、人事、看板与系统管理等场景，构建一体化的线上协同平台，减少线下流程和人工统计成本。",
                "核心目标",
                "审批流程灵活配置、权限控制精细可靠、数据全过程可追溯，并通过前后端分离架构支持后续模块持续扩展。");
        statRow(slide, List.of(
                new Stat("8", "业务域"),
                new Stat("50+", "功能模块"),
                new Stat("7+", "审批类型"),
                new Stat("3", "客户端形态")
        ), 0.9, 4.65);
        bulletBand(slide, 0.9, 5.55, 11.6, 0.9, "场景覆盖",
                List.of("行政 / 人事 / 审批 / 协同 / 资产 / 财务 / 报表 / 系统管理"));
    }

    private void architecture() {
        XSLFSlide slide = section("系统架构总览", "前后端分离 + 多层解耦的现代化系统蓝图");
        flowBox(slide, 1.0, 1.65, 2.0, 0.55, "前端交互层", BLUE, "Vue 3 + TypeScript + Vite + Element Plus");
        arrow(slide, 3.2, 1.92);
        flowBox(slide, 3.8, 1.65, 2.2, 0.55, "接口与认证层", PURPLE, "Axios / JWT / 拦截器 / RBAC");
        arrow(slide, 6.25, 1.92);
        flowBox(slide, 6.85, 1.65, 2.2, 0.55, "业务服务层", GREEN, "Spring Boot 3 + MyBatis-Plus");
        arrow(slide, 9.3, 1.92);
        flowBox(slide, 9.9, 1.65, 2.2, 0.55, "存储与缓存层", ORANGE, "MySQL 8 + Redis");
        infoCard(slide, 0.9, 2.9, 3.75, 2.7, "前端层职责",
                List.of("构建高交互管理界面", "管理路由、状态与表单", "实现图表渲染与权限可见性控制"), LIGHT);
        infoCard(slide, 4.8, 2.9, 3.75, 2.7, "后端层职责",
                List.of("承担认证授权与业务逻辑", "封装工作流运行能力", "统一响应、异常、日志与事务管理"), LIGHT);
        infoCard(slide, 8.7, 2.9, 3.75, 2.7, "数据层职责",
                List.of("关系数据持久化", "缓存会话与分布式锁", "通过 Flyway 管理表结构演进"), LIGHT);
        bulletBand(slide, 0.9, 6.05, 11.6, 0.8, "后端模块划分",
                List.of("oa-common、oa-model、oa-mapper、oa-service、oa-web 五层清晰解耦"));
    }

    private void workflowHighlight() {
        XSLFSlide slide = section("核心亮点：自研工作流引擎", "轻量级、可配置、贴合 OA 业务特性的审批引擎");
        infoCard(slide, 0.9, 1.55, 4.0, 2.7, "为什么不直接用 Flowable？",
                List.of("OA 场景更强调审批与路由，不需要完整 BPM 能力", "自研方案表更少、依赖更轻、逻辑更容易解释", "审批人策略可以按企业层级关系灵活扩展"), LIGHT);
        table(slide, 5.15, 1.55, 7.0, 2.7,
                List.of("对比项", "Flowable", "自研方案"),
                List.of(
                        List.of("依赖规模", "~5MB", "~2000 行核心逻辑"),
                        List.of("数据表", "30+", "4 张工作流核心表"),
                        List.of("学习成本", "需要 BPMN 理解", "JSON 图结构更直观"),
                        List.of("定制方式", "偏引擎约束", "按业务策略灵活扩展")
                ),
                List.of(1.3, 2.0, 2.6));
        infoCard(slide, 0.9, 4.6, 3.8, 1.7, "引擎核心组件",
                List.of("流程定义、流程实例、任务、运行时引擎、业务回调"), WHITE);
        infoCard(slide, 4.95, 4.6, 3.8, 1.7, "路由维度",
                List.of("金额、天数、小时数、角色级别"), WHITE);
        infoCard(slide, 9.0, 4.6, 3.5, 1.7, "覆盖业务",
                List.of("请假、出差、报销、采购、加班、外出、借支"), WHITE);
    }

    private void workflowDesigner() throws IOException {
        featureImagePage("核心功能：流程定义设计器",
                "可视化设计器 → JSON 图结构 → 引擎执行，零代码配置审批流程",
                "流程自定义.png",
                List.of(
                        "通过前端界面配置节点、条件和审批人策略",
                        "支持金额分级、时长分级等模板化流程设计",
                        "保存前自动完成环路、连通性与配置完整性校验",
                        "每次编辑自动形成版本，运行中的实例使用快照隔离"
                ),
                List.of(
                        "管理员进入流程定义页面后，可以新增、编辑并在线激活不同业务类型的审批流。",
                        "设计器内部维护“审批步骤 + 分级规则”两套状态，既能描述基础审批链，也能附加条件路由。",
                        "后端保存的是 JSON 图结构，运行时再由工作流引擎解析成真正的审批路径。"
                ),
                new AccentSet(PURPLE, SKY, GREEN));
    }

    private void login() throws IOException {
        featureImagePage("核心功能：系统登录",
                "眼球追踪动画 + 图形验证码 + JWT 完整认证链路",
                "登录页.png",
                List.of(
                        "Pupil / EyeBall 组件实现交互式视觉效果",
                        "验证码 + BCrypt + JWT 构成登录第一道安全防线",
                        "Token 存储与刷新机制配合 Redis 完成会话控制",
                        "兼具体验亮点与技术可讲性"
                ),
                List.of(
                        "登录流程会先经过验证码校验，再完成员工查询、密码比对、角色加载和 Token 签发。",
                        "前端请求拦截器会检测 Token 是否即将过期，并在必要时自动调用 refresh-token 接口续期。",
                        "这页既能体现前端交互打磨，也能自然引出后面的认证体系设计。"
                ),
                new AccentSet(BLUE, ORANGE, PURPLE));
    }

    private void workbench() throws IOException {
        featureImagePage("核心功能：智能工作台",
                "信息聚合中枢，待办、数据、快捷入口一站式触达",
                "工作台.png",
                List.of(
                        "登录后直接进入综合工作入口",
                        "聚合待办事项、考勤状态、通知与快捷操作",
                        "让多个业务模块以产品化方式集中呈现",
                        "很适合体现系统的整体性和使用效率"
                ),
                List.of(
                        "工作台作为个人首页，既展示今日考勤状态，也提供请假申请、我的日程等高频业务的快捷入口。",
                        "这一页的价值不只是信息展示，更在于把分散模块重新组织成“用户的一天”视角。",
                        "答辩时可以强调：这说明系统设计考虑了真实使用路径，而不是只做功能堆砌。"
                ),
                new AccentSet(GREEN, SKY, ORANGE));
    }

    private void leave() throws IOException {
        featureImagePage("核心功能：请假申请",
                "业务表单、工作流与回调联动的典型闭环场景",
                "请假申请.png",
                List.of(
                        "支持多种假期类型与时间区间配置",
                        "提交表单后自动触发工作流实例",
                        "审批完成后回调扣减假期余额并更新考勤状态",
                        "是一条非常适合答辩讲解的完整业务链"
                ),
                List.of(
                        "请假单提交时，系统会自动计算请假天数，并把业务数据作为工作流上下文传给引擎。",
                        "审批通过后不仅修改请假单状态，还会自动扣减对应假期额度，并同步标记考勤结果。",
                        "这页最适合讲“业务表单 -> 工作流 -> 业务回调”的完整链路。"
                ),
                new AccentSet(PURPLE, RED, SKY));
    }

    private void todo() throws IOException {
        featureImagePage("核心功能：我的待办",
                "统一待办枢纽，聚合全业务类型的审批任务",
                "我的待办.png",
                List.of(
                        "统一呈现待办、已办与需要跟进的流程任务",
                        "减少用户在多个业务入口之间来回切换",
                        "适合衔接工作流引擎的运行结果说明",
                        "可以强调“配置端 + 使用端”完整闭环"
                ),
                List.of(
                        "待办中心集中展示请假、出差、采购、报销等多个业务流程产生的任务。",
                        "用户不需要分别进入多个模块，只需在一个入口完成审批、催办或转办操作。",
                        "这页可以很好地说明工作流引擎对上层业务页面的统一支撑作用。"
                ),
                new AccentSet(SKY, BLUE, GREEN));
    }

    private void attendance() throws IOException {
        featureImagePage("核心功能：考勤打卡",
                "上下班打卡 + 自动迟到早退判断 + 审批联动",
                "考勤打卡.png",
                List.of(
                        "同一页面完成上班和下班打卡操作",
                        "系统自动根据考勤组规则判断迟到与早退",
                        "考勤与请假、出差审批状态自动联动",
                        "后续还能支持月报查询和导出"
                ),
                List.of(
                        "后端会先检查今日是否已有记录，再根据当前时间与考勤组规则判断状态，比如正常、迟到或早退。",
                        "考勤表不仅记录打卡时间，也会存储考勤状态、备注和审计信息，便于后续统计。",
                        "审批通过的请假和出差，会自动把当天考勤标记成对应业务状态，而不是让 HR 手工修改。"
                ),
                new AccentSet(GREEN, ORANGE, RED));
    }

    private void message() throws IOException {
        featureImagePage("核心功能：消息中心",
                "待办、站内信与实时推送组成协同触达体系",
                "消息中心.png",
                List.of(
                        "消息可持久化存储，也可实时触达用户",
                        "既可服务于审批提醒，也可服务于公告通知",
                        "与 WebSocket 和待办能力形成多层提醒网络",
                        "体现系统不只是审批工具，也是协同平台"
                ),
                List.of(
                        "消息表采用发送人、接收人、标题、内容和已读状态模型，便于实现未读数统计和消息列表分页。",
                        "当新消息产生时，既可以写入数据库，也可以通过 WebSocket 立即推送到前端更新未读提醒。",
                        "这说明系统的通知能力不是单点实现，而是“持久化 + 实时触达”的组合方案。"
                ),
                new AccentSet(ORANGE, PURPLE, SKY));
    }

    private void schedule() throws IOException {
        featureImagePage("核心功能：我的日程",
                "会议预约、日程共享与冲突检测，提升协同效率",
                "我的日程.png",
                List.of(
                        "提供个人日程和共享日程管理能力",
                        "支持时间冲突识别与协同安排",
                        "强化系统在协同办公方面的完整性",
                        "适合作为“非审批类核心业务”的补充展示"
                ),
                List.of(
                        "日程模块让系统不只停留在审批和表单，而是进一步覆盖协同办公中的时间管理场景。",
                        "它可以和会议、消息通知、工作台形成联动，增强整个系统的办公协同属性。",
                        "这页适合用来说明：项目的业务覆盖面不止行政审批，还包含协作效率提升。"
                ),
                new AccentSet(SKY, GREEN, PURPLE));
    }

    private void dashboard() throws IOException {
        featureImagePage("核心功能：数据看板",
                "ECharts 可视化管理驾驶舱，一眼看懂业务状态",
                "数据看板1.png",
                List.of(
                        "把原始业务数据转成趋势图、热力图、排行等可视化图表",
                        "既支撑员工业务状态查看，也支撑管理层决策",
                        "体现系统不止有录入和审批，还有数据价值输出",
                        "适合作为答辩中的视觉亮点页"
                ),
                List.of(
                        "数据看板通过 ECharts 把考勤、负载、活跃度和业务趋势转成更容易理解的图形表达。",
                        "相比普通统计列表，看板更能突出系统对管理层的价值，而不仅仅是操作层价值。",
                        "答辩时可以把它作为“从数据沉淀走向数据利用”的代表页面。"
                ),
                new AccentSet(BLUE, GREEN, ORANGE));
    }

    private void employee() throws IOException {
        featureImagePage("核心功能：员工管理",
                "员工全生命周期数据管理与组织基础信息维护",
                "员工管理.png",
                List.of(
                        "维护员工账号、状态、档案等核心基础数据",
                        "与部门、角色、岗位、审批链条能力强关联",
                        "是权限控制和业务路由的基础支撑层",
                        "体现系统管理员视角的管理能力"
                ),
                List.of(
                        "员工基础数据是审批路由、权限分配和组织结构管理的上游依赖。",
                        "系统支持员工状态控制、逻辑删除和角色关系绑定，保证历史数据与权限链条稳定。",
                        "这页可以自然衔接到角色权限管理和系统管理能力说明。"
                ),
                new AccentSet(PURPLE, BLUE, GREEN));
    }

    private void rolePermission() throws IOException {
        featureImagePage("核心功能：角色与权限管理",
                "注解式鉴权 + 角色菜单矩阵 + 前端动态路由",
                "角色管理.png",
                List.of(
                        "角色是用户、菜单和权限码之间的桥梁",
                        "前端页面可见性与后端接口鉴权形成双保险",
                        "支持更细颗粒度的能力控制，而非简单页面隐藏",
                        "非常适合用于解释 RBAC 权限模型"
                ),
                List.of(
                        "RBAC 模型通过用户、角色、菜单和权限码的关系，把系统功能做成可组合、可授权的能力集合。",
                        "前端根据角色动态渲染页面入口，后端再通过注解和拦截器完成最终的接口级校验。",
                        "这样可以兼顾用户体验和安全性，也更符合企业级后台系统的权限设计要求。"
                ),
                new AccentSet(RED, PURPLE, SKY));
    }

    private void redis() {
        XSLFSlide slide = section("Redis 六大核心应用场景", "一套缓存中间件在系统中发挥多重关键作用");
        smallInfo(slide, 0.9, 1.6, 3.75, 1.2, "JWT Token 管理", "登录态缓存、主动失效、强制下线", BLUE);
        smallInfo(slide, 4.8, 1.6, 3.75, 1.2, "登录 IP 限流", "INCR + EXPIRE 组成轻量限流策略", ORANGE);
        smallInfo(slide, 8.7, 1.6, 3.75, 1.2, "在线用户管理", "心跳、在线状态与管理员踢下线", GREEN);
        smallInfo(slide, 0.9, 3.2, 3.75, 1.2, "验证码存储", "一次性校验与自动过期清理", PURPLE);
        smallInfo(slide, 4.8, 3.2, 3.75, 1.2, "Token 自动续期", "临近过期时自动刷新会话", SKY);
        smallInfo(slide, 8.7, 3.2, 3.75, 1.2, "分布式锁", "会签、或签等并发场景防止重复推进", RED);
        bulletBand(slide, 0.9, 5.45, 11.6, 1.0, "答辩可强调",
                List.of("Redis 不是只做缓存，而是承担认证、限流、实时状态和并发控制等多类关键职责。"));
    }

    private void security() {
        XSLFSlide slide = section("安全与权限设计", "四层防护体系，筑牢系统安全防线");
        infoCard(slide, 0.9, 1.55, 3.75, 2.8, "登录入口防护",
                List.of("验证码校验", "密码加密存储", "IP 级限流防暴力破解"), LIGHT);
        infoCard(slide, 4.8, 1.55, 3.75, 2.8, "接口身份认证",
                List.of("JWT 解析身份", "Redis 控制 Token 有效性", "全局拦截器统一校验"), LIGHT);
        infoCard(slide, 8.7, 1.55, 3.75, 2.8, "RBAC 权限控制",
                List.of("角色、菜单、权限码关联", "注解式接口鉴权", "前端路由动态控制"), LIGHT);
        bulletBand(slide, 0.9, 4.75, 11.6, 1.05, "安全设计价值",
                List.of("兼顾无状态认证、服务端可控性与业务操作审计，是前后端分离系统中比较完整的一套安全方案。"));
    }

    private void summary() {
        XSLFSlide slide = section("总结与展望", "架构决策回顾与项目价值凝练");
        infoCard(slide, 0.9, 1.55, 3.75, 3.0, "本次项目成果",
                List.of("完成 8 大业务域与多模块协同办公能力", "自研工作流引擎支撑多类审批业务", "形成从业务表单到回调联动的完整闭环"), LIGHT);
        infoCard(slide, 4.8, 1.55, 3.75, 3.0, "技术价值",
                List.of("体现了分层架构、权限体系、缓存设计和流程抽象能力", "兼顾业务完整性与工程可维护性", "具有毕业设计答辩中的“技术亮点可讲性”"), LIGHT);
        infoCard(slide, 8.7, 1.55, 3.75, 3.0, "后续优化方向",
                List.of("移动端和小程序进一步完善", "对象存储替代本地文件", "企业微信 / 钉钉等外部系统集成"), LIGHT);
        text(slide, 0.9, 5.3, 11.6, 0.55,
                "答辩建议：优先讲工作流引擎，再用核心业务截图页证明系统完整性，最后用 Redis 与安全设计提升技术深度。",
                12, false, NAVY, TextParagraph.TextAlign.LEFT);
    }

    private XSLFSlide section(String title, String subtitle) {
        XSLFSlide slide = slide();
        bg(slide, WHITE);
        ribbon(slide);
        text(slide, 0.9, 0.5, 4.8, 0.45, title, 24, true, NAVY, TextParagraph.TextAlign.LEFT);
        text(slide, 0.9, 0.92, 6.3, 0.3, subtitle, 11, false, SUB, TextParagraph.TextAlign.LEFT);
        return slide;
    }

    private void featureImagePage(String title, String subtitle, String imageName, List<String> bullets,
                                  List<String> detailLines, AccentSet accents) throws IOException {
        XSLFSlide slide = section(title, subtitle);
        imageCard(slide, 0.82, 1.45, 7.95, 4.95, imageName, true);
        infoCard(slide, 9.0, 1.45, 3.52, 2.1, "本页展示重点", bullets, accents.primary, true, WHITE);
        infoCard(slide, 9.0, 3.72, 3.52, 2.68, "实现补充说明", detailLines, accents.secondary, false, TEXT);
        bulletBand(slide, 0.9, 6.55, 11.6, 0.52, "答辩提示",
                List.of(detailLines.get(detailLines.size() - 1)));
    }

    private void twoColumnIntro(XSLFSlide slide, String leftTitle, String leftBody, String rightTitle, String rightBody) {
        infoCard(slide, 0.9, 1.55, 5.55, 2.45, leftTitle, List.of(leftBody), LIGHT, false);
        infoCard(slide, 6.75, 1.55, 5.55, 2.45, rightTitle, List.of(rightBody), LIGHT, false);
    }

    private void statRow(XSLFSlide slide, List<Stat> stats, double x, double y) {
        double cx = x;
        for (Stat stat : stats) {
            round(slide, cx, y, 2.65, 1.1, LIGHT, LINE);
            text(slide, cx, y + 0.2, 2.65, 0.28, stat.value, 18, true, BLUE, TextParagraph.TextAlign.CENTER);
            text(slide, cx, y + 0.58, 2.65, 0.18, stat.label, 9, false, SUB, TextParagraph.TextAlign.CENTER);
            cx += 2.9;
        }
    }

    private void bulletBand(XSLFSlide slide, double x, double y, double w, double h, String title, List<String> bullets) {
        round(slide, x, y, w, h, LIGHT, LINE);
        text(slide, x + 0.2, y + 0.14, 1.7, 0.18, title, 10, true, BLUE, TextParagraph.TextAlign.LEFT);
        text(slide, x + 1.65, y + 0.12, w - 1.9, h - 0.2, String.join("  ·  ", bullets), 10, false, TEXT, TextParagraph.TextAlign.LEFT);
    }

    private void flowBox(XSLFSlide slide, double x, double y, double w, double h, String title, Color color, String desc) {
        round(slide, x, y, w, h, color, color);
        text(slide, x, y + 0.1, w, 0.18, title, 11, true, WHITE, TextParagraph.TextAlign.CENTER);
        round(slide, x, y + 0.75, w, 0.65, LIGHT, LINE);
        text(slide, x + 0.1, y + 0.92, w - 0.2, 0.24, desc, 8, false, SUB, TextParagraph.TextAlign.CENTER);
    }

    private void arrow(XSLFSlide slide, double x, double y) {
        text(slide, x, y - 0.03, 0.42, 0.2, "→", 18, true, MUTED, TextParagraph.TextAlign.CENTER);
    }

    private void infoCard(XSLFSlide slide, double x, double y, double w, double h, String title, List<String> bullets, Color fill) {
        infoCard(slide, x, y, w, h, title, bullets, fill, true);
    }

    private void infoCard(XSLFSlide slide, double x, double y, double w, double h, String title, List<String> bullets, Color fill, boolean bulletMode) {
        infoCard(slide, x, y, w, h, title, bullets, fill, bulletMode, NAVY);
    }

    private void infoCard(XSLFSlide slide, double x, double y, double w, double h, String title, List<String> bullets,
                          Color fill, boolean bulletMode, Color textColor) {
        round(slide, x, y, w, h, fill, LINE);
        text(slide, x + 0.18, y + 0.16, w - 0.36, 0.2, title, 13, true, textColor, TextParagraph.TextAlign.LEFT);
        if (bulletMode) {
            bulletText(slide, x + 0.16, y + 0.48, w - 0.32, h - 0.6, bullets, 10, textColor);
        } else {
            text(slide, x + 0.18, y + 0.52, w - 0.36, h - 0.66, String.join("\n", bullets), 10, false, textColor, TextParagraph.TextAlign.LEFT);
        }
    }

    private void smallInfo(XSLFSlide slide, double x, double y, double w, double h, String title, String body, Color color) {
        round(slide, x, y, w, h, WHITE, LINE);
        rect(slide, x, y, w, 0.08, color, color);
        text(slide, x + 0.18, y + 0.18, w - 0.36, 0.18, title, 11, true, NAVY, TextParagraph.TextAlign.LEFT);
        text(slide, x + 0.18, y + 0.48, w - 0.36, 0.42, body, 9, false, SUB, TextParagraph.TextAlign.LEFT);
    }

    private void metricPill(XSLFSlide slide, double x, double y, double w, double h, String text, Color color) {
        round(slide, x, y, w, h, color, color);
        text(slide, x, y + 0.07, w, 0.15, text, 9, true, WHITE, TextParagraph.TextAlign.CENTER);
    }

    private void imageCard(XSLFSlide slide, double x, double y, double w, double h, String imageName, boolean addCaption) throws IOException {
        round(slide, x, y, w, h, WHITE, LINE);
        Path image = IMG_DIR.resolve(imageName);
        if (Files.exists(image)) {
            try (FileInputStream in = new FileInputStream(image.toFile())) {
                XSLFPictureData data = ppt.addPicture(in, PictureData.PictureType.PNG);
                BufferedImage img = ImageIO.read(image.toFile());
                Rectangle fit = fit(img, x + 0.12, y + 0.12, w - 0.24, h - 0.24);
                slide.createPicture(data).setAnchor(fit);
            }
        } else {
            text(slide, x + 0.2, y + 0.2, w - 0.4, h - 0.4, "缺少截图：" + imageName, 16, true, RED, TextParagraph.TextAlign.CENTER);
        }
        if (addCaption) {
            rect(slide, x, y + h - 0.45, w, 0.45, new Color(255, 255, 255, 220), WHITE);
            text(slide, x + 0.14, y + h - 0.33, w - 0.28, 0.14, imageName.replace(".png", ""), 8, false, SUB, TextParagraph.TextAlign.LEFT);
        }
    }

    private void table(XSLFSlide slide, double x, double y, double w, double h, List<String> headers,
                       List<List<String>> rows, List<Double> widths) {
        XSLFTable table = slide.createTable();
        table.setAnchor(rect(x, y, w, h));
        XSLFTableRow header = table.addRow();
        header.setHeight(points(0.36));
        for (int i = 0; i < headers.size(); i++) {
            XSLFTableCell cell = header.addCell();
            styleCell(cell, headers.get(i), BLUE, WHITE, true, 9);
            table.setColumnWidth(i, points(widths.get(i)));
        }
        for (List<String> row : rows) {
            XSLFTableRow tr = table.addRow();
            tr.setHeight(points(0.34));
            for (String value : row) {
                XSLFTableCell cell = tr.addCell();
                styleCell(cell, value, WHITE, TEXT, false, 8);
            }
        }
    }

    private void styleCell(XSLFTableCell cell, String value, Color fill, Color color, boolean bold, int fontSize) {
        cell.setFillColor(fill);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setBorderColor(XSLFTableCell.BorderEdge.bottom, LINE);
        cell.setBorderColor(XSLFTableCell.BorderEdge.top, LINE);
        cell.setBorderColor(XSLFTableCell.BorderEdge.left, LINE);
        cell.setBorderColor(XSLFTableCell.BorderEdge.right, LINE);
        XSLFTextParagraph p = cell.getTextParagraphs().isEmpty() ? cell.addNewTextParagraph() : cell.getTextParagraphs().get(0);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(value);
        r.setFontFamily("Microsoft YaHei");
        r.setFontSize((double) fontSize);
        r.setBold(bold);
        r.setFontColor(color);
    }

    private void bulletText(XSLFSlide slide, double x, double y, double w, double h, List<String> lines, int size, Color color) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(rect(x, y, w, h));
        box.setInsets(new Insets2D(0, 0, 0, 0));
        for (int i = 0; i < lines.size(); i++) {
            XSLFTextParagraph p = i == 0 ? box.getTextParagraphs().get(0) : box.addNewTextParagraph();
            p.setLeftMargin(14.0);
            p.setIndent(-10.0);
            p.setBullet(true);
            p.setSpaceAfter(3.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(lines.get(i));
            r.setFontFamily("Microsoft YaHei");
            r.setFontSize((double) size);
            r.setFontColor(color);
        }
    }

    private Rectangle fit(BufferedImage image, double x, double y, double w, double h) {
        double boxW = points(w);
        double boxH = points(h);
        double scale = Math.min(boxW / image.getWidth(), boxH / image.getHeight());
        int finalW = (int) Math.round(image.getWidth() * scale);
        int finalH = (int) Math.round(image.getHeight() * scale);
        int left = pointsInt(x) + (int) Math.round((boxW - finalW) / 2);
        int top = pointsInt(y) + (int) Math.round((boxH - finalH) / 2);
        return new Rectangle(left, top, finalW, finalH);
    }

    private void ribbon(XSLFSlide slide) {
        rect(slide, 0, 0, 13.333, 0.08, BLUE, BLUE);
        rect(slide, 0, 7.38, 13.333, 0.04, LINE, LINE);
    }

    private XSLFSlide slide() {
        return ppt.createSlide();
    }

    private void bg(XSLFSlide slide, Color color) {
        slide.getBackground().setFillColor(color);
    }

    private void rect(XSLFSlide slide, double x, double y, double w, double h, Color fill, Color line) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.RECT);
        shape.setAnchor(rect(x, y, w, h));
        shape.setFillColor(fill);
        shape.setLineColor(line);
    }

    private void round(XSLFSlide slide, double x, double y, double w, double h, Color fill, Color line) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.ROUND_RECT);
        shape.setAnchor(rect(x, y, w, h));
        shape.setFillColor(fill);
        shape.setLineColor(line);
    }

    private void text(XSLFSlide slide, double x, double y, double w, double h, String content,
                      int size, boolean bold, Color color, TextParagraph.TextAlign align) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(rect(x, y, w, h));
        box.setInsets(new Insets2D(0, 0, 0, 0));
        String[] parts = content.split("\n");
        for (int i = 0; i < parts.length; i++) {
            XSLFTextParagraph p = i == 0 ? box.getTextParagraphs().get(0) : box.addNewTextParagraph();
            p.setTextAlign(align);
            p.setLineSpacing(112.0);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(parts[i]);
            r.setFontFamily("Microsoft YaHei");
            r.setFontSize((double) size);
            r.setBold(bold);
            r.setFontColor(color);
        }
    }

    private Rectangle rect(double x, double y, double w, double h) {
        return new Rectangle(pointsInt(x), pointsInt(y), pointsInt(w), pointsInt(h));
    }

    private double points(double inch) {
        return inch * 72.0;
    }

    private int pointsInt(double inch) {
        return (int) Math.round(points(inch));
    }

    private record Stat(String value, String label) {}
    private record AccentSet(Color primary, Color secondary, Color tertiary) {}
}
