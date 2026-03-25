package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * 采集规格变更确认弹窗控制器
 */
public class ShiwanM2SpecChangeDialogController {

    @FXML private HBox      titleBar;
    @FXML private StackPane specWarnIconPane;
    @FXML private Label     savedSpecLabel;
    @FXML private Label     currentSpecLabel;

    private double  dragOffsetX;
    private double  dragOffsetY;
    private boolean confirmed = false;

    @FXML
    private void initialize() {
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        SvgIconLoader.loadInto(specWarnIconPane, SvgIconLoader.ICON_WARN, 18, Color.web("#D97706"));
    }

    /** 设置已保存规格和当前规格的展示文字 */
    public void setSpec(String saved, String current) {
        savedSpecLabel.setText(saved);
        currentSpecLabel.setText(current);
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        ((Stage) titleBar.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
