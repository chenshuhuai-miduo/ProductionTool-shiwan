package com.miduo.cloud.frontend.util;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JavaFX 开发热重载工具（CSS + FXML）。
 * <p>
 * 仅在开发环境使用（{@code -Ddev.mode=true} 或自动检测源码目录存在时启用）。
 * <ul>
 *   <li>F5 — 刷新 CSS（保留界面状态）</li>
 *   <li>Ctrl+F5 — 重载 FXML + CSS（界面完全重建，状态重置）</li>
 *   <li>自动监听 CSS 文件变更，保存后即刷新</li>
 *   <li>自动监听 FXML 文件变更，保存后即重载</li>
 * </ul>
 */
public class CssHotReloader {

    private static final boolean DEV_MODE;
    private static final Path RESOURCES_DIR;
    private static final Path CSS_SOURCE_DIR;
    private static final Path FXML_SOURCE_DIR;

    static {
        Path detected = detectResourcesDir();
        RESOURCES_DIR = detected;
        CSS_SOURCE_DIR = detected != null ? detected.resolve("css") : null;
        FXML_SOURCE_DIR = detected != null ? detected.resolve("fxml") : null;
        String devFlag = System.getProperty("dev.mode", "false");
        DEV_MODE = "true".equalsIgnoreCase(devFlag)
                || (CSS_SOURCE_DIR != null && Files.isDirectory(CSS_SOURCE_DIR));
    }

    private final Scene scene;
    private final String fxmlResourcePath;
    private List<String> rootFileUrls;
    private List<String> sceneFileUrls;
    private Thread cssWatchThread;
    private Thread fxmlWatchThread;
    private volatile boolean running;

    /**
     * @param scene            当前 Scene
     * @param fxmlResourcePath FXML classpath 路径，如 "/fxml/ShiwanM2MainWindow.fxml"
     */
    public CssHotReloader(Scene scene, String fxmlResourcePath) {
        this.scene = scene;
        this.fxmlResourcePath = fxmlResourcePath;
    }

    /** 兼容旧接口（无 FXML 重载） */
    public CssHotReloader(Scene scene) {
        this(scene, null);
    }

    public static boolean isDevMode() {
        return DEV_MODE;
    }

    public void start() {
        if (!DEV_MODE || CSS_SOURCE_DIR == null) {
            System.out.println("[HotReloader] 非开发模式或源码目录不存在，跳过");
            return;
        }

        System.out.println("[HotReloader] ============================");
        System.out.println("[HotReloader] 开发模式已启用！");
        System.out.println("[HotReloader] CSS  源码: " + CSS_SOURCE_DIR);
        System.out.println("[HotReloader] FXML 源码: " + FXML_SOURCE_DIR);

        Parent root = scene.getRoot();
        System.out.println("[HotReloader] Root stylesheets: " + root.getStylesheets().size() + " 个");

        rootFileUrls = toFileUrls(root.getStylesheets());
        sceneFileUrls = toFileUrls(scene.getStylesheets());
        applyFileUrls();

        registerKeyboardShortcuts();
        startCssWatcher();
        startFxmlWatcher();

        System.out.println("[HotReloader] 热重载已就绪");
        System.out.println("[HotReloader]   F5      → 刷新 CSS（保留状态）");
        System.out.println("[HotReloader]   Ctrl+F5 → 重载 FXML（完全重建）");
        System.out.println("[HotReloader]   自动监听文件变更");
        System.out.println("[HotReloader] ============================");
    }

    public void stop() {
        running = false;
        if (cssWatchThread != null) cssWatchThread.interrupt();
        if (fxmlWatchThread != null) fxmlWatchThread.interrupt();
    }

    // ==================== CSS 热重载 ====================

    private List<String> toFileUrls(ObservableList<String> sheets) {
        List<String> result = new ArrayList<>();
        for (String sheet : sheets) {
            String fileName = extractFileName(sheet);
            if (fileName != null) {
                Path filePath = CSS_SOURCE_DIR.resolve(fileName);
                if (Files.exists(filePath)) {
                    result.add(filePath.toUri().toString());
                    System.out.println("[HotReloader] CSS 映射: " + fileName);
                } else {
                    result.add(sheet);
                }
            } else {
                result.add(sheet);
            }
        }
        return result;
    }

