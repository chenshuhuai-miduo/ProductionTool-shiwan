package com.miduo.cloud.application.shiwan;

/**
 * 上传日志总线（发布-订阅解耦）。
 * <p>
 * 后端服务（ShiwanM2BoxCaseService）通过 {@link #log} 发布文字日志，
 * 通过 {@link #firePalletEvent} 发布结构化垛上传状态事件；<br>
 * 前端控制器在初始化时注册监听，避免 miduo-application 直接依赖 miduo-frontend。
 * </p>
 */
public final class UploadLogBus {

    /** 日志颜色（对应界面实时上传区的文字颜色） */
    public enum Color { GRAY, BLUE, GREEN, RED }

    /** 垛上传状态（用于实时上传数据区动态条目） */
    public enum PalletUploadStatus { UPLOADING, SUCCESS, FAILED }

    /** 文字日志监听器 */
    @FunctionalInterface
    public interface Listener {
        void onLog(String message, Color color);
    }

    /** 垛状态事件监听器 */
    @FunctionalInterface
    public interface PalletEventListener {
        void onPalletEvent(String palletCode, int boxCount, PalletUploadStatus status, String errorMsg);
    }

    private static volatile Listener listener;
    private static volatile PalletEventListener palletEventListener;
    private static final int MAX_LOG_HISTORY = 500;
    private static final java.util.Deque<LogRecord> logHistory = new java.util.ArrayDeque<>();

    private static final class LogRecord {
        private final String message;
        private final Color color;

        private LogRecord(String message, Color color) {
            this.message = message;
            this.color = color;
        }
    }

    private UploadLogBus() {}

    /** 注册文字日志监听器（前端初始化时调用） */
    public static void register(Listener l) {
        java.util.List<LogRecord> snapshot;
        synchronized (UploadLogBus.class) {
            listener = l;
            snapshot = new java.util.ArrayList<>(logHistory);
        }
        if (l != null) {
            // 按时间正序回放；前端列表通常按“头插”显示，最终效果仍为最新在上。
            for (LogRecord r : snapshot) {
                l.onLog(r.message, r.color);
            }
        }
    }

    /** 注册垛状态事件监听器（前端主界面初始化时调用） */
    public static void registerPalletEventListener(PalletEventListener l) {
        palletEventListener = l;
    }

    /** 发布一条文字上传日志（后端服务调用） */
    public static void log(String message, Color color) {
        Listener l;
        synchronized (UploadLogBus.class) {
            logHistory.addLast(new LogRecord(message, color));
            while (logHistory.size() > MAX_LOG_HISTORY) {
                logHistory.removeFirst();
            }
            l = listener;
        }
        if (l != null) {
            l.onLog(message, color);
        }
    }

    /**
     * 发布垛上传状态事件（后端服务调用）。
     * UPLOADING：成垛后开始上传；SUCCESS/FAILED：5分钟轮询返回结果后发布。
     */
    public static void firePalletEvent(String palletCode, int boxCount,
                                       PalletUploadStatus status, String errorMsg) {
        PalletEventListener l = palletEventListener;
        if (l != null) {
            l.onPalletEvent(palletCode, boxCount, status, errorMsg);
        }
    }
}
