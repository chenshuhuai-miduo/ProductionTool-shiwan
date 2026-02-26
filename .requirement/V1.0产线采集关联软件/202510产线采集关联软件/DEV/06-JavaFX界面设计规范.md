# JavaFX界面设计规范

## 概述

本文档定义了产线采集关联软件的JavaFX界面设计规范，包括色彩方案、字体系统、组件样式、布局规范等，确保界面的一致性和用户体验。

## 🎨 主题色彩方案

### 基础色彩定义

```css
/* 主题色彩 - ThemeColors.css */

/* 主色调 */
.root {
    -primary-color: #4a90e2;
    -primary-dark: #357abd;
    -primary-light: #6ba6ff;
    
    /* 辅助色 */
    -secondary-color: #f5f5f5;
    -accent-color: #ff6b6b;
    
    /* 状态色 */
    -success-color: #28a745;
    -warning-color: #ffc107;
    -error-color: #dc3545;
    -info-color: #17a2b8;
    
    /* 中性色 */
    -text-primary: #333333;
    -text-secondary: #666666;
    -text-muted: #999999;
    -border-color: #e0e0e0;
    -background-color: #ffffff;
    -surface-color: #f8f9fa;
}

/* 应用背景 */
.application-background {
    -fx-background-color: -background-color;
}

/* 主要按钮 */
.primary-button {
    -fx-background-color: -primary-color;
    -fx-text-fill: white;
}
```

## 📝 字体系统

### 字体定义

```css
/* 字体系统 - Typography.css */

.root {
    /* 字体族 */
    -primary-font: "Microsoft YaHei";
    -monospace-font: "Courier New";
    
    /* 字体大小 */
    -font-size-small: 12px;
    -font-size-normal: 14px;
    -font-size-large: 16px;
    -font-size-xlarge: 18px;
    -font-size-xxlarge: 20px;
    
    /* 字体重量 */
    -font-weight-normal: 400;
    -font-weight-medium: 500;
    -font-weight-bold: 700;
}

/* 标题样式 */
.heading-1 {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-xxlarge;
    -fx-font-weight: -font-weight-bold;
    -fx-text-fill: -text-primary;
}

.heading-2 {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-xlarge;
    -fx-font-weight: -font-weight-medium;
    -fx-text-fill: -text-primary;
}

/* 正文样式 */
.body-text {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-font-weight: -font-weight-normal;
    -fx-text-fill: -text-primary;
}

/* 说明文字 */
.caption-text {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-small;
    -fx-font-weight: -font-weight-normal;
    -fx-text-fill: -text-secondary;
}

/* 等宽字体 */
.monospace-text {
    -fx-font-family: -monospace-font;
    -fx-font-size: -font-size-normal;
    -fx-text-fill: -text-primary;
}
```

## 🔘 按钮组件

### 按钮样式

```css
/* 按钮样式 - ButtonStyles.css */

/* 主要按钮 */
.primary-button {
    -fx-background-color: -primary-color;
    -fx-text-fill: white;
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-font-weight: -font-weight-medium;
    -fx-padding: 8 16;
    -fx-min-height: 36px;
    -fx-min-width: 80px;
    -fx-border-width: 0;
    -fx-cursor: hand;
    -fx-background-radius: 4px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);
}

.primary-button:hover {
    -fx-background-color: -primary-dark;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 3, 0, 0, 2);
}

.primary-button:pressed {
    -fx-background-color: derive(-primary-color, -20%);
}

/* 次要按钮 */
.secondary-button {
    -fx-background-color: transparent;
    -fx-text-fill: -primary-color;
    -fx-border-color: -primary-color;
    -fx-border-width: 1px;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-padding: 8 16;
    -fx-min-height: 36px;
    -fx-min-width: 80px;
    -fx-cursor: hand;
}

.secondary-button:hover {
    -fx-background-color: derive(-primary-color, 90%);
}

/* 危险按钮 */
.danger-button {
    -fx-background-color: -error-color;
    -fx-text-fill: white;
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-padding: 8 16;
    -fx-min-height: 36px;
    -fx-min-width: 80px;
    -fx-border-width: 0;
    -fx-background-radius: 4px;
    -fx-cursor: hand;
}

.danger-button:hover {
    -fx-background-color: derive(-error-color, -10%);
}
```

## 📝 输入控件

### 输入框样式

```css
/* 输入控件样式 - InputStyles.css */

/* 文本输入框 */
.text-field {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-text-fill: -text-primary;
    -fx-background-color: white;
    -fx-border-color: -border-color;
    -fx-border-width: 1px;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
    -fx-padding: 8 12;
    -fx-min-height: 36px;
}

.text-field:focused {
    -fx-border-color: -primary-color;
    -fx-effect: dropshadow(gaussian, rgba(74,144,226,0.3), 3, 0, 0, 0);
}

/* 下拉选择框 */
.combo-box {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-background-color: white;
    -fx-border-color: -border-color;
    -fx-border-width: 1px;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
    -fx-min-height: 36px;
}

.combo-box:focused {
    -fx-border-color: -primary-color;
}

/* 多行文本框 */
.text-area {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-text-fill: -text-primary;
    -fx-background-color: white;
    -fx-border-color: -border-color;
    -fx-border-width: 1px;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
    -fx-padding: 8 12;
}

.text-area:focused {
    -fx-border-color: -primary-color;
}
```

