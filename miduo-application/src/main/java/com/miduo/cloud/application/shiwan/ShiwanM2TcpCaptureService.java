package com.miduo.cloud.application.shiwan;

import com.miduo.cloud.application.device.DeviceApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 石湾 2 号机 TCP 采集服务。
 * 盒码收到即落库（CodeRelationUpload 一条，BigSerialNumber 为空）；箱码收到时查当前未关联的前 N 条盒码，
 * 够 N 条则更新其 BigSerialNumber，不足则箱码入待处理列表，待后续盒码落库后再尝试关联。
 * 启用条件：shiwan.m2.tcp-capture.enabled=true
 */
@Service
@ConditionalOnProperty(name = "shiwan.m2.tcp-capture.enabled", havingValue = "true")
public class ShiwanM2TcpCaptureService {

    private static final Logger log = LoggerFactory.getLogger(ShiwanM2TcpCaptureService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** 盒码采集相机的 deviceCategory 文本（INI 中存为数字 2，读出后转为此文本） */
    private static final String BOX_CATEGORY  = "盒码采集";
    /** 箱码采集相机的 deviceCategory 文本（INI 中存为数字 3） */
    private static final String CASE_CATEGORY = "箱码采集";
    /** 每个盒码在 CodeRelationUpload 中需满足的条数 */
    private static final int REQUIRED_ROWS_PER_BOX = 6;
    /** 事件队列最大容量 */
    private static final int MAX_EVENTS = 1000;
    /** 相机断线后重试间隔（ms） */
    private static final int RECONNECT_MS = 3000;

    @Resource
    private ShiwanM2BoxCaseService boxCaseService;

    @Resource
    private DeviceApplicationService deviceApplicationService;

    @Resource
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    // -------- 运行时状态 --------
    private volatile boolean active = false;
    private volatile String  currentOrderNo;
    private volatile String  currentProductNo;
    private volatile int     boxesPerCase    = 4;
    private volatile int     boxesPerPallet  = 70;

    /** 当前垛内已完成的箱数（从最后一次关联返回值更新） */
    private final AtomicInteger currentCasesInPallet = new AtomicInteger(0);
    /** 已完成关联的总次数（供前端检测是否有新关联） */
    private final AtomicLong    totalAssociations    = new AtomicLong(0);

    /** 已通过校验的盒码内存队列（校验条件：表中该 MediumSerialNumber 对应 6 条、SmallSerialNumber 互异、BigSerialNumber 均为空） */
    private final LinkedBlockingQueue<String> boxCodeQueue = new LinkedBlockingQueue<>();
    /** 待处理箱码列表：收到箱码时若内存队列中盒码不足 N 条则放入此队列 */
    private final LinkedBlockingQueue<String> pendingCaseCodes = new LinkedBlockingQueue<>();
    /**
     * BOX_FAIL 暂存队列：盒码校验失败后，箱码尚未到来，无法写剔除记录（需要箱码必填）。
     * 在下一个 ASSOC_FAIL 时一并排尽并写入 RejectRecord；ASSOCIATED 成功时清空（无需写记录）。
     */
    private final LinkedBlockingQueue<FailedBoxEntry> pendingFailedBoxes = new LinkedBlockingQueue<>();

    // -------- 事件列表（前端轮询） --------
    private final Deque<CaptureEvent>  recentEvents = new ArrayDeque<>(MAX_EVENTS);
    private final AtomicLong           eventSeq     = new AtomicLong(0);
    private final Object               eventLock    = new Object();

    // -------- 采集线程 --------
    private Thread boxReceiverThread;
    private Thread caseReceiverThread;
    /** 保存 socket 引用用于 stop 时强制关闭 */
    private volatile Socket boxSocket;
    private volatile Socket caseSocket;

    // ================================================================
    //  对外接口
    // ================================================================

    /**
     * 启动 TCP 采集。
     * @return null 表示成功，否则返回错误说明。
     */
    public synchronized String start(String orderNo, String productNo, int boxesPerCase, int boxesPerPallet) {
        if (active) {
            return "TCP采集已在运行中";
        }

        // 查找设备配置
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
        boxCodeQueue.clear();
        pendingCaseCodes.clear();
        pendingFailedBoxes.clear();
        currentCasesInPallet.set(0);

        active = true;

        // 启动守护线程（盒码/箱码收到即落库或触发关联，不再使用独立匹配线程）
        boxReceiverThread  = newDaemonThread(() -> receiveLoop(boxDev,  true),  "tcp-box-receiver");
        caseReceiverThread = newDaemonThread(() -> receiveLoop(caseDev, false), "tcp-case-receiver");
        boxReceiverThread.start();
        caseReceiverThread.start();

        addEvent("STARTED", "TCP采集已启动：订单=" + orderNo
                + "  每箱" + this.boxesPerCase + "盒  每垛" + this.boxesPerPallet + "箱", null);
        log.info("TCP采集启动 orderNo={} boxesPerCase={} boxesPerPallet={}", orderNo, this.boxesPerCase, this.boxesPerPallet);
        return null;
    }

    /** 停止 TCP 采集（关闭 Socket → receiver 线程自动退出）。 */
    public synchronized void stop() {
        active = false;
        closeSocket(boxSocket);
        closeSocket(caseSocket);
        boxSocket  = null;
        caseSocket = null;
        if (boxReceiverThread  != null) boxReceiverThread.interrupt();
        if (caseReceiverThread != null) caseReceiverThread.interrupt();
        addEvent("STOPPED", "TCP采集已停止", null);
        log.info("TCP采集已停止");
    }

    /**
     * 从数据库恢复未关联盒码到内存队列（用于软件重启后继续采集）。
     * 仅在采集服务启动后（active=true）调用才有效。
     * 查询条件：OrderNo=? AND Status=0 AND BigSerialNumber='' AND VirtualSerialNumber='' AND IsDel=0
     * @return 恢复的盒码数量
     */
    public int restoreBoxQueueFromDb(String orderNo) {
        if (orderNo == null || orderNo.trim().isEmpty()) return 0;
        try {
            List<String> pending = jdbcTemplate().queryForList(
                    "SELECT DISTINCT MediumSerialNumber FROM CodeRelationUpload " +
                    "WHERE OrderNo = ? AND IsDel = 0 AND Status = 0 " +
                    "AND (VirtualSerialNumber IS NULL OR VirtualSerialNumber = '') " +
                    "AND (BigSerialNumber IS NULL OR BigSerialNumber = '') " +
                    "ORDER BY MIN(ID) ASC",
                    String.class, orderNo.trim());
            int count = 0;
            for (String code : pending) {
                if (code != null && !code.isEmpty()) {
                    boxCodeQueue.offer(code);
                    count++;
                }
            }
            if (count > 0) {
                log.info("[队列恢复] 从数据库恢复 {} 个盒码到内存队列，订单号：{}", count, orderNo);
            }
            return count;
        } catch (Exception e) {
            log.warn("[队列恢复] 恢复盒码队列失败：{}", e.getMessage());
            return 0;
        }
    }

    /** 当前运行状态（供 Controller 查询）。 */
    public Map<String, Object> getStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active",               active);
        m.put("pendingBoxCodes",      boxCodeQueue.size());
        m.put("pendingCaseCodes",     pendingCaseCodes.size());
        m.put("currentCasesInPallet", currentCasesInPallet.get());
        m.put("totalAssociations",    totalAssociations.get());
        m.put("boxesPerCase",         boxesPerCase);
        m.put("boxesPerPallet",       boxesPerPallet);
        return m;
    }

