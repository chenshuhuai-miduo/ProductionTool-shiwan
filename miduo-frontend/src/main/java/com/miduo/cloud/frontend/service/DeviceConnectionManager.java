package com.miduo.cloud.frontend.service;

import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * 设备连接管理器
 * 管理所有IO设备的连接状态和数据接收
 */
public class DeviceConnectionManager {
    
    // 单例
    private static DeviceConnectionManager instance;
    
    // 存储所有连接服务
    private Map<String, Object> connectionServices = new HashMap<>();
    
    // 存储设备配置信息（用于按名称查找设备）
    private Map<String, IoDeviceDTO> deviceConfigs = new HashMap<>();
    
    // 存储设备类别映射（设备ID -> 设备类别代码1-6）
    private Map<String, Integer> deviceCategoryMap = new HashMap<>();
    
    // 已连接的设备列表（按连接顺序）
    private List<String> connectedDeviceIds = new ArrayList<>();
    
    // 数据接收处理器（旧版本，兼容保留）
    private Consumer<String> dataReceiveHandler;
    
    // 数据接收处理器（新版本，携带设备类别信息）
    // 参数1: 设备类别代码（1=码校验, 2=箱码采集, 3=托盘码关联, 4=箱码关联, 5=报警器, 6=剔除设备），参数2: 接收到的数据
    private BiConsumer<Integer, String> dataReceiveHandlerWithOrder;
    
    // 操作日志处理器（用于记录重试日志到主界面）
    private Consumer<String> operationLogHandler;
    
    // 设备状态变化处理器（设备连接/断开时通知主界面更新状态）
    private Runnable deviceStatusChangeHandler;
    
    // 设备重试状态（deviceId -> Thread）
    private Map<String, Thread> retryThreads = new HashMap<>();
    
    private DeviceConnectionManager() {}
    
    public static synchronized DeviceConnectionManager getInstance() {
        if (instance == null) {
            instance = new DeviceConnectionManager();
        }
        return instance;
    }
    
    /**
     * 设置数据接收处理器（通常由主界面设置）
     */
    public void setDataReceiveHandler(Consumer<String> handler) {
        this.dataReceiveHandler = handler;
    }
    
    /**
     * 设置数据接收处理器（携带设备类别信息）
     * 
     * @param handler 处理器，参数1=设备类别代码(1=码校验,2=箱码采集,3=托盘码关联,4=箱码关联,5=报警器,6=剔除设备)，参数2=接收到的数据
     */
    public void setDataReceiveHandlerWithOrder(BiConsumer<Integer, String> handler) {
        this.dataReceiveHandlerWithOrder = handler;
    }
    
    /**
     * 设置操作日志处理器（用于记录重试日志）
     * 
     * @param handler 日志处理器
     */
    public void setOperationLogHandler(Consumer<String> handler) {
        this.operationLogHandler = handler;
    }
    
    /**
     * 设置设备状态变化处理器（设备连接/断开时通知）
     * 
     * @param handler 状态变化处理器
     */
    public void setDeviceStatusChangeHandler(Runnable handler) {
        this.deviceStatusChangeHandler = handler;
    }
    
    /**
     * 根据设备配置启动连接
     */
    public void startConnection(IoDeviceDTO device) throws Exception {
        if (device == null || !device.getEnabled()) {
            return;
        }
        
        String deviceId = device.getId();
        
        // 保存设备配置
        deviceConfigs.put(deviceId, device);
        deviceConfigs.put(device.getDeviceName(), device); // 也支持按名称查找
        
        // 如果已经连接，先断开
        stopConnection(deviceId);
        
        String connectionType = device.getConnectionType();
        
        if ("网口".equals(connectionType)) {
            startNetworkConnection(device);
        } else if ("串口".equals(connectionType)) {
            startSerialConnection(device);
        }
        
        // 记录设备连接顺序
        if (!connectedDeviceIds.contains(deviceId)) {
            connectedDeviceIds.add(deviceId);
        }
        
        // 获取设备类别代码（从设备类别文本转换为数字代码）
        int categoryCode = convertCategoryTextToCode(device.getDeviceCategory());
        deviceCategoryMap.put(deviceId, categoryCode);
        
        String categoryName = device.getDeviceCategory() != null ? device.getDeviceCategory() : "未知";
        System.out.println("[连接管理器] 设备已连接: " + device.getDeviceName() + 
                          " (类别: " + categoryName + ", 代码: " + categoryCode + ")");
        
        // 通知主界面更新设备状态
        notifyDeviceStatusChange();
    }
    
