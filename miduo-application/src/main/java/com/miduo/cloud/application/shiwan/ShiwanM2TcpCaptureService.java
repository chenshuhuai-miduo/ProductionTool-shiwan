package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.application.device.DeviceApplicationService;
import com.miduo.cloud.common.config.ShiwanM2SettingsDto;
import com.miduo.cloud.common.config.ShiwanM2SettingsFileLoader;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 石湾 2 号机 TCP 采集服务（批次匹配模式）。
 *
 * 工作原理：
 *   - 盒码相机每次扫描产生一个盒码批次（BatchNo 自增），放入盒码批次队列。
 *   - 箱码相机每次扫描产生一个箱码批次（BatchNo 自增），放入箱码批次队列。
 *   - 当两个队列头部的 BatchNo 相同时，触发盒箱关联。
 *   - 无论校验是否通过，所有码均进入批次队列；校验结果仅在数据接收区展示。
 *   - 关联时重新校验位数 + 检查 Status=4：有问题则整批剔除并写剔除记录表，全部通过则执行正常关联。
 *
 * 启用条件：shiwan.m2.tcp-capture.enabled=true
 */
@Service
@ConditionalOnProperty(name = "shiwan.m2.tcp-capture.enabled", havingValue = "true")
public class ShiwanM2TcpCaptureService {

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2TcpCaptureService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 盒码采集相机的 deviceCategory 文本 */
    private static final String BOX_CATEGORY  = "盒码采集";
    /** 箱码采集相机的 deviceCategory 文本 */
    private static final String CASE_CATEGORY = "箱码采集";
    /** 事件队列最大容量 */
    private static final int MAX_EVENTS    = 1000;
    /** 相机断线后重试间隔（ms） */
    private static final int RECONNECT_MS  = 3000;
    /** 批次超时巡检间隔（ms） */
    private static final int TIMEOUT_CHECK_INTERVAL_MS = 200;

    @Resource
    private ShiwanM2BoxCaseService boxCaseService;

    @Resource
    private DeviceApplicationService deviceApplicationService;

    // -------- 运行时状态 --------
    private volatile boolean active         = false;
    private volatile String  currentOrderNo;
    private volatile String  currentProductNo;
    private volatile int     boxesPerCase   = 4;
    private volatile int     boxesPerPallet = 70;

    /** 当前垛内已完成的箱数 */
    private final AtomicInteger currentCasesInPallet = new AtomicInteger(0);
    /** 已完成关联的总次数 */
    private final AtomicLong    totalAssociations    = new AtomicLong(0);

    /**
     * 盒码批次队列：盒码相机每次扫描 → BoxBatchEntry（含批号、所有码及各码位数校验状态）。
     */
    private final LinkedBlockingQueue<BoxBatchEntry>  boxBatchQueue  = new LinkedBlockingQueue<>();
    /**
     * 箱码批次队列：箱码相机每次扫描 → CaseBatchEntry（含批号、箱码及位数校验状态）。
     */
    private final LinkedBlockingQueue<CaseBatchEntry> caseBatchQueue = new LinkedBlockingQueue<>();

    /** 盒码批次计数器：每次盒码相机触发自增，从 1 开始 */
    private final AtomicInteger boxBatchCounter  = new AtomicInteger(0);
    /** 箱码批次计数器：每次箱码相机触发自增，从 1 开始；与盒码批次计数器顺序对应 */
    private final AtomicInteger caseBatchCounter = new AtomicInteger(0);

    /** 批次匹配锁：保证 peek+poll 操作的原子性 */
    private final Object matchLock = new Object();

    // -------- 事件列表（前端轮询） --------
    private final Deque<CaptureEvent> recentEvents = new ArrayDeque<>(MAX_EVENTS);
    private final AtomicLong          eventSeq     = new AtomicLong(0);
    private final Object              eventLock    = new Object();