    private void applyFileUrls() {
        Parent root = scene.getRoot();
        if (!rootFileUrls.isEmpty()) {
            root.getStylesheets().setAll(rootFileUrls);
        }
        if (!sceneFileUrls.isEmpty()) {
            scene.getStylesheets().setAll(sceneFileUrls);
        }
    }

    private void reloadCss() {
        Platform.runLater(() -> {
            Parent root = scene.getRoot();
            root.getStylesheets().clear();
            scene.getStylesheets().clear();
            root.applyCss();

            Platform.runLater(() -> {
                applyFileUrls();
                root.applyCss();
                System.out.println("[HotReloader] ✓ CSS 已刷新");
            });
        });
    }

    // ==================== FXML 热重载 ====================

    private void reloadFxml() {
        if (fxmlResourcePath == null || FXML_SOURCE_DIR == null) {
            System.out.println("[HotReloader] 未配置 FXML 路径，无法重载");
            return;
        }

        Platform.runLater(() -> {
            try {
                String fxmlFileName = extractFileName(fxmlResourcePath);
                Path fxmlPath = FXML_SOURCE_DIR.resolve(fxmlFileName);

                if (!Files.exists(fxmlPath)) {
                    System.err.println("[HotReloader] FXML 源文件不存在: " + fxmlPath);
                    return;
                }

                URL fxmlUrl = fxmlPath.toUri().toURL();
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent newRoot = loader.load();

                scene.setRoot(newRoot);

                // FXML 从源码目录加载时，@../css/xxx.css 会自动指向源码 CSS
                // 无需再手动替换 CSS URL
                newRoot.applyCss();
                newRoot.layout();

                System.out.println("[HotReloader] ✓ FXML 已重载: " + fxmlFileName);
                System.out.println("[HotReloader]   注：控制器已重新初始化，界面状态已重置");

            } catch (Exception e) {
                System.err.println("[HotReloader] ✗ FXML 重载失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ==================== 快捷键 ====================

    private void registerKeyboardShortcuts() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F5) {
                event.consume();
                if (event.isControlDown()) {
                    System.out.println("[HotReloader] Ctrl+F5 → 重载 FXML");
                    reloadFxml();
                } else {
                    System.out.println("[HotReloader] F5 → 刷新 CSS");
                    reloadCss();
                }
            }
        });
    }

    // ==================== 文件监听 ====================

    private void startCssWatcher() {
        if (!Files.isDirectory(CSS_SOURCE_DIR)) return;
        running = true;
        cssWatchThread = createWatcher(CSS_SOURCE_DIR, ".css", () -> {
            System.out.println("[HotReloader] CSS 文件变更 → 自动刷新");
            reloadCss();
        });
        cssWatchThread.start();
    }

    private void startFxmlWatcher() {
        if (fxmlResourcePath == null || !Files.isDirectory(FXML_SOURCE_DIR)) return;
        fxmlWatchThread = createWatcher(FXML_SOURCE_DIR, ".fxml", () -> {
            System.out.println("[HotReloader] FXML 文件变更 → 自动重载");
            reloadFxml();
        });
        fxmlWatchThread.start();
    }

    private Thread createWatcher(Path dir, String extension, Runnable onChanged) {
        Thread t = new Thread(() -> {
            try (WatchService ws = FileSystems.getDefault().newWatchService()) {
                dir.register(ws,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_CREATE);

                while (running) {
                    WatchKey key = ws.take();
                    boolean changed = false;

                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path p = (Path) event.context();
                        if (p != null && p.toString().endsWith(extension)) {
                            changed = true;
                            System.out.println("[HotReloader] 检测到变更: " + p);
                        }
                    }

                    if (changed) {
                        Thread.sleep(300);
                        onChanged.run();
                    }

                    if (!key.reset()) break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.err.println("[HotReloader] 监听异常 (" + dir + "): " + e.getMessage());
            }
        }, "HotReloader-" + extension);
        t.setDaemon(true);
        return t;
    }

    // ==================== 工具方法 ====================

    private static String extractFileName(String url) {
        if (url == null || url.isEmpty()) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return url;
    }

    private static Path detectResourcesDir() {
        try {
            Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            for (int i = 0; i < 5; i++) {
                Path candidate = cwd.resolve("miduo-frontend/src/main/resources");
                if (Files.isDirectory(candidate.resolve("css"))
                        && Files.isDirectory(candidate.resolve("fxml"))) {
                    return candidate.toAbsolutePath().normalize();
                }
                cwd = cwd.getParent();
                if (cwd == null) break;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