    /**
     * 将设备类别文本转换为代码
     * 码校验->1, 箱码采集->2, 托盘码关联->3, 箱码关联->4, 报警器->5, 剔除设备->6, 扫码枪->7
     */
    private int convertCategoryTextToCode(String categoryText) {
        if (categoryText == null) return 0;
        switch (categoryText) {
            case "码校验": return 1;
            case "箱码采集": return 2;
            case "托盘码关联": return 3;
            case "箱码关联": return 4;
            case "报警器": return 5;
            case "剔除设备": return 6;
            case "扫码枪": return 7;
            default: return 0;
        }
    }
    
    /**
     * 启动网口连接（TCP/UDP）
     */
    private void startNetworkConnection(IoDeviceDTO device) throws Exception {
        String protocolType = device.getProtocolType();
        String address = device.getAddress();
        int port = Integer.parseInt(device.getPort());
        String deviceId = device.getId();
        
        if ("UDP".equals(protocolType)) {
            // UDP连接
            UdpConnectionService udpService = new UdpConnectionService();
            udpService.setMessageHandler(data -> handleReceivedData(deviceId, data));
            udpService.setErrorHandler(this::handleError);
            udpService.connect(address, port);
            connectionServices.put(deviceId, udpService);
            System.out.println("[连接管理器] UDP连接启动成功: " + device.getDeviceName() + " (" + address + ":" + port + ")");
        } else {
            // TCP连接（默认）
            TcpConnectionService tcpService = new TcpConnectionService();
            tcpService.setMessageHandler(data -> handleReceivedData(deviceId, data));
            tcpService.setErrorHandler(this::handleError);
            tcpService.connect(address, port);
            connectionServices.put(deviceId, tcpService);
            System.out.println("[连接管理器] TCP连接启动成功: " + device.getDeviceName() + " (" + address + ":" + port + ")");
        }
    }
    
    /**
     * 启动串口连接
     */
    private void startSerialConnection(IoDeviceDTO device) throws Exception {
        String portName = device.getAddress(); // 串口号
        int baudRate = Integer.parseInt(device.getPort()); // 波特率
        String deviceId = device.getId();
        String deviceCategory = device.getDeviceCategory();
        
        // 判断是否为扫码枪设备（扫码枪只打开配置串口，数据接收串口由外部程序管理）
        if ("扫码枪".equals(deviceCategory)) {
            BarcodeScannerConnectionService scannerService = new BarcodeScannerConnectionService();
            scannerService.setMessageHandler(data -> handleReceivedData(deviceId, data));
            scannerService.setErrorHandler(this::handleError);
            scannerService.connect(portName, baudRate);
            connectionServices.put(deviceId, scannerService);
            System.out.println("[连接管理器] 扫码枪串口连接启动成功: " + device.getDeviceName() + 
                             " (串口:" + portName + ", 数据接收串口COM(N+1)由外部程序管理)");
        } else {
            // 普通串口设备
            SerialConnectionService serialService = new SerialConnectionService();
            serialService.setMessageHandler(data -> handleReceivedData(deviceId, data));
            serialService.setErrorHandler(this::handleError);
            serialService.connect(portName, baudRate);
            connectionServices.put(deviceId, serialService);
            System.out.println("[连接管理器] 串口连接启动成功: " + device.getDeviceName());
        }
    }
    
    /**
     * 停止指定设备的连接
     */
    public void stopConnection(String deviceId) {
        Object service = connectionServices.get(deviceId);
        if (service != null) {
            if (service instanceof TcpConnectionService) {
                ((TcpConnectionService) service).disconnect();
            } else if (service instanceof UdpConnectionService) {
                ((UdpConnectionService) service).disconnect();
            } else if (service instanceof SerialConnectionService) {
                ((SerialConnectionService) service).disconnect();
            } else if (service instanceof BarcodeScannerConnectionService) {
                ((BarcodeScannerConnectionService) service).disconnect();
            }
            connectionServices.remove(deviceId);
            connectedDeviceIds.remove(deviceId);
            deviceCategoryMap.remove(deviceId);
            System.out.println("[连接管理器] 连接已停止: " + deviceId);
            
            // 通知主界面更新设备状态
            notifyDeviceStatusChange();
        }
    }
    