    // -------- 采集线程 --------
    private Thread          boxReceiverThread;
    private Thread          caseReceiverThread;
    private volatile Socket boxSocket;
    private volatile Socket caseSocket;
    /** 批次超时巡检线程（用于“另一相机未在指定时间到达同批次”剔除） */
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "tcp-batch-timeout-checker");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> timeoutCheckFuture;

    // ================================================================
    //  对外接口
    // ================================================================

    /**
     * 启动 TCP 采集。
     * @return null 表示成功，否则返回错误说明。
     */
    public synchronized String start(String orderNo, String productNo, int boxesPerCase, int boxesPerPallet) {
        if (active) return "TCP采集已在运行中";

        IoDeviceDTO boxDev  = findDevice(BOX_CATEGORY);
        IoDeviceDTO caseDev = findDevice(CASE_CATEGORY);
        if (boxDev  == null) return "未找到盒码采集相机配置（设备类型=盒码采集），请在系统设置→IO设备管理中添加";
        if (caseDev == null) return "未找到箱码采集相机配置（设备类型=箱码采集），请在系统设置→IO设备管理中添加";
        if (Boolean.FALSE.equals(boxDev.getEnabled()))  return "盒码采集相机已禁用，请启用后重试";
        if (Boolean.FALSE.equals(caseDev.getEnabled())) return "箱码采集相机已禁用，请启用后重试";
        if (boxDev.getAddress()  == null || boxDev.getPort()  == null) return "盒码采集相机地址/端口未配置";
        if (caseDev.getAddress() == null || caseDev.getPort() == null) return "箱码采集相机地址/端口未配置";

        this.currentOrderNo   = orderNo;
        this.currentProductNo = productNo;
        this.boxesPerCase     = boxesPerCase  > 0 ? boxesPerCase  : 4;
        this.boxesPerPallet   = boxesPerPallet > 0 ? boxesPerPallet : 70;

        // 重置状态
        boxBatchQueue.clear();
        caseBatchQueue.clear();
        boxBatchCounter.set(0);
        caseBatchCounter.set(0);
        currentCasesInPallet.set(0);

        active = true;
        startTimeoutChecker();

        boxReceiverThread  = newDaemonThread(() -> receiveLoop(boxDev,  true),  "tcp-box-receiver");
        caseReceiverThread = newDaemonThread(() -> receiveLoop(caseDev, false), "tcp-case-receiver");
        boxReceiverThread.start();
        caseReceiverThread.start();

        addEvent("STARTED", "TCP采集已启动：订单=" + orderNo
                + "  每箱" + this.boxesPerCase + "盒  每垛" + this.boxesPerPallet + "箱", null);
        log.info("TCP采集启动 orderNo={} boxesPerCase={} boxesPerPallet={}", orderNo, this.boxesPerCase, this.boxesPerPallet);
        return null;
    }

    /** 停止 TCP 采集。 */
    public synchronized void stop() {
        active = false;
        closeSocket(boxSocket);
        closeSocket(caseSocket);
        boxSocket  = null;
        caseSocket = null;
        if (boxReceiverThread  != null) boxReceiverThread.interrupt();
        if (caseReceiverThread != null) caseReceiverThread.interrupt();
        stopTimeoutChecker();
        addEvent("STOPPED", "TCP采集已停止", null);
        log.info("TCP采集已停止");
    }

    /**
     * 批次模式下不支持从数据库恢复盒码队列（无法还原原始批次分组），返回 0。
     * 软件重启后如有未关联盒码，需重新扫描。
     */
    public int restoreBoxQueueFromDb(String orderNo) {
        log.info("[队列恢复] 批次匹配模式下不支持从数据库恢复盒码队列，请重新扫描（订单：{}）", orderNo);
        return 0;
    }

    /** 当前运行状态（供 Controller 查询）。 */
    public Map<String, Object> getStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active",               active);
        m.put("pendingBoxBatches",    boxBatchQueue.size());
        m.put("pendingCaseBatches",   caseBatchQueue.size());
        m.put("boxBatchCounter",      boxBatchCounter.get());
        m.put("caseBatchCounter",     caseBatchCounter.get());
        m.put("currentCasesInPallet", currentCasesInPallet.get());
        m.put("totalAssociations",    totalAssociations.get());
        m.put("boxesPerCase",         boxesPerCase);
        m.put("boxesPerPallet",       boxesPerPallet);
        return m;
    }

    /**
     * 获取 seq > lastSeq 的事件列表（前端增量轮询）。
     */
    public List<Map<String, Object>> getEvents(long lastSeq) {
        List<Map<String, Object>> result = new ArrayList<>();
        synchronized (eventLock) {
            for (CaptureEvent e : recentEvents) {
                if (e.seq > lastSeq) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("seq",     e.seq);
                    item.put("type",    e.type);
                    item.put("message", e.message);
                    item.put("time",    e.time);
                    item.put("timeMs",  e.timeMs);
                    if (e.data != null) item.put("data", e.data);
                    result.add(item);
                }
            }
        }
        return result;
    }

    public boolean isActive() { return active; }

    // ================================================================
    //  TCP 接收线程
    // ================================================================

    /**
     * TCP 接收循环：连接相机 → 原始字节读取 → 按 \n 拆包（无 \n 时立即触发）→ 调用 onCodeReceived。
     */
    private void receiveLoop(IoDeviceDTO device, boolean isBoxCamera) {
        String name    = device.getDeviceName();
        String address = device.getAddress().trim();
        int    port;
        try { port = Integer.parseInt(device.getPort().trim()); }
        catch (NumberFormatException ex) {
            addEvent(isBoxCamera ? "BOX_ERROR" : "CASE_ERROR",
                    name + " 端口号配置非法：" + device.getPort(), null);
            return;
        }

        while (active) {
            Socket sock = null;
            try {
                log.info("[{}] 正在连接 {}:{}", name, address, port);
                sock = new Socket();
                sock.setTcpNoDelay(true);
                sock.connect(new InetSocketAddress(address, port), 5000);
                if (isBoxCamera) boxSocket  = sock;
                else             caseSocket = sock;

                addEvent(isBoxCamera ? "BOX_CONNECTED" : "CASE_CONNECTED",
                        name + " 连接成功 (" + address + ":" + port + ")", null);
                log.info("[{}] 连接成功", name);

                InputStream   is      = sock.getInputStream();
                byte[]        buffer  = new byte[1024];
                StringBuilder msgBuf  = new StringBuilder();

                while (active) {
                    int n = is.read(buffer);
                    if (n == -1) break;

                    String  chunk      = new String(buffer, 0, n, StandardCharsets.UTF_8);
                    boolean hasNewline = chunk.contains("\n");

                    for (char c : chunk.toCharArray()) {
                        if (c == '\n') {
                            String code = msgBuf.toString().trim();
                            if (!code.isEmpty()) onCodeReceived(code, isBoxCamera);
                            msgBuf.setLength(0);
                        } else if (c != '\r') {
                            msgBuf.append(c);
                        }
                    }
                    if (!hasNewline && msgBuf.length() > 0) {
                        String code = msgBuf.toString().trim();
                        if (!code.isEmpty()) onCodeReceived(code, isBoxCamera);
                        msgBuf.setLength(0);
                    }
                    if (msgBuf.length() >= 1024) {
                        String code = msgBuf.toString().trim();
                        if (!code.isEmpty()) onCodeReceived(code, isBoxCamera);
                        msgBuf.setLength(0);
                    }
                }

                if (active) {
                    addEvent(isBoxCamera ? "BOX_DISCONNECTED" : "CASE_DISCONNECTED",
                            name + " 连接断开，" + RECONNECT_MS / 1000 + " 秒后重连", null);
                    log.warn("[{}] 连接断开，重连中", name);
                }
            } catch (Exception e) {
                if (!active) break;
                addEvent(isBoxCamera ? "BOX_DISCONNECTED" : "CASE_DISCONNECTED",
                        name + " 连接异常：" + e.getMessage() + "，" + RECONNECT_MS / 1000 + " 秒后重连", null);
                log.warn("[{}] 连接异常：{}，重连中", name, e.getMessage());
            } finally {
                closeSocket(sock);
                if (isBoxCamera) boxSocket  = null;
                else             caseSocket = null;
            }
            if (active) {
                try { Thread.sleep(RECONNECT_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.debug("[{}] 接收线程退出", name);
    }

    // ================================================================
    //  核心处理
    // ================================================================

    /**
     * 收到原始码数据后的处理入口。
     *
     * 盒码相机：
     *   1. 按 ";" 分隔、去重
     *   2. 对每个码做位数校验（仅展示结果，不阻止入队）
     *   3. 检查数量是否与 boxesPerCase 相符（仅展示结果）
     *   4. 整批封装为 BoxBatchEntry（含批号、各码校验状态）放入 boxBatchQueue
     *   5. 调用 tryMatchAndAssociate()
     *
     * 箱码相机：
     *   1. 对箱码做位数校验（仅展示结果）
     *   2. 封装为 CaseBatchEntry（含批号、校验状态）放入 caseBatchQueue
     *   3. 调用 tryMatchAndAssociate()
     */
    private void onCodeReceived(String rawCode, boolean isBoxCamera) {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        ShiwanM2SettingsDto.CodeDigitsConfig digitsCfg = cfg != null ? cfg.getCodeDigits() : null;
        int mediumDigits = digitsCfg != null ? digitsCfg.getMediumCodeDigits() : -1;
        int largeDigits  = digitsCfg != null ? digitsCfg.getLargeCodeDigits()  : -1;

        if (isBoxCamera) {
            // 盒码批次计数
            int batchNo = boxBatchCounter.incrementAndGet();

            // 去重（保留首次出现顺序）
            String[] parts = rawCode.split(";");
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String p : parts) { String c = p.trim(); if (!c.isEmpty()) seen.add(c); }
            List<String> uniqueCodes = new ArrayList<>(seen);

            // 位数校验
            Set<String> queuedBoxCodes = snapshotQueuedBoxCodes();
            List<BoxCodeItem> items = new ArrayList<>();
            List<String> digitFailCodes = new ArrayList<>();
            List<String> queueDuplicateCodes = new ArrayList<>();
            for (String code : uniqueCodes) {
                boolean digitOk = (mediumDigits <= 0) || (code.length() == mediumDigits);
                boolean queueDuplicate = queuedBoxCodes.contains(code);
                String  reason  = digitOk ? null
                        : "盒码位数不符（期望" + mediumDigits + "位，实际" + code.length() + "位）";
                if (digitOk && queueDuplicate) {
                    reason = "检测到采集的码重复";
                }
                if (!digitOk) digitFailCodes.add(code);
                if (queueDuplicate) queueDuplicateCodes.add(code);
                items.add(new BoxCodeItem(code, digitOk, queueDuplicate, reason));
            }

            // 数量校验
            boolean countOk = (uniqueCodes.size() == boxesPerCase);

            // 数据接收区展示
            addEvent("BOX_RECV",
                    "[盒码相机] 批" + batchNo + " 接收到 " + uniqueCodes.size() + " 个盒码：" + uniqueCodes, null);
            if (!digitFailCodes.isEmpty()) {
                addEvent("BOX_FAIL",
                        "[盒码相机] 批" + batchNo + " 位数不符（期望" + mediumDigits + "位），码：" + digitFailCodes, null);
            }
            if (!countOk) {
                String countReason = uniqueCodes.size() < boxesPerCase ? "缺码" : "超码";
                addEvent("BOX_FAIL",
                        "[盒码相机] 批" + batchNo + " " + countReason
                        + "（期望" + boxesPerCase + "个，实际" + uniqueCodes.size() + "个）", null);
            }
            if (!queueDuplicateCodes.isEmpty()) {
                addEvent("BOX_FAIL",
                        "[盒码相机] 批" + batchNo + " 检测到采集的码重复：" + queueDuplicateCodes, null);
            }

            boxBatchQueue.offer(new BoxBatchEntry(batchNo, items, countOk));
            log.debug("[盒码] 批{} 入队 codes={} countOk={}", batchNo, uniqueCodes, countOk);
            tryMatchAndAssociate();

        } else {
            // 箱码批次计数
            int    batchNo = caseBatchCounter.incrementAndGet();
            String code    = rawCode.trim();
            if (code.isEmpty()) return;

            // 位数校验
            boolean digitOk = (largeDigits <= 0) || (code.length() == largeDigits);
            boolean queueDuplicate = snapshotQueuedCaseCodes().contains(code);
            String  reason  = digitOk ? null
                    : "箱码位数不符（期望" + largeDigits + "位，实际" + code.length() + "位）";
            if (digitOk && queueDuplicate) {
                reason = "检测到采集的码重复";
            }

            // 数据接收区展示
            addEvent("CASE_RECV", "[箱码相机] 批" + batchNo + " 接收数据：" + code, null);
            if (!digitOk) {
                addEvent("BOX_FAIL", "[箱码相机] 批" + batchNo + " " + reason, null);
            }
            if (queueDuplicate) {
                addEvent("BOX_FAIL", "[箱码相机] 批" + batchNo + " 检测到采集的码重复：" + code, null);
            }

            caseBatchQueue.offer(new CaseBatchEntry(batchNo, code, digitOk, queueDuplicate, reason));
            log.debug("[箱码] 批{} 入队 code={} digitOk={} queueDuplicate={}",
                    batchNo, code, digitOk, queueDuplicate);
            tryMatchAndAssociate();
        }
    }

    /**
     * 尝试匹配并触发关联：
     * 当两个队列头部的 BatchNo 相同时，原子地弹出这两个批次，调用 processMatchedBatch。
     * 处理完成后递归尝试下一对（应对连续多批积压的情况）。
     */
    private void tryMatchAndAssociate() {
        BoxBatchEntry  boxBatch;
        CaseBatchEntry caseBatch;
        synchronized (matchLock) {
            // 先做“指定剔除延迟内未等到同批次”的超时剔除，再尝试正常批次匹配
            expireAndRejectTimeoutBatchesLocked();
            BoxBatchEntry  peekBox  = boxBatchQueue.peek();
            CaseBatchEntry peekCase = caseBatchQueue.peek();
            if (peekBox == null || peekCase == null) return;
            if (peekBox.batchNo != peekCase.batchNo) {
                log.debug("[批次匹配] 批号不一致：盒码队列头={}, 箱码队列头={}, 等待中",
                        peekBox.batchNo, peekCase.batchNo);
                return;
            }
            boxBatch  = boxBatchQueue.poll();
            caseBatch = caseBatchQueue.poll();
        }
        if (boxBatch == null || caseBatch == null) return;

        processMatchedBatch(boxBatch, caseBatch);

        // 递归：处理完当前批次后继续尝试下一批
        tryMatchAndAssociate();
    }

    /** 启动批次超时巡检。 */
    private synchronized void startTimeoutChecker() {
        stopTimeoutChecker();
        timeoutCheckFuture = timeoutScheduler.scheduleWithFixedDelay(() -> {
            if (!active) return;
            try {
                tryMatchAndAssociate();
            } catch (Exception e) {
                log.warn("[批次超时巡检] 执行异常: {}", e.getMessage());
            }
        }, TIMEOUT_CHECK_INTERVAL_MS, TIMEOUT_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** 停止批次超时巡检。 */
    private synchronized void stopTimeoutChecker() {
        if (timeoutCheckFuture != null) {
            timeoutCheckFuture.cancel(false);
            timeoutCheckFuture = null;
        }
    }

    /** 在批次匹配锁内执行超时剔除（仅剔除队列头，逐步推进）。 */
    private void expireAndRejectTimeoutBatchesLocked() {
        long timeoutMs = resolveRejectDelayMs();
        if (timeoutMs <= 0) return;
        long now = System.currentTimeMillis();

        while (true) {
            boolean handled = false;
            BoxBatchEntry boxHead = boxBatchQueue.peek();
            if (boxHead != null
                    && now - boxHead.receivedAtMs >= timeoutMs
                    && !containsCaseBatchNo(boxHead.batchNo)) {
                boxBatchQueue.poll();
                handleBoxBatchTimeoutReject(boxHead);
                handled = true;
            }
            if (handled) continue;

            CaseBatchEntry caseHead = caseBatchQueue.peek();
            if (caseHead != null
                    && now - caseHead.receivedAtMs >= timeoutMs
                    && !containsBoxBatchNo(caseHead.batchNo)) {
                caseBatchQueue.poll();
                handleCaseBatchTimeoutReject(caseHead);
                handled = true;
            }
            if (!handled) break;
        }
    }

    private boolean containsCaseBatchNo(int batchNo) {
        for (CaseBatchEntry entry : caseBatchQueue) {
            if (entry != null && entry.batchNo == batchNo) return true;
        }
        return false;
    }

    private boolean containsBoxBatchNo(int batchNo) {
        for (BoxBatchEntry entry : boxBatchQueue) {
            if (entry != null && entry.batchNo == batchNo) return true;
        }
        return false;
    }

    /** 盒码批次超时（未等到同批次箱码）→ 整批剔除。 */
    private void handleBoxBatchTimeoutReject(BoxBatchEntry boxBatch) {
        alignBatchCountersAfterTimeout(boxBatch.batchNo);
        String reason = "箱相机未在指定剔除延迟时间内采集到箱码，剔除整箱";
        List<String> boxCodes = boxBatch.codes == null
                ? Collections.emptyList()
                : boxBatch.codes.stream().map(i -> i.code).filter(Objects::nonNull).collect(Collectors.toList());

        // 落库：按盒码记录剔除条目（无箱码时 CaseCode 为空）
        for (String boxCode : boxCodes) {
            boxCaseService.insertRejectRecord(currentOrderNo, null, boxCode, null, boxCode, reason);
        }
        // 将该批次盒码在关联表中的未关联记录状态置为未完成（Status=0）
        if (!boxCodes.isEmpty()) {
            boxCaseService.markCodeRelationUploadUnfinishedByBoxCodes(boxCodes);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("boxCodes", boxCodes);
        data.put("caseCode", "");
        data.put("reason", reason);
        addEvent("ASSOC_FAIL", "[批" + boxBatch.batchNo + "] " + reason, data);
        log.warn("[批{}] 超时剔除：盒码批次未匹配到箱码，boxCodes={}", boxBatch.batchNo, boxCodes);
    }

    /** 箱码批次超时（未等到同批次盒码）→ 整批剔除。 */
    private void handleCaseBatchTimeoutReject(CaseBatchEntry caseBatch) {
        alignBatchCountersAfterTimeout(caseBatch.batchNo);
        String reason = "盒相机未在指定剔除延迟时间内采集到盒码，剔除整箱";
        String caseCode = caseBatch.code != null ? caseBatch.code : "";

        // 落库：按箱码记录剔除条目
        boxCaseService.insertRejectRecord(currentOrderNo, caseCode, null, null, caseCode, reason);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("boxCodes", Collections.emptyList());
        data.put("caseCode", caseCode);
        data.put("reason", reason);
        addEvent("ASSOC_FAIL", "[批" + caseBatch.batchNo + "] " + reason + "，箱码:" + caseCode, data);
        log.warn("[批{}] 超时剔除：箱码批次未匹配到盒码，caseCode={}", caseBatch.batchNo, caseCode);
    }

    /**
     * 任一侧批次超时剔除后，将盒/箱批次计数器统一推进到同一批次基线，
     * 避免后续一侧仍从旧批次号继续入队，导致长期错位。
     */
    private void alignBatchCountersAfterTimeout(int timeoutBatchNo) {
        int alignedBox = boxBatchCounter.updateAndGet(v -> Math.max(v, timeoutBatchNo));
        int alignedCase = caseBatchCounter.updateAndGet(v -> Math.max(v, timeoutBatchNo));
        log.info("[批次对齐] 超时批次={}，对齐后 boxBatchCounter={}, caseBatchCounter={}",
                timeoutBatchNo, alignedBox, alignedCase);
    }

    /** 读取系统设置中的“触发剔除延时（ms）”。 */
    private long resolveRejectDelayMs() {
        ShiwanM2SettingsDto cfg = ShiwanM2SettingsFileLoader.load();
        if (cfg == null || cfg.getAlarm() == null) return 0L;
        int ms = cfg.getAlarm().getRejectTriggerDelayMs();
        return Math.max(ms, 0);
    }

    /**
     * 处理已匹配的盒码批次与箱码批次。
     *
     * 校验顺序：
     *   1. 重新校验箱码位数
     *   2. 重新校验各盒码位数
     *   3. 校验盒码数量是否与 boxesPerCase 相符
     *   4. 检查位数通过的盒码在 CodeRelationUpload 中是否有 Status=4 记录
     *
     * 若步骤 1-4 中有任何问题 → 整批剔除：
     *   - 将位数不符 / Status=4 的码写入 RejectRecord（箱码问题写 CaseCode，盒码问题写 BoxCode+BottleCode）
     *   - 从 CodeRelationUpload 硬删除该批次所有盒码的未关联记录
     *   - 发出 ASSOC_FAIL 事件
     *
     * 若全部通过 → 调用 associateCaseCodeWithBoxCodes 正常关联：
     *   - 关联成功 → ASSOCIATED 事件
     *   - 关联失败 → 写剔除记录 + 删除 CodeRelationUpload + ASSOC_FAIL 事件
     */
    private void processMatchedBatch(BoxBatchEntry boxBatch, CaseBatchEntry caseBatch) {
        int    batchNo  = boxBatch.batchNo;
        String caseCode = caseBatch.code;
        List<BoxCodeItem> boxItems    = boxBatch.codes;
        List<String>      allBoxCodes = boxItems.stream().map(i -> i.code).collect(Collectors.toList());

        // 有问题的条目（写 RejectRecord 用）
        List<RejectEntry> rejectEntries = new ArrayList<>();

        // 1. 箱码位数校验
        if (!caseBatch.digitOk || caseBatch.queueDuplicate) {
            rejectEntries.add(new RejectEntry(caseCode, null, caseCode, caseBatch.reason));
        }

        // 2. 盒码位数校验
        for (BoxCodeItem item : boxItems) {
            if (!item.digitOk || item.queueDuplicate) {
                rejectEntries.add(new RejectEntry(caseCode, item.code, item.code, item.reason));
            }
        }

        // 3. 数量校验
        boolean countOk = boxBatch.countOk;

        // 4. Status=4 检查（仅对位数通过的盒码）
        List<String> digitOkBoxCodes = boxItems.stream()
                .filter(i -> i.digitOk).map(i -> i.code).collect(Collectors.toList());
        List<String> status4Codes = Collections.emptyList();
        if (!digitOkBoxCodes.isEmpty()) {
            status4Codes = boxCaseService.getStatus4BoxCodes(digitOkBoxCodes);
        }

        boolean hasProblems = !rejectEntries.isEmpty() || !countOk || !status4Codes.isEmpty();

        if (hasProblems) {
            // --- 整批剔除 ---

            // 写剔除记录：位数不符条目
            for (RejectEntry re : rejectEntries) {
                boxCaseService.insertRejectRecord(
                        currentOrderNo, re.caseCode, re.boxCode, null, re.problemCode, re.reason);
            }

            // 写剔除记录：Status=4 条目（含瓶码）
            for (String status4Box : status4Codes) {
                boxCaseService.insertStatus4RejectRecordsForBox(currentOrderNo, caseCode, status4Box);
            }

            // 从 CodeRelationUpload 删除该批次所有盒码的未关联记录
            if (!allBoxCodes.isEmpty()) {
                int deleted = boxCaseService.deleteCodeRelationUploadByBoxCodes(allBoxCodes);
                log.warn("[批{}] 整批剔除：从 CodeRelationUpload 删除 {} 条记录", batchNo, deleted);
            }

            // 构造问题摘要
            StringBuilder summary = new StringBuilder();
            if (!rejectEntries.isEmpty()) {
                long digitFailBox  = rejectEntries.stream().filter(e -> e.boxCode != null).count();
                boolean caseDigitFail = rejectEntries.stream().anyMatch(e -> e.boxCode == null);
                long queueDupFail = rejectEntries.stream().filter(e -> "检测到采集的码重复".equals(e.reason)).count();
                if (caseDigitFail && "检测到采集的码重复".equals(caseBatch.reason)) {
                    summary.append("检测到采集的码重复；");
                } else if (caseDigitFail) {
                    summary.append("箱码位数不符；");
                }
                if (digitFailBox > 0) {
                    summary.append("盒码异常×").append(digitFailBox).append("；");
                }
                if (queueDupFail > 0 && !(caseDigitFail && "检测到采集的码重复".equals(caseBatch.reason))) {
                    summary.append("检测到采集的码重复×").append(queueDupFail).append("；");
                }
            }
            if (!countOk) {
                String cr = boxBatch.codes.size() < boxesPerCase ? "缺码" : "超码";
                summary.append("盒码").append(cr).append("（实际").append(boxBatch.codes.size())
                        .append("个，期望").append(boxesPerCase).append("个）；");
            }
            if (!status4Codes.isEmpty()) {
                summary.append("Status=4×").append(status4Codes.size()).append("；");
            }

            addEvent("ASSOC_FAIL",
                    "[批" + batchNo + "] 整批剔除 箱码:" + caseCode + "，原因：" + summary,
                    buildFailData(allBoxCodes, caseCode, summary.toString()));
            log.warn("[批{}] 整批剔除 caseCode={} summary={}", batchNo, caseCode, summary);
            return;
        }

        // --- 全部通过，执行正常关联 ---
        ApiResult<Map<String, Object>> res = boxCaseService.associateCaseCodeWithBoxCodes(
                currentOrderNo, currentProductNo, caseCode, digitOkBoxCodes, boxesPerPallet);

        if (res == null || res.getCode() == null || res.getCode() != 200) {
            String errMsg = res != null ? res.getMessage() : "服务返回null";
            // 关联失败：写剔除记录 + 删除 CodeRelationUpload 记录
            String caseRejectReason = parseCaseRejectReason(errMsg);
            if ("大标不通过".equals(caseRejectReason) || "重码".equals(caseRejectReason)) {
                boxCaseService.insertRejectRecord(
                        currentOrderNo, caseCode, null, null, caseCode, caseRejectReason);
            } else {
                for (String boxCode : digitOkBoxCodes) {
                    boxCaseService.insertRejectRecord(
                            currentOrderNo, caseCode, boxCode, null, null, "盒箱校验不通过");
                }
            }
            if (!digitOkBoxCodes.isEmpty()) {
                boxCaseService.deleteCodeRelationUploadByBoxCodes(digitOkBoxCodes);
            }
            addEvent("ASSOC_FAIL",
                    "[批" + batchNo + "] 关联失败 箱码:" + caseCode + "，原因：" + errMsg,
                    buildFailData(allBoxCodes, caseCode, errMsg));
            log.warn("[批{}] 关联失败 caseCode={} err={}", batchNo, caseCode, errMsg);
            return;
        }

        // 关联成功
        Map<String, Object> data     = res.getData();
        long    assocNo    = totalAssociations.incrementAndGet();
        int     cases      = data != null && data.get("currentCaseCount") instanceof Number
                ? ((Number) data.get("currentCaseCount")).intValue() : currentCasesInPallet.get() + 1;
        boolean fullPallet = data != null && Boolean.TRUE.equals(data.get("fullPallet"));
        String  palletCode = data != null ? (String) data.get("palletCode") : null;

        if (fullPallet) currentCasesInPallet.set(0);
        else            currentCasesInPallet.set(cases);

        Map<String, Object> evtData = new LinkedHashMap<>();
        evtData.put("boxCodes",          digitOkBoxCodes);
        evtData.put("caseCode",          caseCode);
        evtData.put("currentCaseCount",  cases);
        evtData.put("fullPallet",        fullPallet);
        if (palletCode != null) evtData.put("palletCode", palletCode);

        addEvent("ASSOCIATED",
                "[批" + batchNo + "] 关联成功 #" + assocNo
                        + " 箱码:" + caseCode + " 盒码:[" + String.join(",", digitOkBoxCodes) + "]",
                evtData);
        log.info("[批{}] 关联成功 #{} caseCode={} boxCodes={} cases={} fullPallet={}",
                batchNo, assocNo, caseCode, digitOkBoxCodes, cases, fullPallet);
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private IoDeviceDTO findDevice(String category) {
        try {
            List<IoDeviceDTO> all = deviceApplicationService.getAllDevices();
            if (all == null) return null;
            for (IoDeviceDTO d : all) {
                if (category.equals(d.getDeviceCategory())) return d;
            }
        } catch (Exception e) {
            log.warn("查找设备失败 category={}: {}", category, e.getMessage());
        }
        return null;
    }

    private void addEvent(String type, String message, Map<String, Object> data) {
        long seq = eventSeq.incrementAndGet();
        long nowMs = System.currentTimeMillis();
        CaptureEvent evt = new CaptureEvent(seq, type, message,
                LocalDateTime.now().format(TIME_FMT), nowMs, data);
        synchronized (eventLock) {
            if (recentEvents.size() >= MAX_EVENTS) recentEvents.pollFirst();
            recentEvents.addLast(evt);
        }
    }

    /**
     * 从 ASSOC_FAIL 错误信息推断箱码级剔除原因。
     */
    private static String parseCaseRejectReason(String errMsg) {
        if (errMsg == null) return "盒箱校验不通过";
        if (errMsg.contains("大标码包")) return "大标不通过";
        if (errMsg.contains("重码"))     return "重码";
        return "盒箱校验不通过";
    }

    private static Map<String, Object> buildFailData(List<String> boxCodes, String caseCode, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("boxCodes", boxCodes);
        m.put("caseCode", caseCode);
        m.put("reason",   reason);
        return m;
    }

    /** 快照当前内存盒码队列中的所有盒码，用于重复校验。 */
    private Set<String> snapshotQueuedBoxCodes() {
        Set<String> codes = new HashSet<>();
        for (BoxBatchEntry entry : boxBatchQueue) {
            if (entry == null || entry.codes == null) continue;
            for (BoxCodeItem item : entry.codes) {
                if (item != null && item.code != null && !item.code.isEmpty()) {
                    codes.add(item.code);
                }
            }
        }
        return codes;
    }

    /** 快照当前内存箱码队列中的所有箱码，用于重复校验。 */
    private Set<String> snapshotQueuedCaseCodes() {
        Set<String> codes = new HashSet<>();
        for (CaseBatchEntry entry : caseBatchQueue) {
            if (entry != null && entry.code != null && !entry.code.isEmpty()) {
                codes.add(entry.code);
            }
        }
        return codes;
    }

    private static void closeSocket(Socket sock) {
        if (sock != null && !sock.isClosed()) {
            try { sock.close(); } catch (Exception ignored) {}
        }
    }

    private static Thread newDaemonThread(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    // ================================================================
    //  内部数据类
    // ================================================================

    /** 盒码批次：含批号、该批次所有盒码及各码校验状态、数量是否符合规格。 */
    private static class BoxBatchEntry {
        final int            batchNo;
        final List<BoxCodeItem> codes;
        /** 该批次盒码数量是否与 boxesPerCase 相符 */
        final boolean        countOk;
        /** 批次入队时间（ms） */
        final long           receivedAtMs;

        BoxBatchEntry(int batchNo, List<BoxCodeItem> codes, boolean countOk) {
            this.batchNo  = batchNo;
            this.codes    = codes;
            this.countOk  = countOk;
            this.receivedAtMs = System.currentTimeMillis();
        }
    }

    /** 盒码批次中单个盒码的信息。 */
    private static class BoxCodeItem {
        final String  code;
        /** 位数校验是否通过 */
        final boolean digitOk;
        /** 是否与内存队列中的盒码重复 */
        final boolean queueDuplicate;
        /** 若不通过，记录原因（用于展示和写入 RejectRecord）*/
        final String  reason;

        BoxCodeItem(String code, boolean digitOk, boolean queueDuplicate, String reason) {
            this.code    = code;
            this.digitOk = digitOk;
            this.queueDuplicate = queueDuplicate;
            this.reason  = reason;
        }
    }

    /** 箱码批次：含批号、箱码及位数校验状态。 */
    private static class CaseBatchEntry {
        final int     batchNo;
        final String  code;
        /** 位数校验是否通过 */
        final boolean digitOk;
        /** 是否与内存队列中的箱码重复 */
        final boolean queueDuplicate;
        final String  reason;
        /** 批次入队时间（ms） */
        final long    receivedAtMs;

        CaseBatchEntry(int batchNo, String code, boolean digitOk, boolean queueDuplicate, String reason) {
            this.batchNo  = batchNo;
            this.code     = code;
            this.digitOk  = digitOk;
            this.queueDuplicate = queueDuplicate;
            this.reason   = reason;
            this.receivedAtMs = System.currentTimeMillis();
        }
    }

    /** 剔除记录条目（仅在 processMatchedBatch 内部使用）。 */
    private static class RejectEntry {
        final String caseCode;
        final String boxCode;       // null 表示是箱码本身的问题
        final String problemCode;
        final String reason;

        RejectEntry(String caseCode, String boxCode, String problemCode, String reason) {
            this.caseCode    = caseCode;
            this.boxCode     = boxCode;
            this.problemCode = problemCode;
            this.reason      = reason;
        }
    }

    /** 事件记录（前端轮询用）。 */
    private static class CaptureEvent {
        final long               seq;
        final String             type;
        final String             message;
        final String             time;
        final long               timeMs;
        final Map<String, Object> data;

        CaptureEvent(long seq, String type, String message, String time, long timeMs, Map<String, Object> data) {
            this.seq     = seq;
            this.type    = type;
            this.message = message;
            this.time    = time;
            this.timeMs  = timeMs;
            this.data    = data;
        }
    }
}
