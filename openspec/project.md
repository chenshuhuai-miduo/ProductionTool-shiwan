# Project Context

## Purpose
产线采集关联软件（米多星球后端基础服务）是一个用于制造业生产线的数据采集和追溯系统。主要功能包括：
- 生产任务管理和执行
- 多层级码（箱码、托盘码等）的采集和关联
- 数据上传到标识平台
- 产品管理和码查询/替换
- 系统配置和设备管理
- 操作日志记录和追溯

项目目标是构建一套自主可控、标准化的产线采集关联软件平台，实现生产过程全程追溯，减少对第三方供应商的依赖。

## Tech Stack

### 后端技术
- **Java**: JDK 11
- **Spring Boot**: 2.5.6
- **MyBatis-Plus**: 3.5.3.1
- **数据库**: 
  - MySQL 8.0.33（主要）
  - SQL Server（通过 JTDS 1.3.1 驱动）
- **构建工具**: Maven 3.6.1+
- **工具库**:
  - Lombok 1.18.24（代码简化）
  - Jackson 2.13.0（JSON处理）
  - Apache HttpClient 4.5.13（HTTP通信）
  - Guava 31.1-jre（工具类）
  - jSerialComm 2.10.4（串口通信）

### 前端技术
- **JavaFX**: 17.0.2
- **FXML**: 界面布局定义
- **CSS**: 自定义样式系统

### 开发工具
- **IDE**: IntelliJ IDEA（推荐）
- **版本控制**: Git

## Project Conventions

### Code Style
- **包命名**: `com.miduo.cloud.[layer].[module]`
  - 示例: `com.miduo.cloud.domain.task.model`
- **类命名**: 使用大驼峰（PascalCase）
  - 实体类: `Task`, `Product`, `CodeRelation`
  - 服务类: `TaskService`, `ProductService`
  - 控制器: `TaskController`
- **方法命名**: 使用小驼峰（camelCase），动词开头
  - 示例: `createTask()`, `queryCode()`, `uploadData()`
- **常量命名**: 全大写下划线分隔
  - 示例: `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`
- **代码格式化**: 使用 IDEA 默认 Java 代码风格，4 空格缩进
- **注释规范**: 
  - 类和方法使用 JavaDoc 注释
  - 复杂业务逻辑添加行内注释
  - 中文注释（业务相关），英文注释（技术实现）

### Architecture Patterns

#### 分层架构（DDD 风格）
项目采用领域驱动设计（DDD）的分层架构：

1. **miduo-entity**: 实体层
   - PO（Persistent Object）：数据库实体
   - 使用 MyBatis-Plus 注解

2. **miduo-domain**: 领域层
   - 领域模型（Model）
   - 领域服务（Service）
   - 仓储接口（Repository）

3. **miduo-application**: 应用层
   - 应用服务（Application Service）
   - DTO 转换
   - 业务流程编排

4. **miduo-infrastructure**: 基础设施层
   - 数据访问实现（Mapper）
   - 外部服务集成
   - 技术组件封装

5. **miduo-controller**: 控制器层
   - REST API 接口
   - 请求/响应处理
   - 参数验证

6. **miduo-frontend**: 前端层
   - JavaFX 界面
   - FXML 布局
   - 前端业务逻辑

7. **miduo-bootstrap**: 启动层
   - Spring Boot 启动类
   - 应用配置
   - 统一启动入口

8. **miduo-common**: 公共层
   - 通用工具类
   - 公共 DTO
   - 常量定义

#### 设计原则
- **单一职责**: 每个类和方法只做一件事
- **依赖倒置**: 领域层不依赖基础设施层，通过接口解耦
- **接口隔离**: 接口设计精简，避免臃肿
- **开闭原则**: 对扩展开放，对修改关闭

### Testing Strategy
- **单元测试**: 使用 JUnit 5 测试业务逻辑
- **集成测试**: 测试数据库交互和 API 接口
- **测试覆盖率**: 关键业务逻辑要求覆盖
- **测试数据**: 使用测试数据库，避免污染生产数据

### Git Workflow
- **分支策略**: 
  - `main`: 主分支，生产环境代码
  - `develop`: 开发分支
  - `feature/*`: 功能分支
  - `bugfix/*`: 缺陷修复分支
- **提交规范**: 
  - 提交信息使用中文
  - 格式: `[类型] 简短描述`
  - 类型: `feat`（新功能）、`fix`（修复）、`refactor`（重构）、`docs`（文档）、`style`（格式）、`test`（测试）
  - 示例: `[feat] 添加任务创建功能`

## Domain Context

### 核心业务概念
- **任务（Task）**: 生产任务，包含订单信息、产品信息、数量等
- **码（Code）**: 各层级的标识码，包括：
  - 产品码（最小单位）
  - 箱码（包装单位）
  - 托盘码（物流单位）
- **码关联（CodeRelation）**: 记录不同层级码之间的关联关系
- **产品（Product）**: 产品基础信息，包括产品编码、名称、规格等
- **设备（IoDevice）**: IO 设备配置，用于数据采集
- **操作日志（OperateLog）**: 记录用户操作和系统事件

### 业务流程
1. **任务创建**: 管理员创建生产任务，指定产品、数量、订单等信息
2. **任务执行**: 操作员选择任务，开始采集码数据
3. **码采集**: 通过扫描设备采集各层级码，建立关联关系
4. **数据上传**: 将采集的数据上传到标识平台
5. **码查询/替换**: 支持查询码关联关系和替换错误码

### 技术特点
- **串口通信**: 通过 jSerialComm 与工业设备通信
- **批量处理**: 使用批量插入和批量上传优化性能
- **Bloom Filter**: 使用 Guava Bloom Filter 优化码查询性能
- **异步处理**: 前端使用 JavaFX 异步线程处理耗时操作

## Important Constraints

### 技术约束
- **Java 版本**: 必须使用 JDK 11，不支持更高版本（JavaFX 17 兼容性）
- **数据库**: 支持 MySQL 和 SQL Server，使用 JTDS 驱动连接 SQL Server（解决 TLS 版本兼容问题）
- **运行环境**: Windows 工控机环境，需要稳定运行
- **网络环境**: 需要连接到标识平台进行数据上传

### 业务约束
- **数据一致性**: 码关联关系必须完整，不允许缺失
- **追溯要求**: 必须能够完整追溯产品的生产过程
- **实时性**: 数据采集和上传需要实时或准实时
- **可靠性**: 系统需要7x24小时稳定运行，异常情况需要自动恢复

### 性能约束
- **并发处理**: 支持多设备并发采集
- **数据库连接池**: 最大连接数 50，最小空闲连接数 10
- **批量操作**: 使用批量处理优化性能

## External Dependencies

### 外部服务
- **标识平台**: 通过 HTTP API 上传码关联数据
  - 接口地址: 配置在系统配置中
  - 认证方式: 通过配置的认证信息

### 硬件设备
- **扫描设备**: 通过串口通信采集码数据
  - 串口配置: 在系统配置中设置
  - 通信协议: 根据设备类型配置

### 数据库
- **生产数据库**: SQL Server（192.168.5.22:1433/cshTest_db）
- **开发数据库**: MySQL（本地开发环境）

### 配置文件
- `application.properties`: Spring Boot 主配置
- `application-device-only.properties`: 仅设备模式配置
- `io_devices.ini`: IO 设备配置文件
