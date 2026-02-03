# 规范：设备信息采集和唯一码生成

## ADDED Requirements

### REQ-DEVICE-001: 设备信息采集

#### Scenario: 采集设备硬件信息
**Given** 应用需要生成设备唯一码  
**When** 调用设备信息采集服务  
**Then** 系统应采集以下信息：
- 主板序列号（baseboardSerial）
- CPU ID（cpuId）
- 制造商（manufacturer）
- 设备型号（deviceModel）
- 操作系统版本（osVersion）
- 应用版本（appVersion）

**实现要求**：
- 使用OSHI库获取硬件信息
- 处理硬件信息获取失败的情况（返回默认值或抛出异常）
- 支持Windows平台

#### Scenario: 设备唯一码生成
**Given** 已采集设备硬件信息  
**When** 调用设备唯一码生成器  
**Then** 系统应：
- 将主板序列号、CPU ID、制造商组合成字符串
- 使用MD5算法生成32位哈希值
- 返回格式化的设备唯一码（小写，无分隔符）

**实现要求**：
- 使用Apache Commons Codec的MD5工具
- 确保相同硬件信息生成相同唯一码
- 确保不同硬件信息生成不同唯一码

#### Scenario: 设备信息持久化
**Given** 设备信息已采集  
**When** 需要保存设备信息  
**Then** 系统应：
- 将设备信息保存到设备请求文件（.devreq）
- 包含设备唯一码（deviceId）
- 包含所有硬件信息字段
- 包含创建时间（createTime）
- 包含文件类型标识（fileType: "DEVICE_ACTIVATION_REQUEST"）

## MODIFIED Requirements

无

## REMOVED Requirements

无


