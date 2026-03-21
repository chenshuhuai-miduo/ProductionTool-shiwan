package com.miduo.cloud.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.codepackage.CodePackageImportVO;
import com.miduo.cloud.entity.dto.codepackage.CodePackageLocalImportRequest;
import com.miduo.cloud.frontend.util.FxToast;
import com.miduo.cloud.frontend.util.HttpUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 码包本地导入弹窗控制器
 */
public class ShiwanM2PackageImportDialogController {

    @FXML private HBox       titleBar;
    @FXML private ComboBox<String> packageTypeCombo;
    @FXML private TextField  selectedFileField;
    @FXML private TextArea   remarkField;
    @FXML private PasswordField passwordField;
    @FXML private Label      tipsLabel;

    private File selectedFile;
    private Runnable onImportSuccess;

    /** 拖拽偏移量 */
    private double dragOffsetX, dragOffsetY;

    @FXML
    public void initialize() {
        packageTypeCombo.getItems().setAll("盖外码小标", "盒外码中标", "箱外码大标");
        packageTypeCombo.setPromptText("请选择");
        tipsLabel.setText("");

        // 备注最多 50 字限制
        remarkField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > 50) {
                remarkField.setText(oldVal);
            }
        });

        // 标题栏拖拽
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        titleBar.setOnMouseDragged(e -> {
            Stage stage = (Stage) titleBar.getScene().getWindow();
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }

    public void setOnImportSuccess(Runnable onImportSuccess) {
        this.onImportSuccess = onImportSuccess;
    }

    @FXML
    private void onBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择码包 TXT 文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT 文件", "*.txt"));
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            selectedFile = file;
            selectedFileField.setText(file.getName());
            tipsLabel.setText("");
        }
    }

    @FXML
    private void onCancel() {
        getStage().close();
    }

    @FXML
    private void onConfirmImport() {
        Integer packageType = resolvePackageType(packageTypeCombo.getValue());
        if (packageType == null) {
            tipsLabel.setText("请选择码包类型");
            return;
        }
        if (selectedFile == null) {
            tipsLabel.setText("请先选择 TXT 文件");
            return;
        }
        if (passwordField.getText() == null || passwordField.getText().trim().isEmpty()) {
            tipsLabel.setText("请输入导入密码");
            return;
        }
        String password = passwordField.getText().trim();

        tipsLabel.setText("正在导入，请稍候...");
        new Thread(() -> {
            try {
                List<String> lines = Files.readAllLines(selectedFile.toPath(), StandardCharsets.UTF_8);
                CodePackageLocalImportRequest request = new CodePackageLocalImportRequest();
                request.setPackageType(packageType);
                request.setPackageName(selectedFile.getName());
                request.setFileName(selectedFile.getName());
                request.setPassword(password);
                String remark = remarkField.getText() != null ? remarkField.getText().trim() : "";
                if (!remark.isEmpty()) request.setRemark(remark);
                request.setCodes(lines);

                String responseJson = HttpUtil.doPost("/api/code-package/import/local", request);
                ApiResult<CodePackageImportVO> result = HttpUtil.parseJson(
                        responseJson, new TypeReference<ApiResult<CodePackageImportVO>>() {});

                Platform.runLater(() -> {
                    if (result != null && result.getCode() == 200) {
                        if (onImportSuccess != null) {
                            onImportSuccess.run();
                        }
                        // 先拿到父窗口引用，再关闭弹窗，最后在父窗口上显示 Toast
                        javafx.stage.Window owner = getStage().getOwner();
                        getStage().close();
                        FxToast.success(owner, "码包导入成功");
                    } else {
                        tipsLabel.setText(result == null ? "导入失败" : result.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> tipsLabel.setText("导入失败：" + e.getMessage()));
            }
        }, "package-local-import").start();
    }

    private Integer resolvePackageType(String text) {
        if ("盖外码小标".equals(text)) {
            return 1;
        }
        if ("盒外码中标".equals(text)) {
            return 2;
        }
        if ("箱外码大标".equals(text)) {
            return 3;
        }
        return null;
    }

    private Stage getStage() {
        return (Stage) packageTypeCombo.getScene().getWindow();
    }

}