## 📊 表格组件

### 表格样式

```css
/* 表格样式 - TableStyles.css */

/* 表格视图 */
.table-view {
    -fx-background-color: white;
    -fx-border-color: -border-color;
    -fx-border-width: 1px;
    -fx-border-radius: 4px;
    -fx-background-radius: 4px;
}

/* 表格列标题 */
.table-view .column-header {
    -fx-background-color: -surface-color;
    -fx-text-fill: -text-primary;
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-font-weight: -font-weight-medium;
    -fx-padding: 8 12;
    -fx-border-color: -border-color;
    -fx-border-width: 0 0 1px 0;
}

/* 表格行 */
.table-view .table-row-cell {
    -fx-background-color: white;
    -fx-text-fill: -text-primary;
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-normal;
    -fx-padding: 4 0;
}

.table-view .table-row-cell:odd {
    -fx-background-color: derive(-surface-color, 50%);
}

.table-view .table-row-cell:selected {
    -fx-background-color: derive(-primary-color, 80%);
}

/* 表格单元格 */
.table-view .table-cell {
    -fx-padding: 8 12;
    -fx-border-color: -border-color;
    -fx-border-width: 0 1px 0 0;
}
```

## 🏗️ 布局规范

### 主界面布局

```xml
<!-- 主界面布局 MainWindow.fxml -->
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns="http://javafx.com/javafx/11.0.1" xmlns:fx="http://javafx.com/fxml/1" 
            fx:controller="com.company.productionline.ui.controller.MainController"
            styleClass="application-background">
   
   <!-- 顶部菜单栏 -->
   <top>
      <MenuBar>
         <Menu text="文件">
            <MenuItem text="新建任务" onAction="#onNewTask"/>
            <MenuItem text="导入数据" onAction="#onImportData"/>
            <SeparatorMenuItem/>
            <MenuItem text="退出" onAction="#onExit"/>
         </Menu>
         <Menu text="配置">
            <MenuItem text="系统设置" onAction="#onSystemConfig"/>
            <MenuItem text="设备配置" onAction="#onDeviceConfig"/>
         </Menu>
         <Menu text="帮助">
            <MenuItem text="关于" onAction="#onAbout"/>
         </Menu>
      </MenuBar>
   </top>
            
            <!-- 工具栏 -->
   <center>
      <VBox>
         <ToolBar>
            <Button text="产品管理" styleClass="primary-button" onAction="#onProductManagement"/>
            <Button text="任务管理" styleClass="primary-button" onAction="#onTaskManagement"/>
            <Button text="数据上传" styleClass="primary-button" onAction="#onDataUpload"/>
            <Button text="查询码" styleClass="secondary-button" onAction="#onCodeQuery"/>
            <Button text="码替换" styleClass="secondary-button" onAction="#onCodeReplace"/>
            <Button text="清屏" styleClass="secondary-button" onAction="#onClearScreen"/>
         </ToolBar>
         
         <!-- 主内容区 -->
         <HBox VBox.vgrow="ALWAYS">
            <!-- 左侧面板 -->
            <VBox prefWidth="340" styleClass="panel">
               <Label text="当前生产信息" styleClass="heading-2"/>
               <!-- 生产信息内容 -->
            </VBox>
            
            <!-- 中间面板 -->
            <VBox HBox.hgrow="ALWAYS" styleClass="panel">
               <Label text="数据接收区" styleClass="heading-2"/>
               <!-- 数据接收内容 -->
            </VBox>
            
            <!-- 右侧面板 -->
            <VBox prefWidth="336" styleClass="panel">
               <Label text="任务控制" styleClass="heading-2"/>
               <!-- 任务控制内容 -->
            </VBox>
         </HBox>
      </VBox>
   </center>
            
            <!-- 状态栏 -->
   <bottom>
      <HBox styleClass="status-bar">
         <Label text="系统状态：运行中" styleClass="caption-text"/>
         <Region HBox.hgrow="ALWAYS"/>
         <Label text="当前时间：2024-12-01 10:30:25" styleClass="caption-text"/>
      </HBox>
   </bottom>
</BorderPane>
```

### 对话框布局

