# 规范：许可证文件管理

## ADDED Requirements

### REQ-FILE-001: 许可证文件存储

#### Scenario: 许可证文件保存路径
**Given** 系统需要保存许可证文件  
**When** 保存许可证文件  
**Then** 系统应：
- 使用路径：`%APPDATA%\miduo\license\production_line.lic`
- 如果目录不存在，自动创建目录
- 保存文件时覆盖已存在的文件
- 保存成功后返回成功状态

**实现要求**：
- 使用`System.getenv("APPDATA")`获取APPDATA路径
- 目录创建失败时抛出异常
- 文件保存失败时抛出异常

#### Scenario: 读取许可证文件
**Given** 系统需要读取许可证文件  
**When** 读取本地许可证文件  
**Then** 系统应：
- 检查文件是否存在（%APPDATA%\miduo\license\production_line.lic）
- 如果不存在：返回null或空状态
- 如果存在：
  - 读取文件内容
  - Base64解码
  - 返回许可证文件对象
- 文件读取失败时抛出异常

**实现要求**：
- 使用缓存机制，避免重复读取
- 文件读取失败时记录日志
- 返回清晰的错误信息

#### Scenario: 删除许可证文件
**Given** 系统需要删除许可证文件  
**When** 删除本地许可证文件  
**Then** 系统应：
- 检查文件是否存在
- 如果存在：删除文件
- 如果不存在：返回成功（无需删除）
- 删除失败时抛出异常

**实现要求**：
- 删除前确认文件存在
- 删除失败时记录日志
- 返回操作结果

### REQ-FILE-002: 许可证文件备份

#### Scenario: 注册表备份存储（可选）
**Given** 系统需要备份许可证信息  
**When** 保存许可证文件后  
**Then** 系统可选择：
- 将关键信息备份到注册表：
  - 路径：`HKEY_CURRENT_USER\Software\Miduo\ProductionLine`
  - 键名：`LicenseInfo`（加密存储）
  - 键名：`LastVerifyTime`（上次验证时间）
- 备份用于校验和恢复

**实现要求**：
- 备份为可选功能
- 备份信息加密存储
- 备份失败不影响主流程

### REQ-FILE-003: 许可证文件工具类

#### Scenario: 许可证文件工具类
**Given** 系统需要操作许可证文件  
**When** 使用许可证文件工具类  
**Then** 工具类应提供：
- `saveLicenseFile(LicenseFile licenseFile)` - 保存许可证文件
- `loadLicenseFile()` - 读取许可证文件
- `deleteLicenseFile()` - 删除许可证文件
- `existsLicenseFile()` - 检查文件是否存在
- `getLicenseFilePath()` - 获取许可证文件路径

**实现要求**：
- 工具类：`LicenseFileUtil.java`
- 所有方法处理异常情况
- 提供清晰的错误信息

## MODIFIED Requirements

无

## REMOVED Requirements

无

