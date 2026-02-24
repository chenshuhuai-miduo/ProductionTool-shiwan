@echo off
chcp 65001 >nul
echo ========================================
echo 米多星球产线采集系统 - 编译打包脚本
echo ========================================
echo.

echo [步骤 1/3] 清理项目...
call mvn clean -q
if %errorlevel% neq 0 (
    echo 清理失败！
    pause
    exit /b 1
)
echo ✓ 清理完成
echo.

echo [步骤 2/3] 安装依赖模块...
call mvn install -DskipTests -pl miduo-common,miduo-entity,miduo-domain,miduo-infrastructure,miduo-application,miduo-controller,miduo-frontend -am -q
if %errorlevel% neq 0 (
    echo 安装依赖失败！
    pause
    exit /b 1
)
echo ✓ 依赖安装完成
echo.

echo [步骤 3/3] 打包 bootstrap 模块...
call mvn package -DskipTests -pl miduo-bootstrap -q
if %errorlevel% neq 0 (
    echo 打包失败！
    pause
    exit /b 1
)
echo ✓ 打包完成
echo.

echo ========================================
echo 打包成功！
echo ========================================
echo.
echo JAR 文件位置：
echo   miduo-bootstrap\target\miduo-launcher-launcher.jar
echo.
echo 启动命令：
echo   java -jar miduo-bootstrap\target\miduo-launcher-launcher.jar
echo.
pause