```xml
<!-- 任务管理对话框 TaskManagement.fxml -->
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns="http://javafx.com/javafx/11.0.1" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.company.productionline.ui.controller.TaskManagementController"
      prefWidth="1000" prefHeight="600" styleClass="dialog-content">
   
   <!-- 工具栏 -->
   <HBox styleClass="toolbar">
      <Button text="添加任务" styleClass="primary-button" onAction="#onAddTask"/>
      <Button text="自动同步" styleClass="secondary-button" onAction="#onSyncTasks"/>
      <Region HBox.hgrow="ALWAYS"/>
      <TextField fx:id="searchField" promptText="搜索任务..." prefWidth="200"/>
   </HBox>
   
   <!-- 任务列表 -->
   <TableView fx:id="taskTable" VBox.vgrow="ALWAYS">
      <columns>
         <TableColumn text="生产单号" prefWidth="120"/>
         <TableColumn text="产品编号" prefWidth="100"/>
         <TableColumn text="产品名称" prefWidth="120"/>
         <TableColumn text="计划数量" prefWidth="80"/>
         <TableColumn text="完成数量" prefWidth="80"/>
         <TableColumn text="进度状态" prefWidth="80"/>
         <TableColumn text="操作" prefWidth="120"/>
      </columns>
   </TableView>
   
   <!-- 分页控制 -->
   <HBox styleClass="pagination-bar">
      <Label text="共 0 条记录" styleClass="caption-text"/>
      <Region HBox.hgrow="ALWAYS"/>
      <Pagination fx:id="pagination"/>
   </HBox>
</VBox>
```

## 🎯 状态指示器

### 状态标签样式

```css
/* 状态指示器 - StatusStyles.css */

/* 基础状态标签 */
.status-label {
    -fx-font-family: -primary-font;
    -fx-font-size: -font-size-small;
    -fx-font-weight: -font-weight-medium;
    -fx-padding: 4 8;
    -fx-background-radius: 12px;
    -fx-text-alignment: center;
}

/* 成功状态 */
.status-success {
    -fx-background-color: derive(-success-color, 80%);
    -fx-text-fill: -success-color;
}

/* 警告状态 */
.status-warning {
    -fx-background-color: derive(-warning-color, 80%);
    -fx-text-fill: derive(-warning-color, -40%);
}

/* 错误状态 */
.status-error {
    -fx-background-color: derive(-error-color, 80%);
    -fx-text-fill: -error-color;
}

/* 信息状态 */
.status-info {
    -fx-background-color: derive(-info-color, 80%);
    -fx-text-fill: -info-color;
}

/* 待处理状态 */
.status-pending {
    -fx-background-color: derive(-text-muted, 60%);
    -fx-text-fill: -text-muted;
}
```

## 📱 响应式设计

### 自适应布局

```css
/* 响应式布局 - ResponsiveLayout.css */

/* 大屏幕适配 (>1400px) */
.root {
    -fx-font-size: 14px;
}

/* 中等屏幕适配 (1024px-1400px) */
@media screen and (max-width: 1400px) {
    .root {
        -fx-font-size: 13px;
    }
    
    .primary-button {
        -fx-min-width: 70px;
        -fx-padding: 6 12;
    }
}

/* 小屏幕适配 (<1024px) */
@media screen and (max-width: 1024px) {
    .root {
        -fx-font-size: 12px;
    }
    
    .primary-button {
        -fx-min-width: 60px;
        -fx-padding: 4 8;
    }
    
    .table-view .table-cell {
        -fx-padding: 6 8;
    }
}
```

## 🎨 主题切换

### 主题管理

```java
// 主题管理器 ThemeManager.java
public class ThemeManager {
    private static final String LIGHT_THEME = "/css/themes/light-theme.css";
    private static final String DARK_THEME = "/css/themes/dark-theme.css";
    
    public static void applyTheme(Scene scene, String theme) {
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(
            ThemeManager.class.getResource("/css/base.css").toExternalForm(),
            ThemeManager.class.getResource(theme).toExternalForm()
        );
    }
    
    public static void applyLightTheme(Scene scene) {
        applyTheme(scene, LIGHT_THEME);
    }
    
    public static void applyDarkTheme(Scene scene) {
        applyTheme(scene, DARK_THEME);
    }
}
```

## 📋 样式应用指南

### 在FXML中应用样式

```xml
<!-- 在FXML中应用CSS类 -->
<Button text="确认" styleClass="primary-button"/>
<Label text="错误信息" styleClass="status-error"/>
<TextField styleClass="text-field"/>
<TableView styleClass="table-view"/>
```

### 在Java代码中应用样式

```java
// 在控制器中动态应用样式
@FXML
private Button confirmButton;

@FXML
private Label statusLabel;

public void initialize() {
    // 应用样式类
    confirmButton.getStyleClass().add("primary-button");
    
    // 动态设置状态样式
    updateStatus("success", "操作成功");
}

private void updateStatus(String type, String message) {
    statusLabel.setText(message);
    statusLabel.getStyleClass().clear();
    statusLabel.getStyleClass().addAll("status-label", "status-" + type);
}
```

## 🔧 开发最佳实践

### 1. 样式组织
- 将样式文件按功能模块分组
- 使用CSS变量定义主题色彩
- 保持样式命名的一致性

### 2. 组件复用
- 创建通用的样式类
- 使用组合而非继承
- 保持组件的独立性

### 3. 性能优化
- 避免过度嵌套的选择器
- 使用高效的CSS属性
- 合理使用缓存机制

### 4. 维护性
- 添加必要的注释
- 保持代码的可读性
- 定期重构和优化

---

**注意**: 本规范基于JavaFX 17+版本，确保开发环境的兼容性。所有样式都经过工控机环境的测试验证。 