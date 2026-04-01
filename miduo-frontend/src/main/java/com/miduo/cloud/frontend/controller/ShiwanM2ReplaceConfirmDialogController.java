package com.miduo.cloud.frontend.controller;

import com.miduo.cloud.frontend.util.SvgIconLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ShiwanM2ReplaceConfirmDialogController {

    @FXML private HBox  titleBar;
    @FXML private Label origCodeValue;
    @FXML private Label newCodeValue;
    @FXML private HBox  reasonRow;
    @FXML private Label reasonValue;
    @FXML private StackPane headerWarnIconPane;
    @FXML private StackPane footerWarnIconPane;

    private boolean confirmed = false;

    @FXML
    private void initialize() {
        SvgIconLoader.loadInto(headerWarnIconPane, SvgIconLoader.ICON_WARN, 20, Color.web("#DC2626"));
        SvgIconLoader.loadInto(footerWarnIconPane, SvgIconLoader.ICON_WARN, 18, Color.web("#DC2626"));
    }

    /** 由外部调用初始化替换信息 */
    public void setReplaceInfo(String orig, String newCode, String reason) {
        origCodeValue.setText(orig);
        newCodeValue.setText(newCode);
        if (reason == null || reason.isEmpty()) {
            reasonRow.setVisible(false);
            reasonRow.setManaged(false);
        } else {
            reasonValue.setText(reason);
        }
    }

    @FXML
    private void onCancel() {
        confirmed = false;
        closeStage();
    }

    @FXML
    private void onConfirm() {
        confirmed = true;
        closeStage();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void closeStage() {
        Stage stage = (Stage) titleBar.getScene().getWindow();
        stage.close();
    }
}
