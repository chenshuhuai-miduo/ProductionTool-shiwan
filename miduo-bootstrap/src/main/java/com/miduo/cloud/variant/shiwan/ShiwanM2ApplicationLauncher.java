package com.miduo.cloud.variant.shiwan;

import com.miduo.cloud.ShiwanM2BackendApplication;
import com.miduo.cloud.config.GlobalExceptionHandler;
import com.miduo.cloud.frontend.ShiwanM2FrontendApplication;
import com.miduo.cloud.frontend.util.FileLogManager;
import com.miduo.cloud.frontend.util.LogRedirector;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 石湾 2 号机（盒箱垛关联）统一启动器。
 * <p>
 * 顺序：先启动后端（Spring Boot），就绪后再启动前端（JavaFX 石湾 2 号机界面）；
 * 前端关闭时自动关闭后端。打包为 exe 或 fat jar 时使用此类作为主类，可实现「一次启动、先后端后前端」。
 * </p>
 *
 * @see ShiwanM2Launcher 仅启动前端的入口，需单独先启后端
 */
public class ShiwanM2ApplicationLauncher {

    private static ConfigurableApplicationContext backendContext;
    private static Thread backendThread;
    private static boolean isBackendReady = false;
    private static final CountDownLatch backendReadyLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        long launcherStartMs = System.currentTimeMillis();
        // 「关于系统」内容在 ShiwanM2FrontendApplication.main() 中配置
        initializeLogging();
        printWelcomeBanner();

        try {
            long backendBootBeginMs = System.currentTimeMillis();
            System.out.println("【步骤 1/3】正在启动后端服务...");
            System.out.println("----------------------------------------");
            startBackendService();
            long backendThreadDispatchedMs = System.currentTimeMillis();
            System.out.println("[启动耗时] 后端启动线程已派发，耗时 " + (backendThreadDispatchedMs - backendBootBeginMs) + " ms");

            System.out.println("\n【步骤 2/3】后端后台启动中（与前端并行）...");
            monitorBackendStartupAsync(launcherStartMs, 60);
            System.out.println("----------------------------------------");

            System.out.println("\n【步骤 3/3】正在启动石湾 2 号机前端...");
            System.out.println("----------------------------------------");
            long frontendHandoffMs = System.currentTimeMillis();
            System.out.println("[启动耗时] 进入前端启动前累计耗时 " + (frontendHandoffMs - launcherStartMs) + " ms");
            startFrontendApplication();

            System.out.println("\n========================================");
            System.out.println("石湾 2 号机前端已关闭");
            System.out.println("========================================");

            System.out.println("\n正在关闭后端服务...");
            shutdownBackend();
            shutdownLogging();

            System.out.println("\n========================================");
            System.out.println("✓ 所有服务已安全关闭");
            System.out.println("感谢使用石湾 2 号机盒箱垛关联系统！");
            System.out.println("========================================\n");

        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("✗ 应用程序启动失败！");
            System.err.println("错误: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            FileLogManager.getInstance().logError("系统启动", "石湾2号机应用程序启动失败", e);
            shutdownBackend();
            shutdownLogging();
            System.exit(1);
        }
    }

