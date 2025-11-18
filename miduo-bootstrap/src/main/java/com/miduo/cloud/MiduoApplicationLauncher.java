package com.miduo.cloud;

import com.miduo.cloud.config.GlobalExceptionHandler;
import com.miduo.cloud.frontend.util.FileLogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 米多星球产线采集系统 - 统一启动类
 * 负责启动和管理前端和后端应用程序
 * 
 * 功能：
 * 1. 先启动后端服务（Spring Boot）
 * 2. 等待后端完全启动后再启动前端（JavaFX）
 * 3. 前端关闭时自动关闭后端
 * 
 * 使用方式：
 * - IDEA: 直接运行此类的main方法
 * - 命令行: java -cp "classpath" com.miduo.cloud.MiduoApplicationLauncher
 */
public class MiduoApplicationLauncher {
    
    private static ConfigurableApplicationContext backendContext;
    private static Thread backendThread;
    private static boolean isBackendReady = false;
    private static final CountDownLatch backendReadyLatch = new CountDownLatch(1);
    
    public static void main(String[] args) {
        // 初始化文件日志系统
        initializeLogging();
        
        printWelcomeBanner();
        
        try {
            // 1. 启动后端服务（在独立线程中）
            System.out.println("【步骤 1/3】正在启动后端服务...");
            System.out.println("----------------------------------------");
            startBackendService();
            
            // 2. 等待后端服务完全启动
            System.out.println("\n【步骤 2/3】等待后端服务完全启动...");
            boolean backendStarted = waitForBackendReady(60); // 最多等待60秒
            
            if (!backendStarted) {
                System.err.println("\n✗ 后端服务启动超时（60秒）！");
                System.err.println("请检查：");
                System.err.println("  1. 端口8080是否被占用");
                System.err.println("  2. 数据库连接是否正常");
                System.err.println("  3. 配置文件是否正确");
                shutdownBackend();
                System.exit(1);
                return;
            }
            
            System.out.println("✓ 后端服务启动成功！");
            System.out.println("  API地址：http://localhost:8080/api");
            System.out.println("----------------------------------------");
            
            // 3. 启动前端应用（在当前线程中，会阻塞直到前端关闭）
            System.out.println("\n【步骤 3/3】正在启动前端应用...");
            System.out.println("----------------------------------------");
            startFrontendApplication();
            
            // 前端关闭后执行清理
            System.out.println("\n========================================");
            System.out.println("前端应用已关闭");
            System.out.println("========================================");
            
            // 4. 关闭后端服务
            System.out.println("\n正在关闭后端服务...");
            shutdownBackend();
            
            // 5. 关闭日志系统
            shutdownLogging();
            
            System.out.println("\n========================================");
            System.out.println("✓ 所有服务已安全关闭");
            System.out.println("感谢使用米多星球产线采集系统！");
            System.out.println("========================================\n");
            
        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("✗ 应用程序启动失败！");
            System.err.println("错误: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
            
            // 记录异常到日志
            FileLogManager.getInstance().logError("系统启动", "应用程序启动失败", e);
            
            // 确保清理资源
            shutdownBackend();
            shutdownLogging();
            System.exit(1);
        }
    }
    
    /**
     * 打印欢迎横幅
     */
    private static void printWelcomeBanner() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║    米多星球产线采集系统 v2.0.0              ║");
        System.out.println("║    Miduo Production Line Collection System  ║");
        System.out.println("║                                              ║");
        System.out.println("║    统一启动器 (Backend + Frontend)         ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("系统信息:");
        System.out.println("  Java版本: " + System.getProperty("java.version"));
        System.out.println("  操作系统: " + System.getProperty("os.name"));
        System.out.println("  工作目录: " + System.getProperty("user.dir"));
        System.out.println();
        System.out.println("========================================");
        System.out.println("正在启动米多星球产线采集系统...");
        System.out.println("========================================\n");
    }
    
    /**
     * 启动后端服务（在独立线程中）
     */
    private static void startBackendService() {
        backendThread = new Thread(() -> {
            try {
                System.out.println("正在初始化Spring Boot应用...");
                
                // 启动Spring Boot应用
                backendContext = SpringApplication.run(MiduoBackendApplication.class);
                
                // 标记后端已就绪
                isBackendReady = true;
                backendReadyLatch.countDown();
                
                System.out.println("✓ Spring Boot应用启动完成");
                
            } catch (Exception e) {
                System.err.println("✗ 后端服务启动失败：" + e.getMessage());
                e.printStackTrace();
                backendReadyLatch.countDown(); // 即使失败也要释放锁
            }
        }, "Backend-Startup-Thread");
        
        backendThread.setDaemon(false); // 非守护线程
        backendThread.start();
    }
    
    /**
     * 等待后端服务完全启动
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否成功启动
     */
    private static boolean waitForBackendReady(int timeoutSeconds) {
        try {
            // 等待后端启动信号
            boolean success = backendReadyLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!success) {
                return false;
            }
            
            // 额外等待1秒确保所有组件初始化完成
            Thread.sleep(1000);
            
            return isBackendReady && backendContext != null && backendContext.isActive();
            
        } catch (InterruptedException e) {
            System.err.println("等待后端启动时被中断：" + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 启动前端应用（在当前线程中，会阻塞）
     */
    private static void startFrontendApplication() {
        try {
            // 直接调用前端的main方法（会阻塞直到前端关闭）
            com.miduo.cloud.frontend.FrontendMain.main(new String[]{});
            
        } catch (Exception e) {
            System.err.println("前端应用启动失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("前端启动失败", e);
        }
    }
    
    /**
     * 关闭后端服务
     */
    private static void shutdownBackend() {
        try {
            if (backendContext != null && backendContext.isActive()) {
                System.out.println("正在停止Spring Boot应用...");
                
                // 优雅关闭Spring Boot应用
                SpringApplication.exit(backendContext, () -> 0);
                backendContext.close();
                
                System.out.println("✓ 后端服务已关闭");
            }
            
            // 等待后端线程结束
            if (backendThread != null && backendThread.isAlive()) {
                backendThread.join(5000); // 最多等待5秒
            }
            
        } catch (Exception e) {
            System.err.println("关闭后端服务时发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化日志系统
     */
    private static void initializeLogging() {
        try {
            // 1. 安装全局异常处理器
            GlobalExceptionHandler.install();
            
            // 2. 启动文件日志管理器
            FileLogManager.getInstance().start();
            
            // 3. 记录系统启动日志
            FileLogManager.getInstance().logInfo("系统启动", "米多星球产线采集系统正在启动...");
            FileLogManager.getInstance().logInfo("系统启动", "Java版本: " + System.getProperty("java.version"));
            FileLogManager.getInstance().logInfo("系统启动", "操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            FileLogManager.getInstance().logInfo("系统启动", "工作目录: " + System.getProperty("user.dir"));
            FileLogManager.getInstance().logInfo("系统启动", "日志文件路径: " + FileLogManager.getInstance().getCurrentLogFilePath());
            
            // 注意：不重定向 System.out/err，只记录系统日志和错误日志到文件
            System.out.println("[日志系统] 文件日志系统已启动，仅记录系统日志和错误日志");
            
        } catch (Exception e) {
            System.err.println("[日志系统] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 关闭日志系统
     */
    private static void shutdownLogging() {
        try {
            FileLogManager.getInstance().logInfo("系统关闭", "米多星球产线采集系统正在关闭...");
            
            // 停止文件日志管理器（等待队列中的日志写完）
            FileLogManager.getInstance().stop();
            
            System.out.println("✓ 日志系统已关闭");
        } catch (Exception e) {
            System.err.println("[日志系统] 关闭失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * JVM关闭钩子 - 确保资源清理
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n检测到系统关闭信号，正在清理资源...");
            shutdownBackend();
            shutdownLogging();
        }, "Shutdown-Hook-Thread"));
    }
}