    /**
     * 停止所有连接
     */
    public void stopAllConnections() {
        // 先停止所有重试线程
        stopAllRetries();
        
        // 创建keySet的副本以避免ConcurrentModificationException
        java.util.Set<String> deviceIdsCopy = new java.util.HashSet<>(connectionServices.keySet());
        
        for (String deviceId : deviceIdsCopy) {
            try {
            stopConnection(deviceId);
            } catch (Exception e) {
                System.err.println("[连接管理器] 停止连接时发生错误: " + deviceId + " - " + e.getMessage());
            }
        }
        
        // 清空所有映射
        connectionServices.clear();
        connectedDeviceIds.clear();
        deviceCategoryMap.clear();
        
        System.out.println("[连接管理器] 所有连接已停止");
    }
    
    /**
     * 检查设备是否已连接
     */
    public boolean isConnected(String deviceId) {
        Object service = connectionServices.get(deviceId);
        if (service instanceof TcpConnectionService) {
            return ((TcpConnectionService) service).isConnected();
        } else if (service instanceof UdpConnectionService) {
            return ((UdpConnectionService) service).isConnected();
        } else if (service instanceof SerialConnectionService) {
            return ((SerialConnectionService) service).isConnected();
        } else if (service instanceof BarcodeScannerConnectionService) {
            return ((BarcodeScannerConnectionService) service).isConnected();
        }
        return false;
    }
    
    /**
     * 处理接收到的数据
     * 
     * @param deviceId 设备ID
     * @param data 接收到的数据
     */
    private void handleReceivedData(String deviceId, String data) {
        // 获取设备类别代码
        Integer categoryCode = deviceCategoryMap.get(deviceId);
        if (categoryCode == null) {
            categoryCode = 0; // 未知设备
        }
        
        IoDeviceDTO device = deviceConfigs.get(deviceId);
        String deviceName = device != null ? device.getDeviceName() : deviceId;
        String categoryName = device != null && device.getDeviceCategory() != null ? device.getDeviceCategory() : "未知";
        
        System.out.println("[连接管理器] 接收到数据: [" + categoryName + "(" + categoryCode + ")-" + deviceName + "] " + data);
        
        // 调用新版处理器（携带设备类别代码）
        if (dataReceiveHandlerWithOrder != null) {
            final int finalCategoryCode = categoryCode;
            Platform.runLater(() -> dataReceiveHandlerWithOrder.accept(finalCategoryCode, data));
        }
        
        // 兼容旧版处理器
        if (dataReceiveHandler != null) {
            Platform.runLater(() -> dataReceiveHandler.accept(data));
        }
    }
    
    /**
     * 处理错误（设备断开连接时触发重试）
     */
    private void handleError(String error) {
        System.err.println("[连接管理器] 错误: " + error);
        
        // 通知主界面更新设备状态
        notifyDeviceStatusChange();
        
        // 检查是否有设备断开连接，触发重试机制
        checkAndRetryDisconnectedDevices();
    }
    
    /**
     * 检查并重试断开连接的设备
     */
    private void checkAndRetryDisconnectedDevices() {
        System.out.println("[重试检查] 开始检查断开的设备，当前设备配置数量: " + deviceConfigs.size());
        
        // 使用已处理的设备ID集合，避免重复处理
        java.util.Set<String> processedDeviceIds = new java.util.HashSet<>();
        
        for (Map.Entry<String, IoDeviceDTO> entry : deviceConfigs.entrySet()) {
            IoDeviceDTO device = entry.getValue();
            String deviceId = device.getId();
            
            // 跳过已处理的设备（因为deviceConfigs中每个设备存了两份：ID和名称）
            if (processedDeviceIds.contains(deviceId)) {
                continue;
            }
            
            // 标记为已处理
            processedDeviceIds.add(deviceId);
            
            System.out.println("[重试检查] 检查设备: " + device.getDeviceName() + 
                             ", ID=" + deviceId + 
                             ", 已启用=" + device.getEnabled() + 
                             ", 已连接=" + isConnected(deviceId) +
                             ", timeout=" + device.getTimeout() +
                             ", retryCount=" + device.getRetryCount());
            
            // 检查设备是否已启用且已断开连接
            if (device.getEnabled() && !isConnected(deviceId)) {
                // 检查是否已经在重试中
                Thread existingRetryThread = retryThreads.get(deviceId);
                if (existingRetryThread == null || !existingRetryThread.isAlive()) {
                    // 启动重试线程
                    System.out.println("[连接管理器] 检测到设备断开，准备启动重试: " + device.getDeviceName());
                    startRetryThread(device);
                } else {
                    System.out.println("[重试检查] 设备 " + device.getDeviceName() + " 已经在重试中");
                }
            }
        }
        
        System.out.println("[重试检查] 检查完成");
    }
    