    /**
     * 获取 seq > lastSeq 的事件列表（前端增量轮询）。
     * 每个 Map 包含 seq / type / message / time / data(可选)。
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
     * TCP 接收循环：连接相机 → 原始字节读取 → 按 \n 拆包（无 \n 时立即触发）→ 放入队列。
     * 参照致美斋 TcpConnectionService 实现，支持不带换行符的相机数据格式。
     * 断线后休眠 RECONNECT_MS 再重连（active=false 时退出）。
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
                sock.connect(new InetSocketAddress(address, port), 5000); // 5s 连接超时
                // 不设置 setSoTimeout：读取阻塞等待数据，与致美斋保持一致
                if (isBoxCamera) boxSocket  = sock;
                else             caseSocket = sock;

                addEvent(isBoxCamera ? "BOX_CONNECTED" : "CASE_CONNECTED",
                        name + " 连接成功 (" + address + ":" + port + ")", null);
                log.info("[{}] 连接成功", name);

                InputStream is = sock.getInputStream();
                byte[]        buffer  = new byte[1024];
                StringBuilder msgBuf  = new StringBuilder();

                while (active) {
                    int n = is.read(buffer);
                    if (n == -1) break; // 服务端关闭连接

                    String chunk      = new String(buffer, 0, n, StandardCharsets.UTF_8);
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

                    // 无换行符时立即触发（兼容不以 \n 结尾的相机数据格式）
                    if (!hasNewline && msgBuf.length() > 0) {
                        String code = msgBuf.toString().trim();
                        if (!code.isEmpty()) onCodeReceived(code, isBoxCamera);
                        msgBuf.setLength(0);
                    }

                    // 防止缓冲区积压
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
        log.info("[{}] 接收线程退出", name);
    }

    /**
     * 盒码收到：按 ";" 分隔提取并去重，若数量不等于 boxesPerCase 则全部写入剔除记录（超码/缺码）；
     * 数量正确时逐一校验后放入内存队列。
     * 箱码收到：从内存队列取 N（boxesPerCase）个盒码做关联，不足则箱码入待处理列表。
     */
    private void onCodeReceived(String rawCode, boolean isBoxCamera) {
        if (isBoxCamera) {
            // 去重：保留首次出现顺序
            String[] parts = rawCode.split(";");
            List<String> uniqueCodes = new ArrayList<>();
            java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
            for (String part : parts) {
                String code = part.trim();
                if (!code.isEmpty()) seen.add(code);
            }
            uniqueCodes.addAll(seen);

            int expected = boxesPerCase;
            // 超码/缺码时全部写入剔除记录
            if (uniqueCodes.size() != expected) {
                String reason = uniqueCodes.size() < expected ? "缺码" : "超码";
                String desc = "[盒码相机] 一次读取到 " + uniqueCodes.size()
                        + " 个盒码（去重后），期望 " + expected + " 个，原因：" + reason;
                addEvent("BOX_FAIL", desc, null);
                log.warn("[盒码] 数量不符：期望{}个，实际{}个，原因：{}", expected, uniqueCodes.size(), reason);
                for (String code : uniqueCodes) {
                    pendingFailedBoxes.offer(new FailedBoxEntry(code, reason, code));
                }
                // 无箱码时无法写剔除记录，留在 pendingFailedBoxes 等 ASSOC_FAIL 时统一写入
                return;
            }

            for (String code : uniqueCodes) {
                addEvent("BOX_RECV", "[盒码相机] 接收数据：" + code, null);
                // 检查该盒码是否有 Status=4（M1同步时热表校验不通过）的关联记录
                try {
                    boolean hasStatus4 = boxCaseService.checkAndWriteRejectsForStatus4(currentOrderNo, code);
                    if (hasStatus4) {
                        addEvent("BOX_FAIL", "盒码 " + code + " 对应瓶盒关联数据热表校验不通过（待剔除），已写入剔除记录", null);
                        log.warn("[盒码] {} 存在 Status=4 记录，跳过入队", code);
                        pendingFailedBoxes.offer(new FailedBoxEntry(code, "码包热表校验不通过", code));
                        continue;
                    }
                } catch (Exception ex) {
                    log.warn("[盒码] {} Status=4 检查异常: {}", code, ex.getMessage());
                }
                ApiResult<Void> res = boxCaseService.validateBoxCodeForReceive(
                        currentOrderNo, currentProductNo, code, REQUIRED_ROWS_PER_BOX);
                if (res != null && res.getCode() != null && res.getCode() == 200) {
                    boxCodeQueue.offer(code);
                    addEvent("BOX_CODE", "盒码采集成功：" + code + "（队列=" + boxCodeQueue.size() + "）", null);
                    log.debug("[盒码] {} 校验通过，已入队列，队列长={}", code, boxCodeQueue.size());
                    tryConsumePendingCaseCode();
                } else {
                    String errMsg = res != null ? res.getMessage() : "服务返回null";
                    addEvent("BOX_FAIL", "盒码采集失败：" + code + "，原因：" + errMsg, null);
                    log.warn("[盒码] {} 校验失败: {}", code, errMsg);
                    // 箱码此时尚未到达，无法写剔除记录（需箱码必填）；暂存，待 ASSOC_FAIL 时统一写入
                    pendingFailedBoxes.offer(new FailedBoxEntry(code, parseBoxRejectReason(errMsg), code));
                }
            }
        } else {
            String code = rawCode.trim();
            if (code.isEmpty()) return;
            addEvent("CASE_RECV", "[箱码相机] 接收数据：" + code, null);
            List<String> polled = new ArrayList<>(boxesPerCase);
            for (int i = 0; i < boxesPerCase; i++) {
                String b = boxCodeQueue.poll();
                if (b == null) {
                    polled.forEach(boxCodeQueue::offer);
                    pendingCaseCodes.offer(code);
                    addEvent("CASE_PENDING", "箱码待处理：" + code + "，队列中盒码不足" + boxesPerCase + "条", null);
                    log.debug("[箱码] {} 已入待处理列表", code);
                    return;
                }
                polled.add(b);
            }
            ApiResult<Map<String, Object>> res = boxCaseService.associateCaseCodeWithBoxCodes(
                    currentOrderNo, currentProductNo, code, polled, boxesPerPallet);
            if (res == null || res.getCode() == null || res.getCode() != 200) {
                polled.forEach(boxCodeQueue::offer);
                String errMsg = res != null ? res.getMessage() : "服务返回null";
                addEvent("ASSOC_FAIL", "关联失败 箱码:" + code + "，原因：" + errMsg, buildFailData(polled, code, errMsg));
                log.warn("箱码关联失败 caseCode={} boxCodes={} err={}", code, polled, errMsg);
                writeRejectRecordsOnAssocFail(currentOrderNo, code, polled, errMsg);
                return;
            }
            Map<String, Object> data = res.getData();
            long assocNo = totalAssociations.incrementAndGet();
            int cases = data != null && data.get("currentCaseCount") instanceof Number
                    ? ((Number) data.get("currentCaseCount")).intValue() : currentCasesInPallet.get() + 1;
            boolean fullPallet = data != null && Boolean.TRUE.equals(data.get("fullPallet"));
            String palletCode = data != null ? (String) data.get("palletCode") : null;
            if (fullPallet) currentCasesInPallet.set(0);
            else currentCasesInPallet.set(cases);
            Map<String, Object> evtData = new LinkedHashMap<>();
            evtData.put("boxCodes", polled);
            evtData.put("caseCode", code);
            evtData.put("currentCaseCount", cases);
            evtData.put("fullPallet", fullPallet);
            if (palletCode != null) evtData.put("palletCode", palletCode);
            addEvent("ASSOCIATED", "关联成功 #" + assocNo + " 箱码:" + code + " 盒码:[" + String.join(",", polled) + "]", evtData);
            log.info("关联成功 #{} caseCode={} boxCodes={} cases={} fullPallet={}", assocNo, code, polled, cases, fullPallet);
            // 关联成功：该箱通过，清空暂存失败盒码（物理上无需触发剔除）
            pendingFailedBoxes.clear();
        }
    }

