# 规范：设备请求文件生成和解析

## ADDED Requirements

### REQ-DEVREQ-001: 设备请求文件生成

#### Scenario: 导出设备请求文件
**Given** 用户点击"导出设备请求文件"按钮  
**When** 系统生成设备请求文件  
**Then** 系统应：
- 采集当前设备信息
- 生成设备唯一码
- 创建.devreq文件，包含以下字段：
  - fileType: "DEVICE_ACTIVATION_REQUEST"
  - version: "1.0"
  - createTime: 当前时间（格式：yyyy-MM-dd HH:mm:ss）
  - deviceInfo: 设备信息对象
    - deviceId: 设备唯一码
    - baseboardSerial: 主板序列号
    - cpuId: CPU ID
    - manufacturer: 制造商
    - deviceModel: 设备型号
    - osVersion: 操作系统版本
    - appVersion: 应用版本
  - requestType: "activation"
  - checksum: MD5校验和
- 将JSON内容进行Base64编码
- 保存文件到桌面，文件名格式：`设备激活请求_{设备ID前8位}_{日期}.devreq`
- 显示保存成功提示

**实现要求**：
- 文件格式：Base64编码的JSON字符串
- 文件扩展名：.devreq
- 文件编码：UTF-8
- 错误处理：文件保存失败时显示错误提示

### REQ-DEVREQ-002: 设备请求文件UI交互

#### Scenario: 离线激活向导步骤1
**Given** 用户进入离线激活向导  
**When** 显示步骤1（导出设备请求文件）  
**Then** 界面应显示：
- 界面实现参照D:\MiDuoCode\productiontool\productiontools\202510产线采集关联软件\202510产线采集关联软件\DEV\ActiveCode\offline-activation.html
- 文件图标
- 标题："导出设备请求文件"
- 说明文字："将生成的文件发送给米多客服"
- 设备ID显示（部分隐藏：a3b2****o5p6）
- "导出设备请求文件"按钮
- 文件保存路径提示
- "取消"和"下一步"按钮

**实现要求**：
- 使用JavaFX FileChooser选择保存位置
- 默认保存到桌面
- 导出成功后启用"下一步"按钮

## MODIFIED Requirements

无

## REMOVED Requirements

无


