# 规范：许可证文件格式定义

## ADDED Requirements

### REQ-FORMAT-001: 许可证文件结构

#### Scenario: 许可证文件JSON结构
**Given** 系统需要定义许可证文件格式  
**When** 查看许可证文件结构  
**Then** 文件应包含以下JSON结构：
- `header`对象：
  - `version`: "1.0"（字符串）
  - `type`: "TRIAL" 或 "STANDARD"（字符串）
  - `keyVersion`: "2025"（字符串，密钥版本号）
  - `algorithm`: "AES256+RSA2048"（字符串）
- `payload`对象（AES256加密后的Base64字符串）：
  - 解密后包含：
    - `licenseKey`: 激活码（格式：XXXX-XXXX-XXXX）
    - `licenseType`: "TRIAL" 或 "STANDARD"
    - `deviceId`: 设备ID（32位MD5哈希值）
    - `activationDate`: 激活日期（yyyy-MM-dd格式）
    - `expireDate`: 到期日期（yyyy-MM-dd格式）
    - `validDays`: 有效天数（整数）
    - `features`: ["all"]（功能列表）
- `signature`: RSA2048签名的Base64字符串

**实现要求**：
- 文件格式：JSON
- 文件扩展名：.lic
- 最终文件Base64编码存储
- payload使用AES256加密
- 整个文件使用RSA私钥签名

### REQ-FORMAT-002: 许可证文件实体类

#### Scenario: 许可证文件实体类定义
**Given** 系统需要处理许可证文件  
**When** 定义许可证文件实体类  
**Then** 系统应提供：
- `LicenseHeader.java` - 许可证头部类
  - version字段
  - type字段
  - keyVersion字段
  - algorithm字段
- `LicensePayload.java` - 许可证载荷类
  - licenseKey字段
  - licenseType字段
  - deviceId字段
  - activationDate字段
  - expireDate字段
  - validDays字段
  - features字段
- `LicenseFile.java` - 许可证文件类
  - header字段（LicenseHeader类型）
  - payload字段（String类型，加密后的Base64字符串）
  - signature字段（String类型）

**实现要求**：
- 使用Jackson进行JSON序列化/反序列化
- 字段使用@JsonProperty注解
- 提供getter/setter方法
- 支持JSON Schema验证（可选）

### REQ-FORMAT-003: 许可证文件解析

#### Scenario: 解析许可证文件
**Given** 系统需要解析许可证文件  
**When** 解析.lic文件  
**Then** 系统应：
- Base64解码文件内容
- 解析JSON结构
- 验证header字段（version、type、algorithm）
- 提取signature字段
- 提取payload字段（加密的Base64字符串）
- 返回LicenseFile对象

**实现要求**：
- 使用Jackson解析JSON
- 处理JSON解析异常
- 验证必需字段存在
- 返回清晰的错误信息

#### Scenario: 构建许可证文件对象
**Given** 系统需要构建许可证文件对象  
**When** 构建LicenseFile对象  
**Then** 系统应：
- 创建LicenseHeader对象并设置字段
- 创建LicensePayload对象并设置字段
- 将payload序列化为JSON
- 使用AES256加密payload
- Base64编码加密后的payload
- 使用RSA私钥签名整个文件（在后台管理系统完成）
- 构建LicenseFile对象

**实现要求**：
- 使用Jackson序列化JSON
- 加密和签名在后台管理系统完成
- 客户端仅负责解析和验证

### REQ-FORMAT-004: 设备请求文件格式

#### Scenario: 设备请求文件JSON结构
**Given** 系统需要定义设备请求文件格式  
**When** 查看设备请求文件结构  
**Then** 文件应包含以下JSON结构：
- `fileType`: "DEVICE_ACTIVATION_REQUEST"（字符串）
- `version`: "1.0"（字符串）
- `createTime`: "yyyy-MM-dd HH:mm:ss"（字符串，创建时间）
- `deviceInfo`对象：
  - `deviceId`: 设备唯一码（32位MD5哈希值）
  - `baseboardSerial`: 主板序列号
  - `cpuId`: CPU ID
  - `manufacturer`: 制造商
  - `deviceModel`: 设备型号
  - `osVersion`: 操作系统版本
  - `appVersion`: 应用版本
- `requestType`: "activation"（字符串）
- `checksum`: HMAC-SHA256校验和（字符串）

**实现要求**：
- 文件格式：JSON
- 文件扩展名：.devreq
- 最终文件Base64编码存储
- 使用HMAC-SHA256计算校验和

## MODIFIED Requirements

无

## REMOVED Requirements

无