    /** 内存队列中盒码数≥N 且有待处理箱码时，取 N 个盒码与一条箱码做关联，成功后这 N 条盒码已从队列移除 */
    private void tryConsumePendingCaseCode() {
        if (boxCodeQueue.size() < boxesPerCase) return;
        String caseCode = pendingCaseCodes.poll();
        if (caseCode == null) return;
        List<String> polled = new ArrayList<>(boxesPerCase);
        for (int i = 0; i < boxesPerCase; i++) {
            String b = boxCodeQueue.poll();
            if (b == null) {
                polled.forEach(boxCodeQueue::offer);
                pendingCaseCodes.offer(caseCode);
                return;
            }
            polled.add(b);
        }
        ApiResult<Map<String, Object>> res = boxCaseService.associateCaseCodeWithBoxCodes(
                currentOrderNo, currentProductNo, caseCode, polled, boxesPerPallet);
        if (res == null || res.getCode() == null || res.getCode() != 200) {
            polled.forEach(boxCodeQueue::offer);
            pendingCaseCodes.offer(caseCode);
            log.warn("待处理箱码关联失败 caseCode={} err={}", caseCode, res != null ? res.getMessage() : "null");
            return;
        }
        Map<String, Object> data = res.getData();
        long assocNo = totalAssociations.incrementAndGet();
        int cases = data != null && data.get("currentCaseCount") instanceof Number
                ? ((Number) data.get("currentCaseCount")).intValue() : currentCasesInPallet.get() + 1;
        boolean fullPallet = data != null && Boolean.TRUE.equals(data.get("fullPallet"));
        String palletCode = data != null ? (String) data.get("palletCode") : null;
        if (fullPallet) currentCasesInPallet.set(0);
        else currentCasesInPallet.set(cases);
        Map<String, Object> evtData = new LinkedHashMap<>();
        evtData.put("boxCodes", polled);
        evtData.put("caseCode", caseCode);
        evtData.put("currentCaseCount", cases);
        evtData.put("fullPallet", fullPallet);
        if (palletCode != null) evtData.put("palletCode", palletCode);
        addEvent("ASSOCIATED", "关联成功(待处理) #" + assocNo + " 箱码:" + caseCode + " 盒码:[" + String.join(",", polled) + "]", evtData);
        log.info("待处理箱码关联成功 #{} caseCode={} boxCodes={}", assocNo, caseCode, polled);
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
        long seq  = eventSeq.incrementAndGet();
        CaptureEvent evt = new CaptureEvent(seq, type, message,
                LocalDateTime.now().format(TIME_FMT), data);
        synchronized (eventLock) {
            if (recentEvents.size() >= MAX_EVENTS) recentEvents.pollFirst();
            recentEvents.addLast(evt);
        }
    }

