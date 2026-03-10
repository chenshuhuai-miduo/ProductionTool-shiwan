<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

## Cursor Cloud specific instructions

### Project Overview

This is the **米多赋码采集关联系统 (Miduo CCAS)** — an industrial production-line code collection and traceability system. It is a Java multi-module Maven project with a Spring Boot backend (port 8080) and a JavaFX desktop frontend.

### Prerequisites (installed by VM snapshot)

- **JDK 17** at `/usr/lib/jvm/java-17-openjdk-amd64` — set as default via `update-alternatives`. Must use JDK 17, not JDK 21, for Spring Boot 2.5.6 compatibility.
- **Maven 3.8.7** — installed via `apt`.
- **MySQL 8.0** — database `miduo_ccas` with tables `ProductionOrder`, `ProductionOrderDetail`, `CodeRelationUpload`, `OperateLog`. User: `miduo` / `miduo123`.
- **Xvfb** — needed for running the JavaFX frontend on headless Linux.

### Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn clean install -DskipTests
```

### Starting MySQL

MySQL may not auto-start in the container. Start it manually:

```bash
sudo mysqld_safe &
sleep 3
```

### Starting the Backend

The default `application.properties` targets SQL Server. Override datasource properties to use local MySQL:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export DISPLAY=:99
mvn spring-boot:run -pl miduo-bootstrap \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/miduo_ccas?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver --spring.datasource.username=miduo --spring.datasource.password=miduo123 --mybatis-plus.global-config.db-config.db-type=mysql"
```

### Starting the JavaFX Frontend

Requires Xvfb (or a real display):

```bash
Xvfb :99 -screen 0 1400x900x24 -ac &
export DISPLAY=:99
mvn exec:java -pl miduo-frontend -Dexec.mainClass="com.miduo.cloud.frontend.FrontendMain"
```

### Known Caveats

1. **Pagination SQL is SQL Server-specific**: `MyBatisConfig.java` hardcodes `DbType.SQL_SERVER2005` for the pagination interceptor. Non-paginated queries (task creation, order listing, code operations) work fine with MySQL. Paginated endpoints (e.g., `POST /api/task/page`) will fail with MySQL.
2. **JavaFX plugin main class mismatch**: The `javafx-maven-plugin` in `miduo-frontend/pom.xml` references `com.miduo.cloud.frontend.application.FrontendLauncher` which does not exist. Use `mvn exec:java` with `FrontendMain` instead.
3. **License activation required**: The frontend shows a license activation dialog on startup. Without a valid license, the main application UI cannot be accessed.
4. **No automated tests**: The project has no JUnit tests under `src/test`. The files in `src/main/.../test/` are manual test utilities, not JUnit tests.
5. **No separate linting tool**: There is no Checkstyle/PMD/SpotBugs configured. `mvn compile` is the standard code quality check.