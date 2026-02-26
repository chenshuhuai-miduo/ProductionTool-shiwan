# 架构搭建

## 概述

本文档为产线采集关联软件提供完整的Java开发相关说明，基于JavaFX + Spring Boot + MyBatis-Plus技术栈

## 🚀 环境搭建

### 相关技术栈详细版本号版本
- **Java JDK**: 11   
- **Spring Boot**: 2.5.6
- **数据库**: MySQL 8.0.42
- **构建工具**: Maven 3.6.1
- **JavaFX**: 17.0.17
- **MyBatis-Plus**: 3.5.3.1
- **Swagger**: 3.0.0



## 📁 项目结构
ProCap/
├── src/main/java/com/example/ProCap/
│   ├── DemoApplication.java                    # Spring Boot 启动类
│   │
│   ├── controller/                             # 控制器层（REST接口）
│   │   └── UController.java                # 用户API接口
│   │
│   ├── service/                                # 业务逻辑层
│   │   ├── UService.java                       # 用户业务实现类
│   │   └── IService.java                      # 通用业务接口（Service基类）
│   │
│   ├── dto/                                   # 数据传输对象（共享）
│   │   ├── DTO.java                           # 用户数据模型
│   │   └── Result.java                       # 统一响应格式
│   │
│   ├── interceptor/                           # 拦截器层（请求拦截、权限校验等）
│   │   └── AuthInterceptor.java               # 登录或权限拦截器
│   │
│   ├── handler/                               # 全局异常和响应处理
│   │   ├── GlobalExceptionHandler.java        # 全局异常捕获
│   │   └── CustomException.java               # 自定义业务异常类
│   │
│   ├── utils/                                 # 工具类模块
│   │
│   ├── config/                                # 项目配置类
│   │   └── ExceptionConfig.java               # 异常、日志相关配置（可选）
│   │
│   └── javafx/                                # JavaFX前端层
│
├── src/main/resources/
│   ├── fxml/
│   │   └── t.fxml                    # JavaFX 界面文件
│   │
│   ├── static/                                # 静态资源（如CSS、图标）
│   ├── templates/                             # Thymeleaf模板（如需）
│   └── application.properties                 # Spring Boot 配置文件
│
└── pom.xml                                    # Maven 依赖配置




**注意**: 本指南基于Java 11 + JavaFX 17.0.17 + Spring Boot 2.5.6技术栈，确保开发环境版本兼容。 