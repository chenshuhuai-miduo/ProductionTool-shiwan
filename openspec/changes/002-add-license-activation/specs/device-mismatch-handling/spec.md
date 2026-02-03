# 规范：设备不匹配处理

## ADDED Requirements

### REQ-DEVICE-MISMATCH-001: 设备ID不匹配检测

#### Scenario: 检测设备ID不匹配
**Given** 系统验证许可证  
**When** 比较许可证中的deviceId与当前设备ID  
**Then** 系统应：
- 获取许可证中的deviceId
- 重新生成当前设备的deviceId
- 比较两个deviceId是否相等
- 如果不相等：检测到设备不匹配
- 如果相等：设备匹配，继续验证流程

**实现要求**：
- 设备ID比较使用字符串相等比较
- 设备不匹配时停止验证流程
- 记录设备不匹配日志

### REQ-DEVICE-MISMATCH-002: 设备不匹配提示对话框

#### Scenario: 显示设备不匹配提示
**Given** 检测到设备ID不匹配  
**When** 系统显示设备不匹配提示  
**Then** 系统应显示对话框：
- 界面实现参照D:\MiDuoCode\productiontool\productiontools\202510产线采集关联软件\202510产线采集关联软件\DEV\ActiveCode\device-mismatch.html
- 警告图标（⚠）
- 标题："设备验证失败"
- 提示文字："检测到设备信息已变更，无法验证许可证"
- 可能原因说明：
  - "• 更换了主板或CPU"
  - "• 重装系统后硬件ID变化"
- 当前设备信息显示：
  - "设备ID: a3b2****o5p6"（部分隐藏）
  - "设备型号: HP EliteDesk 800 G5"
- 解决方法说明："请联系米多专员或拨打400-XXX-XXXX，重新申请许可证"
- [退出软件]按钮
- 对话框不可关闭（无×按钮）

**实现要求**：
- 对话框模态显示
- 必须退出软件
- 显示当前设备信息，便于用户联系客服

## MODIFIED Requirements

无

## REMOVED Requirements

无