    /**
     * ASSOC_FAIL 时写剔除记录：
     * <ol>
     *   <li>根据错误信息确定剔除原因，决定是写"箱码级"还是"盒码级"记录；</li>
     *   <li>排尽 {@link #pendingFailedBoxes}（BOX_FAIL 暂存），用本次失败箱码补全后写入。</li>
     * </ol>
     */
    private void writeRejectRecordsOnAssocFail(String orderNo, String caseCode,
                                               List<String> polledBoxCodes, String errMsg) {
        String caseReason = parseCaseRejectReason(errMsg);

        if ("大标不通过".equals(caseReason) || "重码".equals(caseReason)) {
            // 箱码本身有问题：写一条记录，ProblemCode = 箱码
            boxCaseService.insertRejectRecord(orderNo, caseCode, null, null, caseCode, caseReason);
        } else {
            // 盒箱校验不通过：按盒分条写入（便于核对 N 盒与箱码）
            for (String boxCode : polledBoxCodes) {
                boxCaseService.insertRejectRecord(orderNo, caseCode, boxCode, null, null, "盒箱校验不通过");
            }
        }

        // 排尽暂存的 BOX_FAIL 失败盒码，用本次失败箱码补全后写入
        List<FailedBoxEntry> deferred = new ArrayList<>();
        pendingFailedBoxes.drainTo(deferred);
        for (FailedBoxEntry entry : deferred) {
            boxCaseService.insertRejectRecord(orderNo, caseCode,
                    entry.boxCode, null, entry.problemCode, entry.rejectReason);
        }
    }