    private static void monitorBackendStartupAsync(long launcherStartMs, int timeoutSeconds) {
        Thread monitor = new Thread(() -> {
            long backendWaitBeginMs = System.currentTimeMillis();
            boolean backendStarted = waitForBackendReady(timeoutSeconds);
            long backendWaitEndMs = System.currentTimeMillis();
            if (backendStarted) {
                System.out.println("✓ 后端服务启动成功！");
                System.out.println("[启动耗时] 后端就绪等待耗时 " + (backendWaitEndMs - backendWaitBeginMs) + " ms");
                System.out.println("[启动耗时] 启动器累计耗时（到后端可用） " + (backendWaitEndMs - launcherStartMs) + " ms");
                System.out.println("  API地址：http://localhost:8080/api");
                System.out.println("----------------------------------------");
            } else {
                System.err.println("\n✗ 后端服务启动超时（" + timeoutSeconds + "秒）！");
                System.err.println("请检查：");
                System.err.println("  1. 端口8080是否被占用");
                System.err.println("  2. 数据库连接是否正常");
                System.err.println("  3. 配置文件是否正确");
                System.err.println("前端已启动，可稍后在系统页面重试相关后端功能。");
            }
        }, "ShiwanM2-Backend-Startup-Monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    private static void printWelcomeBanner() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║    石湾 2 号机 - 盒箱垛关联系统             ║");
        System.out.println("║    Shiwan M2 Box-Pallet Association          ║");
        System.out.println("║                                              ║");
        System.out.println("║    统一启动器 (先后端 → 再前端)              ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("系统信息:");
        System.out.println("  Java版本: " + System.getProperty("java.version"));
        System.out.println("  操作系统: " + System.getProperty("os.name"));
        System.out.println("  工作目录: " + System.getProperty("user.dir"));
        System.out.println();
        System.out.println("========================================");
        System.out.println("正在启动石湾 2 号机（后端 + 前端）...");
        System.out.println("========================================\n");
    }

    private static void startBackendService() {
        backendThread = new Thread(() -> {
            try {
                System.out.println("正在初始化 Spring Boot 后端...");
                backendContext = SpringApplication.run(ShiwanM2BackendApplication.class,
                    "--spring.profiles.active=shiwan-m2");
                isBackendReady = true;
                backendReadyLatch.countDown();
                System.out.println("✓ Spring Boot 后端启动完成");
            } catch (Exception e) {
                System.err.println("✗ 后端服务启动失败：" + e.getMessage());
                e.printStackTrace();
                backendReadyLatch.countDown();
            }
        }, "ShiwanM2-Backend-Startup-Thread");
        backendThread.setDaemon(false);
        backendThread.start();
    }

    private static boolean waitForBackendReady(int timeoutSeconds) {
        try {
            boolean success = backendReadyLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!success) return false;
            return isBackendReady && backendContext != null && backendContext.isActive();
        } catch (InterruptedException e) {
            System.err.println("等待后端启动时被中断：" + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void startFrontendApplication() {
        try {
            ShiwanM2FrontendApplication.main(new String[]{});
        } catch (Exception e) {
            System.err.println("石湾 2 号机前端启动失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("前端启动失败", e);
        }
    }

    private static void shutdownBackend() {
        try {
            if (backendContext != null && backendContext.isActive()) {
                System.out.println("正在停止 Spring Boot 后端...");
                SpringApplication.exit(backendContext, () -> 0);
                backendContext.close();
                System.out.println("✓ 后端服务已关闭");
            }
            if (backendThread != null && backendThread.isAlive()) {
                backendThread.join(5000);
            }
        } catch (Exception e) {
            System.err.println("关闭后端服务时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeLogging() {
        try {
            GlobalExceptionHandler.install();
            FileLogManager.getInstance().start();
            LogRedirector.install();
            FileLogManager.getInstance().logInfo("系统启动", "石湾2号机（后端+前端）正在启动...");
            FileLogManager.getInstance().logInfo("系统启动", "Java版本: " + System.getProperty("java.version"));
            FileLogManager.getInstance().logInfo("系统启动", "操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            FileLogManager.getInstance().logInfo("系统启动", "工作目录: " + System.getProperty("user.dir"));
            FileLogManager.getInstance().logInfo("系统启动", "日志文件路径: " + FileLogManager.getInstance().getCurrentLogFilePath());
            System.out.println("[日志系统] 文件日志已启动");
        } catch (Exception e) {
            System.err.println("[日志系统] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void shutdownLogging() {
        try {
            FileLogManager.getInstance().logInfo("系统关闭", "石湾2号机正在关闭...");
            FileLogManager.getInstance().stop();
            System.out.println("✓ 日志系统已关闭");
        } catch (Exception e) {
            System.err.println("[日志系统] 关闭失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n检测到关闭信号，正在清理资源...");
            shutdownBackend();
            shutdownLogging();
        }, "ShiwanM2-Shutdown-Hook"));
    }
}
