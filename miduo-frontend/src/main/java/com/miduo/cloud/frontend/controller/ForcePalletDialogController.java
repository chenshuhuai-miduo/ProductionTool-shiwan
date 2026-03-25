package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * 强制满垛对话框控制器
 */
public class ForcePalletDialogController {
    
    @FXML
    private Label mainPromptLabel;
    
    @FXML
    private Label detailLabel;
    
    @FXML
    private HBox supplementRow;

    @FXML
    private StackPane mainWarnIconPane;

    @FXML
    private StackPane supplementWarnIconPane;

    @FXML
    private Label supplementLabel;
    
    @FXML
    private Label orderNoLabel;
    
    @FXML
    private Label currentCountLabel;
    
    @FXML
    private Label boxesPerPalletLabel;
    
    @FXML
    private Label virtualBoxesLabel;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button confirmButton;
    
    // 数据字段
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        if (mainWarnIconPane != null) {
            SvgIconLoader.loadInto(mainWarnIconPane, SvgIconLoader.ICON_WARN, 24, Color.web("#fa8c16"));
        }
        if (supplementWarnIconPane != null) {
            SvgIconLoader.loadInto(supplementWarnIconPane, SvgIconLoader.ICON_WARN, 20, Color.web("#ff9800"));
        }
    }
    
    /**
     * 设置当前垛信息
     */
    public void setPalletInfo(String orderNo, Integer currentCount, Integer boxesPerPallet) {
        // 更新基本信息
        orderNoLabel.setText(orderNo);
        currentCountLabel.setText(String.valueOf(currentCount));
        boxesPerPalletLabel.setText(String.valueOf(boxesPerPallet));
        
        // 计算箱数差异（不再生成虚拟箱码）
        int boxDifference = 0;
        if (currentCount < boxesPerPallet) {
            boxDifference = boxesPerPallet - currentCount;
        } else if (currentCount > boxesPerPallet) {
            boxDifference = currentCount - boxesPerPallet;
        }
        virtualBoxesLabel.setText(String.valueOf(boxDifference));
        
        // 更新主提示
        mainPromptLabel.setText("确定要强制满垛吗？当前垛有 " + currentCount + " 箱，将立即标记为可关联。");
        
        // 更新详细说明（新业务逻辑：不再生成虚拟箱码）
        if (currentCount < boxesPerPallet) {
            detailLabel.setText("此操作不会生成虚拟箱码。点击后将允许当前垛（" + currentCount + " 箱）进行托盘码关联，忽略与规格箱数（" + boxesPerPallet + " 箱）的差异。");
        } else if (currentCount > boxesPerPallet) {
            detailLabel.setText("此操作不会生成虚拟箱码。点击后将允许当前垛（" + currentCount + " 箱）进行托盘码关联，忽略与规格箱数（" + boxesPerPallet + " 箱）的差异。");
        } else {
            detailLabel.setText("此操作不会生成虚拟箱码。点击后将允许当前垛进行托盘码关联。");
        }
        
        // 更新补充说明
        supplementLabel.setText("确认后需通过设备扫码完成托盘码关联，系统将不再校验箱数是否匹配。");
        supplementLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        if (supplementRow != null) {
            supplementRow.setVisible(true);
            supplementRow.setManaged(true);
        }
    }
    
    /**
     * 取消按钮点击
     */
    @FXML
    private void onCancel() {
        confirmed = false;
        closeDialog();
    }
    
    /**
     * 确认按钮点击
     */
    @FXML
    private void onConfirm() {
        confirmed = true;
        closeDialog();
    }
    
    /**
     * 关闭对话框
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * 是否确认
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}

