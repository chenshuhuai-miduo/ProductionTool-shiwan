package com.miduo.cloud.frontend.service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.lang.reflect.Method;

/**
 * 扫码枪串口通信服务类
 * 只打开配置串口COM(N)，从配置串口接收数据
 * 不打开数据接收串口COM(N+1)，该串口由外部程序管理
 */
public class BarcodeScannerConnectionService {
    
    private SerialPort configPort;      // 配置串口（例如COM5），同时用于接收数据
    private volatile boolean running = false;
    private Consumer<String> messageHandler;
    private Consumer<String> errorHandler;
    private StringBuilder messageBuffer = new StringBuilder();
    
    private String configPortName;      // 配置串口名称
    
    // 用于延迟处理数据的定时器
    private java.util.Timer delayTimer = null;
    private final Object timerLock = new Object();
    
    /**
     * 设置消息接收处理器
     */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    /**
     * 设置错误处理器
     */
    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }
    
    /**
     * 打开配置串口（只打开一个串口，用于接收数据）
     * @param portName 配置串口名称（例如：COM5）
     * @param baudRate 波特率
     */
    public void connect(String portName, int baudRate) throws Exception {
        if (isConnected()) {
            throw new Exception("扫码枪串口已经打开");
        }
        
        this.configPortName = portName;
        
        System.out.println("[扫码枪] 正在打开串口: " + configPortName + ", 波特率: " + baudRate);
        System.out.println("[扫码枪] 注意：只打开配置串口，数据接收串口COM(N+1)由外部程序管理");
        
        try {
            // 打开配置串口（用于发送命令和接收数据）
            configPort = SerialPort.getCommPort(configPortName);
            configPort.setBaudRate(baudRate);
            configPort.setNumDataBits(8);
            configPort.setNumStopBits(1);
            configPort.setParity(SerialPort.NO_PARITY);
            configPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
            
            if (!configPort.openPort()) {
                throw new Exception("无法打开串口: " + configPortName);
            }
            System.out.println("[扫码枪] 串口打开成功: " + configPortName);
            
            running = true;
            
            // 在配置串口添加监听器，接收数据
            configPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }
                
                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                        return;
                    
                    byte[] newData = new byte[configPort.bytesAvailable()];
                    int numRead = -1;
                    boolean methodFound = false;
                    
                    // 使用反射查找并调用 readBytes 方法（兼容不同版本的 jSerialComm 库）
                    try {
                        // 尝试方法 1: readBytes(byte[], int)
                        try {
                            Method method = SerialPort.class.getMethod("readBytes", byte[].class, int.class);
                            numRead = (int) method.invoke(configPort, newData, newData.length);
                            methodFound = true;
                        } catch (NoSuchMethodException e) {
                            // 方法不存在，继续尝试其他方法
                        }
                        
                        // 尝试方法 2: readBytes(byte[], long)
                        if (!methodFound) {
                            try {
                                Method method = SerialPort.class.getMethod("readBytes", byte[].class, long.class);
                                numRead = (int) method.invoke(configPort, newData, (long) newData.length);
                                methodFound = true;
                            } catch (NoSuchMethodException e) {
                                // 方法不存在，继续尝试其他方法
                            }
                        }
                        
                        if (!methodFound) {
                            System.err.println("[扫码枪] 无法找到任何可用的 readBytes 方法");
                            return;
                        }
                        
                    } catch (Exception e) {
                        System.err.println("[扫码枪] 反射调用 readBytes 失败: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    
                    if (numRead > 0) {
                        String data = new String(newData, 0, numRead, StandardCharsets.UTF_8);
                        processReceivedData(data);
                    }
                }
            });
            
            System.out.println("[扫码枪] 串口连接成功，等待扫码数据...");
            
        } catch (Exception e) {
            // 发生异常，清理已打开的串口
            cleanup();
            throw e;
        }
    }
    
    /**
     * 处理接收到的数据
     * 扫码枪数据可能以换行符结尾，也可能没有换行符
     */
    private void processReceivedData(String data) {
        System.out.println("[扫码枪] 接收到原始数据: " + data.replace("\n", "\\n").replace("\r", "\\r"));
        
        boolean hasNewline = data.contains("\n");
        
        for (char c : data.toCharArray()) {
            if (c == '\n' || c == '\r') {
                // 取消延迟定时器
                synchronized (timerLock) {
                    if (delayTimer != null) {
                        delayTimer.cancel();
                        delayTimer = null;
                    }
                }
                
                String message = messageBuffer.toString().trim();
                if (!message.isEmpty() && messageHandler != null) {
                    String normalizedCode = extractCodeAfterLastBackslash(message);
                    if (normalizedCode != null) {
                        System.out.println("[扫码枪] 接收到完整数据: " + normalizedCode);
                        messageHandler.accept(normalizedCode);
                    } else {
                        System.out.println("[扫码枪] 丢弃数据（未找到反斜杠）: " + message);
                    }
                }
                messageBuffer.setLength(0);
            } else {
                messageBuffer.append(c);
            }
        }
        
        // 如果没有换行符，使用延迟机制处理数据（等待一段时间没有新数据时再处理）
        // 这样可以避免数据分多次接收时被拆分
        if (!hasNewline && messageBuffer.length() > 0) {
            synchronized (timerLock) {
                // 取消之前的定时器
                if (delayTimer != null) {
                    delayTimer.cancel();
                    delayTimer = null;
                }
                
                // 创建新的定时器，延迟100ms后处理数据
                delayTimer = new java.util.Timer(true);
                delayTimer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        synchronized (timerLock) {
                            if (messageBuffer.length() > 0) {
                                String message = messageBuffer.toString().trim();
                                if (!message.isEmpty() && messageHandler != null) {
                                    String normalizedCode = extractCodeAfterLastBackslash(message);
                                    if (normalizedCode != null) {
                                        System.out.println("[扫码枪] 接收到数据（延迟处理）: " + normalizedCode);
                                        messageHandler.accept(normalizedCode);
                                    } else {
                                        System.out.println("[扫码枪] 丢弃数据（未找到反斜杠）: " + message);
                                    }
                                }
                                messageBuffer.setLength(0);
                            }
                            delayTimer = null;
                        }
                    }
                }, 100); // 延迟100ms
            }
        }
        
        // 防止缓冲区过长
        if (messageBuffer.length() >= 1024) {
            synchronized (timerLock) {
                if (delayTimer != null) {
                    delayTimer.cancel();
                    delayTimer = null;
                }
            }
            String message = messageBuffer.toString().trim();
            if (!message.isEmpty() && messageHandler != null) {
                String normalizedCode = extractCodeAfterLastBackslash(message);
                if (normalizedCode != null) {
                    System.out.println("[扫码枪] 缓冲区过长，强制处理: " + normalizedCode);
                    messageHandler.accept(normalizedCode);
                } else {
                    System.out.println("[扫码枪] 丢弃数据（未找到反斜杠）: " + message);
                }
            }
            messageBuffer.setLength(0);
        }
    }

    /**
     * 扫码枪数据归一化：
     * 仅保留最后一个反斜杠 "\" 后的码值。
     * 若不存在反斜杠或反斜杠后为空，则返回 null（不处理该条数据）。
     */
    private String extractCodeAfterLastBackslash(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int idx = trimmed.lastIndexOf('\\');
        if (idx < 0 || idx >= trimmed.length() - 1) {
            return null;
        }
        String code = trimmed.substring(idx + 1).trim();
        return code.isEmpty() ? null : code;
    }
    
    /**
     * 发送命令到扫码枪（通过配置串口）
     * @param command 命令字符串
     */
    public void sendCommand(String command) {
        if (configPort != null && configPort.isOpen()) {
            try {
                byte[] data = command.getBytes(StandardCharsets.UTF_8);
                int bytesWritten = -1;
                boolean methodFound = false;
                
                // 使用反射查找并调用 writeBytes 方法（兼容不同版本的 jSerialComm 库）
                try {
                    // 尝试方法 1: writeBytes(byte[], int)
                    try {
                        Method method = SerialPort.class.getMethod("writeBytes", byte[].class, int.class);
                        bytesWritten = (int) method.invoke(configPort, data, data.length);
                        methodFound = true;
                    } catch (NoSuchMethodException e) {
                        // 方法不存在，继续尝试其他方法
                    }
                    
                    // 尝试方法 2: writeBytes(byte[], long)
                    if (!methodFound) {
                        try {
                            Method method = SerialPort.class.getMethod("writeBytes", byte[].class, long.class);
                            bytesWritten = (int) method.invoke(configPort, data, (long) data.length);
                            methodFound = true;
                        } catch (NoSuchMethodException e) {
                            // 方法不存在，继续尝试其他方法
                        }
                    }
                    
                    // 尝试方法 3: writeBytes(byte[])
                    if (!methodFound) {
                        try {
                            Method method = SerialPort.class.getMethod("writeBytes", byte[].class);
                            bytesWritten = (int) method.invoke(configPort, (Object) data);
                            methodFound = true;
                        } catch (NoSuchMethodException e) {
                            // 方法不存在
                        }
                    }
                    
                    if (!methodFound) {
                        System.err.println("[扫码枪] 无法找到任何可用的 writeBytes 方法");
                        if (errorHandler != null) {
                            errorHandler.accept("发送命令失败: 无法找到 writeBytes 方法");
                        }
                        return;
                    }
                    
                } catch (Exception e) {
                    System.err.println("[扫码枪] 反射调用 writeBytes 失败: " + e.getMessage());
                    e.printStackTrace();
                    if (errorHandler != null) {
                        errorHandler.accept("发送命令失败: " + e.getMessage());
                    }
                    return;
                }
                
                if (bytesWritten == data.length) {
                    System.out.println("[扫码枪] 发送命令成功: " + command + " (字节数=" + bytesWritten + ")");
                } else if (bytesWritten > 0) {
                    System.out.println("[扫码枪] 发送命令部分成功: " + command + " (期望" + data.length + "字节，实际" + bytesWritten + "字节)");
                } else {
                    System.err.println("[扫码枪] 发送命令失败: writeBytes 返回 " + bytesWritten);
                    if (errorHandler != null) {
                        errorHandler.accept("发送命令失败: writeBytes 返回 " + bytesWritten);
                    }
                }
            } catch (Exception e) {
                System.err.println("[扫码枪] 发送命令失败: " + e.getMessage());
                e.printStackTrace();
                if (errorHandler != null) {
                    errorHandler.accept("发送命令失败: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 关闭串口
     */
    public void disconnect() {
        running = false;
        cleanup();
        System.out.println("[扫码枪] 串口已关闭");
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        // 取消延迟定时器
        synchronized (timerLock) {
            if (delayTimer != null) {
                delayTimer.cancel();
                delayTimer = null;
            }
        }
        
        if (configPort != null && configPort.isOpen()) {
            configPort.removeDataListener();
            configPort.closePort();
            System.out.println("[扫码枪] 串口已关闭: " + configPortName);
        }
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return configPort != null && configPort.isOpen() && running;
    }
    
    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (isConnected()) {
            return "串口: " + configPortName + ", 波特率: " + configPort.getBaudRate();
        }
        return "未连接";
    }
}

