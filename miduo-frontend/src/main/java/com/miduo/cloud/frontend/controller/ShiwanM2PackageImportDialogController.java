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
import java.util.prefs.Preferences;

/**
 * 码包本地导入弹窗控制器
 */
public class ShiwanM2PackageImportDialogController {

    private static final Preferences PACKAGE_IMPORT_PREFS =
            Preferences.userNodeForPackage(ShiwanM2PackageImportDialogController.class);
    private static final String KEY_LAST_IMPORT_DIR = "codePackageImport.lastDirectory";

    @FXML private HBox       titleBar;
    @FXML private ComboBox<String> packageTypeCombo;
    @FXML private TextField  selectedFileField;
    @FXML private TextArea   remarkField;
    @FXML private PasswordField passwordField;
    @FXML private Label      tipsLabel;

    private File selectedFile;
    private Runnable onImportSuccess;

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
    }

    public void setOnImportSuccess(Runnable onImportSuccess) {
        this.onImportSuccess = onImportSuccess;
    }

    @FXML
    private void onBrowseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择码包 TXT 文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT 文件", "*.txt"));
        String lastDirPath = PACKAGE_IMPORT_PREFS.get(KEY_LAST_IMPORT_DIR, null);
        if (lastDirPath != null) {
            File lastDir = new File(lastDirPath);
            if (lastDir.isDirectory()) {
                fileChooser.setInitialDirectory(lastDir);
            }
        }
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            selectedFile = file;
            selectedFileField.setText(file.getName());
            tipsLabel.setText("");
            File parent = file.getParentFile();
            if (parent != null && parent.isDirectory()) {
                PACKAGE_IMPORT_PREFS.put(KEY_LAST_IMPORT_DIR, parent.getAbsolutePath());
            }
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
                CodePackageLocalImportRequest request = new CodePackageLocalImportRequest();
                request.setPackageType(packageType);
                request.setPackageName(selectedFile.getName());
                request.setFileName(selectedFile.getName());
                request.setPassword(password);
                String remark = remarkField.getText() != null ? remarkField.getText().trim() : "";
                if (!remark.isEmpty()) request.setRemark(remark);
                // 传绝对路径由后端读文件，避免十几万行 JSON 序列化/反序列化
                request.setLocalFilePath(selectedFile.getAbsolutePath());

                String responseJson = HttpUtil.doPostLong("/api/code-package/import/local", HttpUtil.getObjectMapper().writeValueAsString(request));
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
