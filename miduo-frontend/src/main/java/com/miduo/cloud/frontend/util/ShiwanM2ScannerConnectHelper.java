package com.miduo.cloud.frontend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import com.miduo.cloud.frontend.service.DeviceConnectionManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 石湾 2 号机：已启用「扫码枪」设备的拉列表、尝试 {@link DeviceConnectionManager#startConnection} 与短时轮询。
 * <p>
 * 供手工采集「开始采集」、主界面 Tab 切换、提取工单未成垛弹窗等场景复用，避免热插拔后必须到系统设置里手动点连接。
 * </p>
 */
public final class ShiwanM2ScannerConnectHelper {

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2ScannerConnectHelper.class);

    /** 避免多路异步同时 startConnection 竞态 */
    private static final Object LOCK = new Object();

    private ShiwanM2ScannerConnectHelper() {}

    /**
     * 后台拉取设备列表并对未连接扫码枪重连；完成后在 JavaFX 线程回调（无返回值需求时用 {@link #tryReconnectScannersAsync()}）。
     */
    public static void fetchListAndEnsureScannersAsync(Consumer<Boolean> onFxThread) {
        CompletableFuture.runAsync(() -> {
            boolean ready;
            synchronized (LOCK) {
                ready = fetchListAndEnsureScannersBlocking();
            }
            final boolean f = ready;
            Platform.runLater(() -> onFxThread.accept(f));
        });
    }

    /** 与 {@link #fetchListAndEnsureScannersAsync} 相同，但不关心结果（静默尝试重连）。 */
    public static void tryReconnectScannersAsync() {
        fetchListAndEnsureScannersAsync(ignored -> {});
    }

    /**
     * 阻塞调用：仅应在后台线程执行（内部含 {@link HttpUtil#doGet} 与 {@link Thread#sleep}）。
     *
     * @return 无启用扫码枪为 true；有启用扫码枪时为是否全部已连接
     */
    public static boolean fetchListAndEnsureScannersBlocking() {
        try {
            String responseJson = HttpUtil.doGet("/api/device/list");
            ApiResult<List<IoDeviceDTO>> result = HttpUtil.parseJson(
                    responseJson, new TypeReference<ApiResult<List<IoDeviceDTO>>>() {});
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                return false;
            }
            List<IoDeviceDTO> scanners = new ArrayList<>();
            for (IoDeviceDTO d : result.getData()) {
                if (Boolean.TRUE.equals(d.getEnabled()) && "扫码枪".equals(d.getDeviceCategory())) {
                    scanners.add(d);
                }
            }
            return ensureScannersConnectedWithRetry(scanners);
        } catch (Exception e) {
            log.warn("[扫码枪重连] 拉列表或连接失败: {}", e.getMessage());
            return false;
        }
    }

    static boolean ensureScannersConnectedWithRetry(List<IoDeviceDTO> scanners) {
        if (scanners == null || scanners.isEmpty()) {
            return true;
        }
        DeviceConnectionManager mgr = DeviceConnectionManager.getInstance();
        for (IoDeviceDTO d : scanners) {
            if (mgr.isConnected(d.getId())) {
                continue;
            }
            try {
                mgr.startConnection(d);
            } catch (Exception e) {
                log.warn("[扫码枪重连] 连接尝试失败 {}: {}", d.getDeviceName(), e.getMessage());
            }
        }
        // 若尝试后仍无一台在线（典型：串口打不开），短等即可，避免空转满 6 秒拖慢「开始采集」等交互
        boolean anyConnected = false;
        for (IoDeviceDTO d : scanners) {
            if (mgr.isConnected(d.getId())) {
                anyConnected = true;
                break;
            }
        }
        long maxWaitMs = anyConnected ? 6000 : 1200;
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            boolean allOk = true;
            for (IoDeviceDTO d : scanners) {
                if (!mgr.isConnected(d.getId())) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) {
                return true;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        for (IoDeviceDTO d : scanners) {
            if (!mgr.isConnected(d.getId())) {
                return false;
            }
        }
        return true;
    }
}
