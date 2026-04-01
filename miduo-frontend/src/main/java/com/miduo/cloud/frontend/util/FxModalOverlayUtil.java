package com.miduo.cloud.frontend.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * 透明样式模态框的遮罩层：铺满 owner 客户区半透明底，避免白底弹窗与浅色主界面融在一起。
 */
public final class FxModalOverlayUtil {

    private static final String OVERLAY_STYLE = "-fx-background-color: rgba(0,0,0,0.45);";

    private FxModalOverlayUtil() {}

    /**
     * FXML / 代码组装的模态框：透明舞台 + 遮罩 + 内容按首选尺寸居中。
     * 请先调用 {@link Stage#initModality}；{@link Stage#initOwner} 建议与 {@code owner} 一致。
     */
    public static void applyOverlayScene(Stage stage, Region dialogRoot, Window owner, Insets dialogMargin) {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(buildModalScene(dialogRoot, owner, dialogMargin));
        stage.setOnShown(e -> sizeStageToOwner(stage, owner));
        sizeStageToOwner(stage, owner);
    }

    /**
     * @param dialogBody  白底卡片根节点
     * @param owner       非 null 时场景与舞台对齐 owner；为 null 时仅包裹内容（无遮罩）
     * @param dialogMargin 卡片四周留白（含阴影）
     */
    public static Scene buildModalScene(Region dialogBody, Window owner, Insets dialogMargin) {
        // StackPane 默认会把子节点拉到铺满；限制为首选宽高，卡片才按内容收缩并居中
        dialogBody.setMaxWidth(Region.USE_PREF_SIZE);
        dialogBody.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(dialogBody, Pos.CENTER);

        StackPane holder = new StackPane(dialogBody);
        holder.setPadding(dialogMargin);
        holder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        holder.setPickOnBounds(false);

        if (owner == null) {
            holder.setStyle("-fx-background-color: transparent;");
            Scene scene = new Scene(holder);
            scene.setFill(Color.TRANSPARENT);
            return scene;
        }

        Region overlay = new Region();
        overlay.setStyle(OVERLAY_STYLE);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        StackPane root = new StackPane(overlay, holder);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        return scene;
    }

    /** 将透明模态舞台对齐并铺满 owner（需在 show 前或 onShown 中调用）。 */
    public static void sizeStageToOwner(Stage stage, Window owner) {
        if (owner == null || stage == null) return;
        stage.setWidth(owner.getWidth());
        stage.setHeight(owner.getHeight());
        stage.setX(owner.getX());
        stage.setY(owner.getY());
    }
}
