# 规范：加密解密工具

## ADDED Requirements

### REQ-CRYPTO-001: RSA签名验证

#### Scenario: 验证许可证RSA签名
**Given** 系统需要验证许可证签名  
**When** 调用RSA签名验证  
**Then** 系统应：
- 从资源文件加载RSA公钥（/resources/keys/license_public.key）
- 使用RSA公钥验证许可证文件签名
- 如果签名有效：返回true
- 如果签名无效：返回false并记录错误

**实现要求**：
- 使用Java内置的RSA算法
- 签名算法：SHA256withRSA
- 公钥格式：Base64编码的PEM格式
- 公钥嵌入JAR资源文件

#### Scenario: 加载RSA公钥
**Given** 系统需要加载RSA公钥  
**When** 加载公钥  
**Then** 系统应：
- 从JAR资源文件读取公钥（/resources/keys/license_public.key）
- Base64解码公钥内容
- 解析PEM格式公钥
- 创建PublicKey对象
- 缓存PublicKey对象，避免重复加载

**实现要求**：
- 公钥文件不存在时抛出异常
- 公钥格式错误时抛出异常
- 使用单例模式缓存公钥

### REQ-CRYPTO-002: AES256解密

#### Scenario: 解密许可证payload
**Given** 系统需要解密许可证payload  
**When** 调用AES解密  
**Then** 系统应：
- 获取AES密钥（硬编码或密钥派生）
- 使用AES256算法解密payload
- 如果解密成功：返回解密后的JSON字符串
- 如果解密失败：抛出异常

**实现要求**：
- 使用AES256算法
- 工作模式：GCM（带认证加密）或CBC
- AES密钥硬编码（混淆保护）或基于设备ID派生
- 解密失败时抛出明确的异常

#### Scenario: AES密钥管理
**Given** 系统需要AES密钥  
**When** 获取AES密钥  
**Then** 系统应：
- 方案A（推荐）：密钥派生
  - 使用内置种子密钥
  - 结合设备ID派生实际AES密钥
  - 每个设备的AES密钥唯一
- 方案B：固定密钥
  - AES密钥硬编码在客户端
  - 使用ProGuard混淆保护
  - 分散存储在多个类中

**实现要求**：
- 密钥不能明文存储
- 使用代码混淆保护密钥
- 密钥派生方案更安全

### REQ-CRYPTO-003: Base64编码解码

#### Scenario: Base64编码解码
**Given** 系统需要编码/解码数据  
**When** 调用Base64工具  
**Then** 系统应提供：
- `encode(byte[] data)` - Base64编码
- `decode(String encoded)` - Base64解码
- 处理编码/解码异常

**实现要求**：
- 使用Java内置Base64工具
- 处理编码/解码异常
- 返回清晰的错误信息

### REQ-CRYPTO-004: 加密解密工具类

#### Scenario: 加密解密工具类
**Given** 系统需要加密解密功能  
**When** 使用加密解密工具类  
**Then** 工具类应提供：
- `verifyRSASignature(byte[] data, String signature)` - 验证RSA签名
- `decryptAES(byte[] encryptedData, byte[] key)` - AES解密
- `encodeBase64(byte[] data)` - Base64编码
- `decodeBase64(String encoded)` - Base64解码
- `loadPublicKey()` - 加载RSA公钥
- `getAESKey()` - 获取AES密钥

**实现要求**：
- 工具类：`LicenseCryptoUtil.java`
- 所有方法处理异常情况
- 提供清晰的错误信息
- 使用单例模式管理密钥

## MODIFIED Requirements

无

## REMOVED Requirements

无

