package com.miduo.cloud.frontend.util;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 帮助/内容弹窗：蓝色标题栏 + 可滚动内容区，无需 FXML。
 * 标题固定，内容区高度上限 380px，超出自动出现竖向滚动条。
 * 支持行内加粗语法：用 {@code **文字**} 标记粗体。
 * 支持列表条目前缀：{@code - } 开头的行自动去掉前缀符号。
 *
 * <pre>
 * // 直接传入 Markdown 风格字符串（与需求文档格式一致）
 * FxHelpDialog.show(window, "任务控制帮助",
 *     "- **开始/停止采集**：点击开始后系统检测硬件设备，检测通过后开始采集工作",
 *     "- **强制满垛**：未达到设定箱数也强制结束当前垛，生成虚拟垛标并重置计数",
 *     "- **提取工单未成垛**：停止采集时可用。弹窗输入或扫描未完成垛中的任一箱码"
 * );
 *
 * // 或 List&lt;String&gt;
 * FxHelpDialog.show(window, "帮助提示", helpItems);
 * </pre>
 */
public class FxHelpDialog {

    private static final double MAX_CONTENT_HEIGHT = 380;
    private static final double DIALOG_WIDTH       = 560;
    private static final String FONT_FAMILY        = "Microsoft YaHei";
    private static final double FONT_SIZE          = 16;
    private static final double LINE_SPACING       = 6;

    // ── 公开入口 ─────────────────────────────────────────────────────────

    public static void show(Window owner, String title, String... items) {
        show(owner, title, Arrays.asList(items));
    }

    public static void show(Window owner, String title, List<String> items) {
        Stage stage = buildStage(owner);
        VBox root = buildRoot(stage, title, items);

        stage.setScene(FxModalOverlayUtil.buildModalScene(root, owner, new Insets(20)));
        stage.setOnShown(e -> FxModalOverlayUtil.sizeStageToOwner(stage, owner));
        FxModalOverlayUtil.sizeStageToOwner(stage, owner);

        stage.showAndWait();
    }

    // ── 构建根节点 ────────────────────────────────────────────────────────

    private static Stage buildStage(Window owner) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setResizable(false);
        return stage;
    }

    private static VBox buildRoot(Stage stage, String title, List<String> items) {
        VBox root = new VBox(buildTitleBar(stage, title), buildScrollContent(items));
        root.setPrefWidth(DIALOG_WIDTH);
        root.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: #D9E1EC;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#000000", 0.12));
        shadow.setRadius(24);
        shadow.setOffsetY(8);
        root.setEffect(shadow);
        return root;
    }

    // ── 标题栏 ────────────────────────────────────────────────────────────

    private static HBox buildTitleBar(Stage stage, String title) {
        Text titleText = new Text(title);
        titleText.setFill(Color.WHITE);
        titleText.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #FFFFFF30;" +
                "-fx-background-radius: 6;" +
                "-fx-min-width: 32; -fx-min-height: 32;" +
                "-fx-max-width: 32; -fx-max-height: 32;" +
                "-fx-border-width: 0; -fx-effect: null; -fx-cursor: hand;" +
                "-fx-padding: 0;"
        );
        closeBtn.setOnAction(e -> stage.close());

        HBox bar = new HBox(titleText, spacer, closeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 24));
        bar.setPrefHeight(56);
        bar.setMinHeight(56);
        bar.setStyle(
                "-fx-background-color: #2563EB;" +
                "-fx-background-radius: 12 12 0 0;"
        );

        return bar;
    }

    // ── 可滚动内容区 ──────────────────────────────────────────────────────

    private static ScrollPane buildScrollContent(List<String> items) {
        VBox contentBox = new VBox(LINE_SPACING * 2);
        contentBox.setPrefWidth(DIALOG_WIDTH - 2);
        contentBox.setPadding(new Insets(20, 24, 20, 24));

        for (String item : items) {
            contentBox.getChildren().add(parseItem(item));
        }

        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(MAX_CONTENT_HEIGHT);
        scroll.setMaxHeight(MAX_CONTENT_HEIGHT);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background-color: white;" +
                "-fx-background: white;" +
                "-fx-border-width: 0;"
        );
        applySmoothScrolling(scroll);
        return scroll;
    }

    /**
     * 为 ScrollPane 添加平滑滚动：拦截原始滚动事件，用缓动动画代替离散跳动。
     */
    private static void applySmoothScrolling(ScrollPane scroll) {
        final double[] target = {0};
        final Timeline[] anim  = {null};

        scroll.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;

            double contentH = scroll.getContent().getBoundsInLocal().getHeight();
            double viewH    = scroll.getViewportBounds().getHeight();
            if (contentH <= viewH) return;

            // 每次滚动移动约 40px，折算为 vvalue 变化量
            double delta = e.getDeltaY() / (contentH - viewH) * -1;
            target[0] = Math.max(0, Math.min(1, scroll.getVvalue() + delta));

            if (anim[0] != null) anim[0].stop();
            anim[0] = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(scroll.vvalueProperty(), scroll.getVvalue())),
                    new KeyFrame(Duration.millis(180),
                            new KeyValue(scroll.vvalueProperty(), target[0], Interpolator.EASE_OUT))
            );
            anim[0].play();
            e.consume();
        });
    }

    /**
     * 解析单条帮助文字，支持：
     * <ul>
     *   <li>行首 {@code - } 或 {@code * } 列表前缀自动去掉</li>
     *   <li>{@code **文字**} 渲染为粗体蓝色</li>
     * </ul>
     */
    private static TextFlow parseItem(String raw) {
        // 去掉行首列表前缀 "- " 或 "* "
        String line = raw.trim();
        if (line.startsWith("- ") || line.startsWith("* ")) {
            line = line.substring(2);
        }

        TextFlow flow = new TextFlow();
        flow.setLineSpacing(LINE_SPACING);
        flow.setMaxWidth(DIALOG_WIDTH - 48); // 左右各 24px padding

        List<Text> segments = parseBold(line);
        flow.getChildren().addAll(segments);
        return flow;
    }

    /**
     * 将字符串按 {@code **...**} 分割，粗体部分用蓝色加粗渲染。
     */
    private static List<Text> parseBold(String line) {
        List<Text> result = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            int boldStart = line.indexOf("**", i);
            if (boldStart == -1) {
                // 剩余全是普通文字
                result.add(normalText(line.substring(i)));
                break;
            }
            // 粗体前的普通文字
            if (boldStart > i) {
                result.add(normalText(line.substring(i, boldStart)));
            }
            int boldEnd = line.indexOf("**", boldStart + 2);
            if (boldEnd == -1) {
                // 没有闭合，当普通文字处理
                result.add(normalText(line.substring(boldStart)));
                break;
            }
            result.add(boldText(line.substring(boldStart + 2, boldEnd)));
            i = boldEnd + 2;
        }
        return result;
    }

    private static Text normalText(String content) {
        Text t = new Text(content);
        t.setFill(Color.web("#374151"));
        t.setFont(Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SIZE));
        return t;
    }

    private static Text boldText(String content) {
        Text t = new Text(content);
        t.setFill(Color.web("#1D4ED8")); // 蓝色，突出关键词
        t.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_SIZE));
        return t;
    }

}
