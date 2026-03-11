package com.miduo.cloud.frontend.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 操作帮助弹窗控制器
 * <p>
 * 对应 FXML：ShiwanM2HelpDialog.fxml
 * 需求文档：P03-01-操作帮助.md
 * </p>
 */
public class ShiwanM2HelpController implements Initializable {

    @FXML private Button closeBtn;
    @FXML private VBox tocContainer;
    @FXML private VBox contentContainer;
    @FXML private ScrollPane contentScrollPane;

    /** 已注册的章节 VBox，顺序对应 TOC */
    private final List<VBox> sections = new ArrayList<>();
    /** 当前激活的 TOC 按钮 */
    private Button activeTocBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildContent();
        buildToc();
    }

    // ==================== TOC 构建 ====================

    private void buildToc() {
        String[][] items = {
            {"h-intro",   "一、系统简介",    "false"},
            {"h-m1",      "二、1号机操作",   "false"},
            {"h-m2",      "三、2号机操作",   "false"},
            {"h-funcs",   "四、各功能说明",  "false"},
            {"h-manual",  "↳ 手工采集",      "true"},
            {"h-package", "↳ 码包管理",      "true"},
            {"h-query",   "↳ 数据查询",      "true"},
            {"h-replace", "↳ 数据替换",      "true"},
            {"h-cancel",  "↳ 取消关联",      "true"},
            {"h-stats",   "↳ 生产统计",      "true"},
            {"h-upload",  "↳ 数据上传",      "true"},
            {"h-faq",     "五、常见问题",    "false"},
            {"h-notes",   "六、注意事项",    "false"},
        };

        for (int i = 0; i < items.length; i++) {
            final String id   = items[i][0];
            final String text = items[i][1];
            final boolean sub = Boolean.parseBoolean(items[i][2]);
            final int sectionIdx = sectionIndexOf(id);

            Button btn = new Button(text);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setFocusTraversable(false);
            applyTocStyle(btn, sub, false);

            btn.setOnAction(e -> {
                scrollToSection(sectionIdx);
                if (activeTocBtn != null) applyTocStyle(activeTocBtn, activeTocBtn.getText().startsWith("↳"), false);
                applyTocStyle(btn, sub, true);
                activeTocBtn = btn;
            });

            tocContainer.getChildren().add(btn);

            if (i == 0) {
                applyTocStyle(btn, false, true);
                activeTocBtn = btn;
            }
        }
    }

    private void applyTocStyle(Button btn, boolean sub, boolean active) {
        String base = "-fx-text-fill: " + (active ? "#2563EB" : (sub ? "#6B7280" : "#374151")) + ";"
            + "-fx-font-size: " + (sub ? "13" : "14") + "px;"
            + "-fx-font-family: 'Microsoft YaHei';"
            + "-fx-background-color: " + (active ? "#EFF6FF" : "transparent") + ";"
            + "-fx-border-color: transparent transparent transparent " + (active ? "#2563EB" : "transparent") + ";"
            + "-fx-border-width: 0 0 0 3px;"
            + "-fx-alignment: CENTER_LEFT;"
            + "-fx-padding: " + (sub ? "6px 16px 6px 28px" : "8px 16px") + ";"
            + "-fx-cursor: hand;"
            + (active ? "-fx-font-weight: bold;" : "");
        btn.setStyle(base);
    }

    private int sectionIndexOf(String id) {
        String[] ids = {"h-intro","h-m1","h-m2","h-funcs","h-manual",
                        "h-package","h-query","h-replace","h-cancel","h-stats","h-upload","h-faq","h-notes"};
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].equals(id)) return i;
        }
        return 0;
    }

    private void scrollToSection(int idx) {
        if (idx < 0 || idx >= sections.size()) return;
        VBox target = sections.get(idx);
        contentContainer.applyCss();
        contentContainer.layout();
        double totalH = contentContainer.getBoundsInLocal().getHeight();
        double targetY = target.getBoundsInParent().getMinY();
        if (totalH <= 0) return;
        double vValue = targetY / (totalH - contentScrollPane.getHeight());
        contentScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
    }

    // ==================== 正文内容构建 ====================

    private void buildContent() {
        buildSection1();
        buildSection2();
        buildSection3();
        buildSection4Header();
        buildSection41Manual();
        buildSection42Package();
        buildSection43Query();
        buildSection44Replace();
        buildSection45Cancel();
        buildSection46Stats();
        buildSection47Upload();
        buildSection5Faq();
        buildSection6Notes();
    }

    // ---- 一、系统简介 ----
    private void buildSection1() {
        VBox sec = newSection();
        addH1(sec, "一、系统简介");
        addH2(sec, "1.1 系统组成");
        addPara(sec, "本系统采用四级关联模式（瓶-盒-箱-垛），由两个独立软件组成：");
        addBullets(sec, new String[]{
            "1号机（瓶盒关联软件）：负责瓶码和盒码的采集关联，数据实时同步至2号机",
            "2号机（盒箱垛关联软件）：负责盒箱垛关联采集、数据管理和数据上传"
        });
        addH2(sec, "1.2 包装规格");
        addSmallNote(sec, "默认值，可在系统设置中配置，勿随意修改");
        addTable(sec,
            new String[]{"层级", "关系"},
            new String[][]{
                {"1盒", "= 6瓶"},
                {"1箱", "= 4盒（24瓶）"},
                {"1垛", "= 70箱（280盒，1680瓶）"}
            });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 二、1号机操作指南 ----
    private void buildSection2() {
        VBox sec = newSection();
        addH1(sec, "二、1号机操作指南（瓶盒关联软件）");
        addH2(sec, "2.1 日常操作流程");

        addStepTitle(sec, "步骤1：确认采集规格");
        addBullets(sec, new String[]{
            "在左侧「采集规格设置」区域，确认「1盒 6瓶」设置正确（6可修改，默认6瓶）",
        });
        addWarnLine(sec, "⚠ 开始采集后不可修改");

        addStepTitle(sec, "步骤2：开始采集");
        addOrdered(sec, new String[]{
            "点击「开始采集」按钮",
            "系统自动检测设备状态（瓶1读码器、瓶2读码器、盒读码器）",
            "检测通过后开始采集；按钮变为「停止采集」"
        });

        addStepTitle(sec, "步骤3：监控采集过程");
        addBullets(sec, new String[]{
            "右侧 - 数据接收区：盒码、瓶码及关联结果统一展示，绿色=关联成功，红色=关联失败或异常",
            "左侧 - 单位实时统计：「合计瓶数」「当前盒数」实时更新（文字在上、数字在下）",
            "黑色码=正常，红色码=重码/错码（系统自动标记剔除，无需手动干预）"
        });

        addStepTitle(sec, "步骤4：停止采集");
        addBullets(sec, new String[]{"点击「停止采集」按钮，未满盒的数据会保留"});

        addH2(sec, "2.2 设备连接状态");
        addBullets(sec, new String[]{
            "右侧操作日志区域显示读码器连接状态",
            "绿色文字：设备已连接 ✓",
            "红色文字：设备未连接，请检查设备连接后重启"
        });

        addH2(sec, "2.3 关闭报警");
        addBullets(sec, new String[]{
            "长报警时（设备未连接、连续重码3次等），点击「关闭报警」可重置报警状态",
        });
        addWarnLine(sec, "⚠ 短报警（重码/错码/超码/缺码等单次异常）自动关闭，不需人工操作");

        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 三、2号机操作指南 ----
    private void buildSection3() {
        VBox sec = newSection();
        addH1(sec, "三、2号机操作指南（盒箱垛关联软件）");
        addH2(sec, "3.1 功能入口");
        addTable(sec,
            new String[]{"Tab 名称", "功能说明"},
            new String[][]{
                {"数据采集", "主界面，默认选中"},
                {"手工采集", "1号机相机损坏时备用"},
                {"码包管理", "导入码包数据"},
                {"数据查询", "查询码关联关系"},
                {"数据替换", "码替换操作"},
                {"取消关联", "单个或批量解除关联"},
                {"生产统计", "查看生产数据统计"},
                {"数据上传", "管理数据上传状态"},
            });
        addPara(sec, "系统设置通过菜单「配置」→「系统设置」进入（弹窗形式，需要密码）。");

        addH2(sec, "3.2 数据采集主界面操作流程");
        addStepTitle(sec, "生产前准备：");
        addOrdered(sec, new String[]{
            "在左侧「当前生产信息」区域，点击「产品选择」下拉框，搜索并选择生产产品",
            "确认生产单号（系统自动生成，可手动修改）",
            "确认采集规格（1垛 [70]箱、1箱 [4]盒，70、4可修改）；采集开始后不可修改"
        });
        addStepTitle(sec, "开始采集：");
        addOrdered(sec, new String[]{
            "点击右侧「开始采集」按钮",
            "系统检测设备状态，检测通过后弹出二次确认弹窗",
            "确认后开始采集"
        });
        addStepTitle(sec, "监控采集过程：");
        addBullets(sec, new String[]{
            "中间上方 - 数据接收区（约60%高）：扫码数据和关联结果，最新在上",
            "中间中部 - 操作日志（约20%）：用户操作记录，最新在上",
            "中间下方 - 报警信息（约20%）：重码、关联失败等异常，最新在上",
            "右侧 - 单位实时统计：当前箱数、每垛箱数、当前盒数、每箱盒数、已生产垛数、总剔除数"
        });
        addStepTitle(sec, "成垛与自动上传：");
        addBullets(sec, new String[]{
            "箱数达到预设值时自动成垛，成垛后自动上传数据至云端",
            "上传成功：绿灯常亮，无蜂鸣/提示音；一分钟后自动熄灭",
        });
        addWarnLine(sec, "上传失败：红灯常亮 + 声音提醒（需处理后手动重传）");

        addH2(sec, "3.3 右侧任务控制");
        addTable(sec,
            new String[]{"按钮", "功能说明"},
            new String[][]{
                {"开始/停止采集", "启动或停止数据采集；停止后未满垛数据保留"},
                {"无需采集码", "当天不生产五码合一产品时开启，所有读码设备不工作"},
                {"关闭报警", "上传失败触发报警后，点击可重置报警状态（停止声光报警器、蜂鸣器）"},
                {"强制满垛", "未达到预设箱数时，强制结束当前垛，生成垛码"},
                {"提取工单未成垛", "弹窗输入/扫描垛内任一箱码，查出对应生产订单并显示，确认后回显主页面继续生产"},
                {"收回剔除", "剔除设备动作后未自动收回时，点击手动收回"},
                {"剔除数清零", "将总剔除数重置为 0"},
            });

        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 四、各功能操作说明（标题章节，无内容） ----
    private void buildSection4Header() {
        VBox sec = newSection();
        addH1(sec, "四、各功能操作说明");
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.1 手工采集 ----
    private void buildSection41Manual() {
        VBox sec = newSection();
        addH2(sec, "4.1 手工采集（1号机相机损坏时使用，仅支持瓶盒关联）");
        addOrdered(sec, new String[]{
            "点击「手工采集」Tab",
            "设置采集规格（1盒 [6]瓶，6可修改）；采集开始后不可修改",
            "点击「开始采集」按钮，按钮变为「停止采集」",
            "按左侧「开始采集提示」顺序用扫码枪扫码：先扫瓶码（足够数量后）→ 扫盒码",
            "左侧开始采集提示区文字随进度变化，告知当前应扫瓶码或盒码",
            "扫码完成后自动完成瓶盒关联，当前已读数量归零，继续下一组"
        });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.2 码包管理 ----
    private void buildSection42Package() {
        VBox sec = newSection();
        addH2(sec, "4.2 码包管理");
        addPara(sec, "用途：导入包材厂提供的码包文件，用于采集时比对校验（不在码包内的码会被剔除）。");
        addTable(sec,
            new String[]{"操作", "步骤"},
            new String[][]{
                {"启动时在线导入", "软件启动时自动执行，仅拉取盖外码小标、箱外码大标"},
                {"在线更新", "点击「在线更新」→ 仅拉取瓶码（盖外码小标）、箱码（箱外码大标）"},
                {"本地导入", "点击「本地导入」→ 选码包类型 → 选TXT文件 → 输入密码123456 → 确认解析入库"},
                {"查看码包", "点击操作列「查看」→ 弹窗显示该码包所有码，支持搜索与分页"},
                {"删除码包", "仅当该码包内无已关联码时允许删除；有则禁止并提示。删除为逻辑删除"},
            });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.3 数据查询 ----
    private void buildSection43Query() {
        VBox sec = newSection();
        addH2(sec, "4.3 数据查询");
        addPara(sec, "用途：查询码的关联关系，用于产品追溯。");
        addPara(sec, "输入码后点击「查询」或回车，查出该码所属垛的全部数据。左侧表格按层级分列展示，与输入的码对应的单元格红色字体显示；点击某行，右侧展示该行的具体信息（码、采集时间、产品编号、产品名称、生产单号）。");
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.4 数据替换 ----
    private void buildSection44Replace() {
        VBox sec = newSection();
        addH2WithBadge(sec, "4.4 数据替换", "需要密码 123456");
        addPara(sec, "用途：码损坏或码错误时，将旧码替换为新码。");
        addOrdered(sec, new String[]{
            "输入原码（待替换）",
            "输入新码（替换后）",
            "输入替换原因（可选）",
            "点击「确认替换」→ 弹窗核对信息 → 输入密码 123456 → 确认执行"
        });
        addPara(sec, "新码要求（需同时满足）：");
        addBullets(sec, new String[]{
            "✅ 在导入的码包范围内",
            "✅ 未被使用过（系统中无关联关系）",
            "✅ 格式有效，且不能与原码相同"
        });
        addWarnLine(sec, "⚠ 替换操作不可恢复，请务必核对无误再执行");
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.5 取消关联 ----
    private void buildSection45Cancel() {
        VBox sec = newSection();
        addH2WithBadge(sec, "4.5 取消关联", "需要密码 123456");
        addPara(sec, "用途：解除码的关联关系（如数据错误、产品需要重新处理时）。");
        addInfoBox(sec, "重要规则：\n"
            + "• 必须从上级起逐级取消（垛 → 箱 → 盒 → 瓶），不可跳过上级直接取消下级\n"
            + "• 系统自动检查上级关联状态，有上级时「确认」按钮禁用，提示先取消上级\n"
            + "• 系统自动检查云端数据并先取消云端关联，确保数据一致");
        addStepTitle(sec, "单码取消操作步骤：");
        addOrdered(sec, new String[]{
            "选择「单码取消」模式",
            "输入或扫描1个码（瓶/盒，不支持箱码/垛码）",
            "点击「识别」→ 右侧显示识别结果（上级单元、取消范围、上级关联链路）",
            "如有上级关联，先从垛开始逐级取消上级，再回来取消当前级",
            "无上级关联后，点击「确认取消关联」→ 输入密码 123456 → 确认执行"
        });
        addStepTitle(sec, "多码取消操作步骤：");
        addOrdered(sec, new String[]{
            "选择「多码取消」模式",
            "逐一输入盒/箱/垛码 → 点击「添加」",
            "点击「识别」→ 右侧显示所有包装单元的识别结果",
            "处理有上级关联的单元后，点击「确认取消关联」→ 输入密码 123456 → 执行"
        });
        addWarnLine(sec, "⚠ 操作不可恢复，请确认无误后操作");
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.6 生产统计 ----
    private void buildSection46Stats() {
        VBox sec = newSection();
        addH2(sec, "4.6 生产统计");
        addPara(sec, "用途：查看生产数据统计和各垛上传状态。");
        addTable(sec,
            new String[]{"操作", "步骤"},
            new String[][]{
                {"生产统计查询", "左侧选择日期（开始-结束）、可输入生产单号 → 点击「查询」→ 显示垛数/箱数/盒数"},
                {"查看垛码列表", "点击蓝色下划线的「垛数」数字 → 弹窗显示垛码列表，支持模糊筛选"},
                {"上传统计查询", "右侧选择日期、生产单号、状态（全部/成功/异常）→ 点击「查询」"},
            });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 4.7 数据上传 ----
    private void buildSection47Upload() {
        VBox sec = newSection();
        addH2(sec, "4.7 数据上传");
        addPara(sec, "用途：管理上传任务和状态，处理上传异常。");
        addTable(sec,
            new String[]{"功能", "操作"},
            new String[][]{
                {"手动上传", "点击「手动上传」→ 批量上传所有未上传的垛"},
                {"查询状态", "输入垛码 → 点击「查询状态」→ 查看上传详情"},
                {"设为未上传", "输入垛码 → 点击「设为未上传」→ 确认（用于异常修复后重新补传）"},
                {"设为已上传", "输入垛码 → 点击「设为已上传」→ 确认（避免系统重复上传）"},
            });
        addPara(sec, "左侧「上传信息展示区」实时显示自动上传日志：");
        addBullets(sec, new String[]{
            "灰色：开始上传",
            "蓝色：上传中",
            "绿色：上传成功",
            "红色：上传失败（含失败原因）",
        });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 五、常见问题 ----
    private void buildSection5Faq() {
        VBox sec = newSection();
        addH1(sec, "五、常见问题 & 异常处理");
        addFaq(sec, "Q1：1号机显示红色码，怎么处理？",
            "红色码 = 重码（已存在）或错码（格式错误）。系统自动标记待剔除，产线正常移动，下游龙门架按箱剔除，无需手动干预。");
        addFaqOrdered(sec, "Q2：设备显示未连接怎么办？", new String[]{
            "检查读码器/相机的电源和网线是否插好",
            "重启设备后，查看连接状态是否恢复绿色",
            "若仍未连接，联系技术人员"
        });
        addFaqOrdered(sec, "Q3：数据上传失败，红灯亮了怎么办？", new String[]{
            "查看「数据上传」Tab 中的失败原因",
            "根据失败原因进行数据处理或等待网络恢复",
            "点击「关闭报警」重置报警灯和蜂鸣器",
            "处理完成后，点击「手动上传」重新上传"
        });
        addFaq(sec, "Q4：生产结束时还有尾数（未满垛），怎么处理？",
            "方式一（推荐）：点击「强制满垛」，将当前数据强制成垛并上传\n"
            + "方式二：直接停止采集，不做处理。下次生产时点击「提取工单未成垛」，选择该条数据继续生产");
        addFaq(sec, "Q4-1：退出软件时有未满垛数据，弹窗怎么选？",
            "暂存：将未满垛数据存库标记未成垛，允许退出；下次可「提取工单未成垛」继续生产\n"
            + "强制满垛：执行强制满垛流程，生成虚拟垛标、重置计数，操作完毕后允许退出\n"
            + "取消：返回主界面，不退出");
        addFaq(sec, "Q5：剔除装置剔出去后没有自动收回，怎么办？",
            "点击右侧「收回剔除」按钮，手动触发收回指令。");
        addFaq(sec, "Q6：忘记密码怎么办？",
            "系统设置、码替换、取消关联的密码统一为：123456");
        addFaq(sec, "Q7：如何知道码属于哪一层级（瓶/盒/箱/垛）？",
            "在「数据查询」Tab 中输入码值，点击「查询」或回车，左侧表格按层级分列展示，点击某行右侧可查看该行的详细信息。");
        addFaqOrdered(sec, "Q8：取消关联时提示「请先取消上级」，怎么操作？", new String[]{
            "在「识别结果」中查看完整链路（如：垛码xxx → 箱码xxx → 盒码xxx）",
            "先输入最顶层的垛码，执行取消关联",
            "再输入箱码，执行取消关联",
            "最后输入盒码，执行取消关联",
            "每次取消都需要输入密码 123456"
        });
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ---- 六、注意事项 ----
    private void buildSection6Notes() {
        VBox sec = newSection();
        addH1(sec, "六、注意事项");
        addInfoBox(sec,
            "1. 密码保护：系统设置、码替换、取消关联均需要密码 123456，请妥善保管\n"
            + "2. 不可恢复操作：码替换、取消关联执行后无法撤销，操作前请仔细核对\n"
            + "3. 包装比例：不可随意修改，如需变更请联系管理员\n"
            + "4. 生产前检查：每次开始生产前，确认码包已导入、设备连接正常\n"
            + "5. 异常优先处理：发现报警信息（中间下方红色区域）应及时处理，避免影响数据准确性");
        sections.add(sec);
        contentContainer.getChildren().add(sec);
    }

    // ==================== 内容构建辅助方法 ====================

    /** 创建新的章节容器 */
    private VBox newSection() {
        VBox sec = new VBox(8);
        sec.setStyle("-fx-padding: 28px 32px 24px 32px;"
            + "-fx-border-color: transparent transparent #E5E7EB transparent;"
            + "-fx-border-width: 0 0 1px 0;");
        sec.setMaxWidth(Double.MAX_VALUE);
        return sec;
    }

    /** 一级标题（大蓝色下划线） */
    private void addH1(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1F2937;"
            + "-fx-font-family: 'Microsoft YaHei';"
            + "-fx-border-color: transparent transparent #2563EB transparent;"
            + "-fx-border-width: 0 0 2px 0;"
            + "-fx-padding: 0 0 8px 0;"
            + "-fx-margin: 0 0 8px 0;");
        sec.getChildren().add(lbl);
    }

    /** 二级标题 */
    private void addH2(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937;"
            + "-fx-font-family: 'Microsoft YaHei';"
            + "-fx-padding: 8px 0 4px 0;");
        sec.getChildren().add(lbl);
    }

    /** 带徽标的二级标题 */
    private void addH2WithBadge(VBox sec, String title, String badge) {
        HBox hb = new HBox(10);
        hb.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8px 0 4px 0;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-font-family: 'Microsoft YaHei';");
        Label b = new Label(badge);
        b.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;"
            + "-fx-background-radius: 20px; -fx-border-color: #FCD34D; -fx-border-width: 1px; -fx-border-radius: 20px;"
            + "-fx-padding: 2px 10px; -fx-font-size: 13px; -fx-font-weight: bold;");
        hb.getChildren().addAll(t, b);
        sec.getChildren().add(hb);
    }

    /** 步骤小标题 */
    private void addStepTitle(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #374151;"
            + "-fx-font-family: 'Microsoft YaHei'; -fx-padding: 6px 0 4px 0;");
        sec.getChildren().add(lbl);
    }

    /** 正文段落 */
    private void addPara(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 15px; -fx-text-fill: #374151; -fx-font-family: 'Microsoft YaHei';"
            + "-fx-line-spacing: 4px;");
        sec.getChildren().add(lbl);
    }

    /** 小注解 */
    private void addSmallNote(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B7280; -fx-font-family: 'Microsoft YaHei';");
        sec.getChildren().add(lbl);
    }

    /** 无序列表 */
    private void addBullets(VBox sec, String[] items) {
        for (String item : items) {
            Label lbl = new Label("• " + item);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-padding: 0 0 0 16px; -fx-line-spacing: 3px;");
            sec.getChildren().add(lbl);
        }
    }

    /** 有序列表 */
    private void addOrdered(VBox sec, String[] items) {
        for (int i = 0; i < items.length; i++) {
            Label lbl = new Label((i + 1) + ". " + items[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-padding: 1px 0 1px 16px; -fx-line-spacing: 3px;");
            sec.getChildren().add(lbl);
        }
    }

    /** 红色警告行 */
    private void addWarnLine(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DC2626;"
            + "-fx-font-family: 'Microsoft YaHei'; -fx-padding: 2px 0;");
        sec.getChildren().add(lbl);
    }

    /** 橙色信息框 */
    private void addInfoBox(VBox sec, String text) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: #FFF7ED; -fx-border-color: #FCD34D; -fx-border-width: 1px;"
            + "-fx-background-radius: 8px; -fx-border-radius: 8px;"
            + "-fx-padding: 14px 16px; -fx-font-size: 14px; -fx-text-fill: #92400E;"
            + "-fx-font-family: 'Microsoft YaHei'; -fx-line-spacing: 5px;");
        VBox.setMargin(lbl, new Insets(4, 0, 4, 0));
        sec.getChildren().add(lbl);
    }

    /** FAQ 条目（文本答案） */
    private void addFaq(VBox sec, String question, String answer) {
        VBox box = new VBox(8);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 1px;"
            + "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 14px 16px;");
        VBox.setMargin(box, new Insets(4, 0, 4, 0));

        Label q = new Label(question);
        q.setMaxWidth(Double.MAX_VALUE);
        q.setWrapText(true);
        q.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1D4ED8;"
            + "-fx-font-family: 'Microsoft YaHei';");

        Label a = new Label(answer);
        a.setMaxWidth(Double.MAX_VALUE);
        a.setWrapText(true);
        a.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-family: 'Microsoft YaHei';"
            + "-fx-line-spacing: 4px;");

        box.getChildren().addAll(q, a);
        sec.getChildren().add(box);
    }

    /** FAQ 条目（有序列表答案） */
    private void addFaqOrdered(VBox sec, String question, String[] steps) {
        VBox box = new VBox(6);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: #F9FAFB; -fx-border-color: #E5E7EB; -fx-border-width: 1px;"
            + "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-padding: 14px 16px;");
        VBox.setMargin(box, new Insets(4, 0, 4, 0));

        Label q = new Label(question);
        q.setMaxWidth(Double.MAX_VALUE);
        q.setWrapText(true);
        q.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1D4ED8;"
            + "-fx-font-family: 'Microsoft YaHei';");
        box.getChildren().add(q);

        for (int i = 0; i < steps.length; i++) {
            Label step = new Label((i + 1) + ". " + steps[i]);
            step.setMaxWidth(Double.MAX_VALUE);
            step.setWrapText(true);
            step.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-padding: 0 0 0 8px; -fx-line-spacing: 3px;");
            box.getChildren().add(step);
        }

        sec.getChildren().add(box);
    }

    /** 简单表格（GridPane 实现） */
    private void addTable(VBox sec, String[] headers, String[][] rows) {
        GridPane grid = new GridPane();
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setStyle("-fx-border-color: #E5E7EB; -fx-border-width: 1px; -fx-background-color: white;");
        VBox.setMargin(grid, new Insets(6, 0, 6, 0));

        // 表头
        for (int c = 0; c < headers.length; c++) {
            Label h = new Label(headers[c]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setPadding(new Insets(8, 12, 8, 12));
            h.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;"
                + "-fx-background-color: #F3F4F6; -fx-font-family: 'Microsoft YaHei';"
                + "-fx-border-color: #E5E7EB; -fx-border-width: 0 " + (c < headers.length - 1 ? "1" : "0") + "px 1px 0;");
            GridPane.setHgrow(h, Priority.ALWAYS);
            grid.add(h, c, 0);
        }

        // 数据行
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                Label cell = new Label(rows[r][c]);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setWrapText(true);
                cell.setPadding(new Insets(8, 12, 8, 12));
                String bg = (r % 2 == 0) ? "white" : "#FAFAFA";
                cell.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-background-color: " + bg + ";"
                    + "-fx-font-family: 'Microsoft YaHei';"
                    + "-fx-border-color: #E5E7EB; -fx-border-width: 0 " + (c < rows[r].length - 1 ? "1" : "0") + "px 1px 0;");
                GridPane.setHgrow(cell, Priority.ALWAYS);
                grid.add(cell, c, r + 1);
            }
        }

        // 设置列宽比例
        for (int c = 0; c < headers.length; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(100.0 / headers.length);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        sec.getChildren().add(grid);
    }

    // ==================== 关闭事件 ====================

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }
}