    /**
     * 从 ASSOC_FAIL 错误信息推断箱码级剔除原因。
     * <ul>
     *   <li>"大标码包" → 大标不通过（箱码不在码包内）</li>
     *   <li>"重码"     → 重码（箱码已被使用）</li>
     *   <li>其他      → 盒箱校验不通过</li>
     * </ul>
     */
    private static String parseCaseRejectReason(String errMsg) {
        if (errMsg == null) return "盒箱校验不通过";
        if (errMsg.contains("大标码包")) return "大标不通过";
        if (errMsg.contains("重码"))     return "重码";
        return "盒箱校验不通过";
    }

    /**
     * 从 BOX_FAIL 错误信息推断盒码级剔除原因。
     * <ul>
     *   <li>"中标码包" → 中标不通过（盒码不在码包内）</li>
     *   <li>"重码"     → 重码（盒码已被使用）</li>
     *   <li>其他      → 盒箱校验不通过（如1号机已标记该盒码为异常）</li>
     * </ul>
     */
    private static String parseBoxRejectReason(String errMsg) {
        if (errMsg == null) return "盒箱校验不通过";
        if (errMsg.contains("中标码包")) return "中标不通过";
        if (errMsg.contains("重码"))     return "重码";
        return "盒箱校验不通过";
    }

    private static Map<String, Object> buildFailData(List<String> boxCodes, String caseCode, String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("boxCodes",  boxCodes);
        m.put("caseCode",  caseCode);
        m.put("reason",    reason);
        return m;
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

    /** BOX_FAIL 暂存项：记录校验失败的盒码及其剔除原因，等待箱码到达后统一写入 RejectRecord。 */
    private static class FailedBoxEntry {
        final String boxCode;
        final String rejectReason;
        final String problemCode;

        FailedBoxEntry(String boxCode, String rejectReason, String problemCode) {
            this.boxCode      = boxCode;
            this.rejectReason = rejectReason;
            this.problemCode  = problemCode;
        }
    }

    private static class CaptureEvent {
        final long               seq;
        final String             type;
        final String             message;
        final String             time;
        final Map<String, Object> data;

        CaptureEvent(long seq, String type, String message, String time, Map<String, Object> data) {
            this.seq     = seq;
            this.type    = type;
            this.message = message;
            this.time    = time;
            this.data    = data;
        }
    }
}
