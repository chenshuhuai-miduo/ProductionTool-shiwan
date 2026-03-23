# config/ 目录说明

本目录是**开发参考文档**，所有文件均不会被任何代码加载，不影响运行时行为。

## 目录结构

```
config/
  variants/          产线变体说明（每条产线一个文件）
  features/          功能矩阵说明（功能 ↔ 产线对应关系）
```

## 与代码的对应关系

| 要做什么 | 改哪里 |
|---|---|
| 记录/更新产线功能说明（文档） | `config/variants/{variant}.yml` |
| 添加产线运行时资源（about.properties、mapper xml 等） | `miduo-variant-{name}/src/main/resources/` |
| 修改后端 Spring 配置（数据库、功能开关等） | `miduo-bootstrap/src/main/resources/application-{profile}.properties` |
| 添加产线专属 Java 业务代码 | `miduo-variant-{name}/src/main/java/` |

## 典型场景

**新增一条产线**（如「天河产线」）：
1. 新建 `config/variants/tianhe.yml`（写文档）
2. 新建 Maven 模块 `miduo-variant-tianhe/`（写代码 + 运行时资源）
3. 新建 `miduo-bootstrap/src/main/resources/application-tianhe.properties`（写 Spring 配置）
4. 新建启动器 `TianheApplicationLauncher.java`（设 `app.about.file` 系统属性）