    /**
     * 启动设备重试线程
     * 
     * @param device 设备配置
     */
    private void startRetryThread(IoDeviceDTO device) {
        String deviceId = device.getId();
        String deviceName = device.getDeviceName();
        int timeout = device.getTimeout() * 1000; // 转换为毫秒
        int retryCount = device.getRetryCount();
        
        // 取消旧的重试线程
        Thread oldThread = retryThreads.get(deviceId);
        if (oldThread != null && oldThread.isAlive()) {
            oldThread.interrupt();
        }
        
        // 创建新的重试线程
        Thread retryThread = new Thread(() -> {
            logOperation("[" + deviceName + "] 检测到连接断开，将尝试重连（重试次数: " + retryCount + ", 间隔: " + device.getTimeout() + "秒）");
            
            for (int i = 1; i <= retryCount; i++) {
                try {
                    // 检查线程是否被中断
                    if (Thread.currentThread().isInterrupted()) {
                        logOperation("[" + deviceName + "] 重试线程被中断");
                        break;
                    }
                    
                    logOperation("[" + deviceName + "] 正在进行第 " + i + "/" + retryCount + " 次重试...");
                    
                    // 等待指定时间后重试
                    Thread.sleep(timeout);
                    
                    // 尝试重新连接
                    try {
                        // 先清理旧连接
                        stopConnection(deviceId);
                        Thread.sleep(500); // 等待资源释放
                        
                        // 启动新连接
                        startConnection(device);
                        Thread.sleep(1000); // 等待连接建立
                        
                        // 检查连接是否成功
                        if (isConnected(deviceId)) {
                            logOperation("[" + deviceName + "] ✓ 第 " + i + " 次重试成功，设备已重新连接");
                            retryThreads.remove(deviceId);
                            // 通知主界面更新设备状态
                            notifyDeviceStatusChange();
                            return; // 重连成功，退出重试
                        } else {
                            logOperation("[" + deviceName + "] ✗ 第 " + i + " 次重试失败");
                        }
                        
                    } catch (Exception e) {
                        logOperation("[" + deviceName + "] ✗ 第 " + i + " 次重试失败: " + e.getMessage());
                    }
                    
                } catch (InterruptedException e) {
                    logOperation("[" + deviceName + "] 重试线程被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 所有重试都失败
            if (!isConnected(deviceId)) {
                logOperation("[" + deviceName + "] ✗ 已尝试 " + retryCount + " 次重连，全部失败，停止重试");
            }
            
            retryThreads.remove(deviceId);
            
        }, "DeviceRetry-" + deviceName);
        
        retryThread.setDaemon(true); // 设置为守护线程
        retryThread.start();
        retryThreads.put(deviceId, retryThread);
    }
    
    /**
     * 记录操作日志
     * 
     * @param message 日志消息
     */
    private void logOperation(String message) {
        System.out.println("[设备重试] " + message);
        
        if (operationLogHandler != null) {
            // 添加时间戳
            String timestamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logMessage = timestamp + " " + message;
            
            Platform.runLater(() -> operationLogHandler.accept(logMessage));
        }
    }
    
    /**
     * 通知主界面更新设备状态
     */
    private void notifyDeviceStatusChange() {
        if (deviceStatusChangeHandler != null) {
            Platform.runLater(() -> deviceStatusChangeHandler.run());
        }
    }
    
    /**
     * 停止设备重试
     * 
     * @param deviceId 设备ID
     */
    public void stopRetry(String deviceId) {
        Thread retryThread = retryThreads.get(deviceId);
        if (retryThread != null && retryThread.isAlive()) {
            retryThread.interrupt();
            retryThreads.remove(deviceId);
            System.out.println("[设备重试] 已停止设备重试: " + deviceId);
        }
    }
    
    /**
     * 停止所有设备重试
     */
    public void stopAllRetries() {
        for (Thread retryThread : retryThreads.values()) {
            if (retryThread != null && retryThread.isAlive()) {
                retryThread.interrupt();
            }
        }
        retryThreads.clear();
        System.out.println("[设备重试] 已停止所有设备重试");
    }
    
    /**
     * 根据设备名称获取设备配置
     * 
     * @param deviceName 设备名称
     * @return 设备配置，未找到返回null
     */
    public IoDeviceDTO getDeviceByName(String deviceName) {
        return deviceConfigs.get(deviceName);
    }
    
    /**
     * 根据设备类别代码获取设备配置
     * 
     * @param categoryCode 设备类别代码（1=码校验, 2=箱码采集, 3=托盘码关联, 4=箱码关联, 5=报警器, 6=剔除设备）
     * @return 设备配置，未找到返回null
     */
    public IoDeviceDTO getDeviceByCategory(int categoryCode) {
        // 遍历设备映射，找到匹配类别代码的设备
        for (Map.Entry<String, Integer> entry : deviceCategoryMap.entrySet()) {
            if (entry.getValue() == categoryCode) {
                String deviceId = entry.getKey();
                return deviceConfigs.get(deviceId);
            }
        }
        return null;
    }
    
    /**
     * 向指定设备发送数据（仅支持串口）
     * 
     * @param deviceName 设备名称
     * @param data 要发送的数据
     * @return 发送成功返回true
     */
    public boolean sendToDevice(String deviceName, String data) {
        System.out.println("[连接管理器] sendToDevice 被调用: 设备名=" + deviceName + ", 数据=" + data);
        
        IoDeviceDTO device = getDeviceByName(deviceName);
        if (device == null) {
            System.err.println("[连接管理器] 未找到设备: " + deviceName);
            return false;
        }
        
        System.out.println("[连接管理器] 设备信息: ID=" + device.getId() + ", 名称=" + device.getDeviceName() + ", 连接类型=" + device.getConnectionType());
        
        Object service = connectionServices.get(device.getId());
        System.out.println("[连接管理器] 连接服务: " + (service != null ? service.getClass().getSimpleName() : "null"));
        
        if (service == null) {
            System.err.println("[连接管理器] 设备未连接: " + deviceName + ", 设备ID=" + device.getId());
            System.err.println("[连接管理器] 当前连接服务列表: " + connectionServices.keySet());
            return false;
        }
        
        // 目前只支持串口发送
        if (service instanceof SerialConnectionService) {
            System.out.println("[连接管理器] 调用串口服务发送数据");
            boolean result = ((SerialConnectionService) service).sendData(data);
            System.out.println("[连接管理器] 串口发送结果: " + result);
            return result;
        } else {
            System.err.println("[连接管理器] 该设备类型不支持发送数据: " + deviceName + ", 服务类型: " + service.getClass().getName());
            return false;
        }
    }
    
    /**
     * 向指定设备类别的设备发送数据
     * 
     * @param categoryCode 设备类别代码（1=码校验, 2=箱码采集, 3=托盘码关联, 4=箱码关联, 5=报警器, 6=剔除设备）
     * @param data 要发送的数据
     * @return 发送成功返回true
     */
    public boolean sendToDeviceByCategory(int categoryCode, String data) {
        System.out.println("[连接管理器] 尝试向类别代码 " + categoryCode + " 的设备发送数据: " + data);
        
        // 打印当前设备类别映射
        System.out.println("[连接管理器] 当前设备类别映射: " + deviceCategoryMap);
        
        // 查找该类别的设备ID
        String targetDeviceId = null;
        for (Map.Entry<String, Integer> entry : deviceCategoryMap.entrySet()) {
            System.out.println("[连接管理器] 检查设备: ID=" + entry.getKey() + ", 类别=" + entry.getValue());
            if (entry.getValue() == categoryCode) {
                targetDeviceId = entry.getKey();
                System.out.println("[连接管理器] 找到匹配设备: " + targetDeviceId);
                break;
            }
        }
        
        if (targetDeviceId == null) {
            System.err.println("[连接管理器] 未找到类别代码为 " + categoryCode + " 的设备");
            return false;
        }
        
        IoDeviceDTO device = deviceConfigs.get(targetDeviceId);
        if (device == null) {
            System.err.println("[连接管理器] 设备配置不存在: " + targetDeviceId);
            return false;
        }
        
        System.out.println("[连接管理器] 准备向设备 " + device.getDeviceName() + " 发送数据");
        return sendToDevice(device.getDeviceName(), data);
    }
    
    /**
     * 获取所有已连接的设备列表
     * 
     * @return 设备配置列表
     */
    public java.util.List<IoDeviceDTO> getConnectedDevices() {
        java.util.List<IoDeviceDTO> connected = new java.util.ArrayList<>();
        for (String deviceId : connectedDeviceIds) {
            IoDeviceDTO device = deviceConfigs.get(deviceId);
            if (device != null && isConnected(deviceId)) {
                connected.add(device);
            }
        }
        return connected;
    }
    
    /**
     * 获取设备的类别代码
     * 
     * @param deviceId 设备ID
     * @return 类别代码（1=码校验,2=箱码采集,3=托盘码关联,4=箱码关联,5=报警器,6=剔除设备），未找到返回0
     */
    public int getDeviceCategory(String deviceId) {
        return deviceCategoryMap.getOrDefault(deviceId, 0);
    }
}

