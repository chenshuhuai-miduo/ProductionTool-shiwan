package com.miduo.cloud.frontend.service;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.lang.reflect.Method;

/**
 * 串口通信服务类
 * 负责串口连接、接收数据
 */
public class SerialConnectionService {
    
    private SerialPort serialPort;
    private volatile boolean running = false;
    private Consumer<String> messageHandler;
    private Consumer<String> errorHandler;
    private StringBuilder messageBuffer = new StringBuilder();
    
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
     * 打开串口（使用默认超时5秒）
     */
    public void connect(String portName, int baudRate) throws Exception {
        connect(portName, baudRate, 5000); // 默认5秒超时
    }
    
    /**
     * 打开串口（带超时配置）
     * @param portName 串口名称
     * @param baudRate 波特率
     * @param timeoutMs 读取超时时间（毫秒）
     */
    public void connect(String portName, int baudRate, int timeoutMs) throws Exception {
        if (isConnected()) {
            throw new Exception("串口已经打开");
        }
        
        System.out.println("[串口] 正在打开串口: " + portName + ", 波特率: " + baudRate + " (超时: " + timeoutMs + "ms)");
        
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        
        // 设置超时：TIMEOUT_READ_SEMI_BLOCKING模式，读超时时间
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0);
        
        if (!serialPort.openPort()) {
            throw new Exception("无法打开串口: " + portName);
        }
        
        System.out.println("[串口] 打开成功");
        running = true;
        
        // 添加数据监听器
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }
            
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;
                
                byte[] newData = new byte[serialPort.bytesAvailable()];
                int numRead = serialPort.readBytes(newData, newData.length);
                
                if (numRead > 0) {
                    String data = new String(newData, 0, numRead, StandardCharsets.UTF_8);
                    processReceivedData(data);
                }
            }
        });
        
        System.out.println("[串口] 监听器已添加");
    }
    
    /**
     * 处理接收到的数据
     */
    private void processReceivedData(String data) {
        System.out.println("[串口] 接收到数据: " + data.replace("\n", "\\n"));
        
        boolean hasNewline = data.contains("\n");
        
        for (char c : data.toCharArray()) {
            if (c == '\n') {
                String message = messageBuffer.toString().trim();
                if (!message.isEmpty() && messageHandler != null) {
                    messageHandler.accept(message);
                }
                messageBuffer.setLength(0);
            } else if (c != '\r') {
                messageBuffer.append(c);
            }
        }
        
        // 无换行符时立即输出
        if (!hasNewline && messageBuffer.length() > 0) {
            String message = messageBuffer.toString().trim();
            if (!message.isEmpty() && messageHandler != null) {
                messageHandler.accept(message);
                messageBuffer.setLength(0);
            }
        }
        
        // 防止缓冲区过长
        if (messageBuffer.length() >= 1024) {
            String message = messageBuffer.toString().trim();
            if (!message.isEmpty() && messageHandler != null) {
                messageHandler.accept(message);
            }
            messageBuffer.setLength(0);
        }
    }
    
    /**
     * 关闭串口
     */
    public void disconnect() {
        running = false;
        
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.removeDataListener();
            serialPort.closePort();
            System.out.println("[串口] 已关闭");
        }
    }
    
    /**
     * 检查串口是否已打开
     */
    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen() && running;
    }
    
    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (isConnected()) {
            return serialPort.getSystemPortName() + " @ " + serialPort.getBaudRate() + " bps";
        }
        return "未连接";
    }
    
    /**
     * 获取系统可用的串口列表
     */
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }
    
    /**
     * 向串口发送原始字节（用于需要严格 hex 协议的工业设备，如报警灯、剔除装置）
     *
     * @param data 要发送的字节数组
     * @return 发送成功返回 true
     */
    public boolean sendBytes(byte[] data) {
        if (!isConnected()) {
            System.err.println("[串口] 发送失败：串口未打开");
            if (errorHandler != null) errorHandler.accept("串口未打开，无法发送数据");
            return false;
        }
        try {
            int written = writeToPort(data);
            if (written == data.length) {
                System.out.printf("[串口] sendBytes 成功，字节数=%d%n", written);
                return true;
            }
            System.err.printf("[串口] sendBytes 不完整：期望%d，实际%d%n", data.length, written);
            return false;
        } catch (Exception e) {
            System.err.println("[串口] sendBytes 异常: " + e.getMessage());
            if (errorHandler != null) errorHandler.accept("发送字节异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 使用反射调用 jSerialComm 的 writeBytes 方法（兼容不同版本 API）
     */
    private int writeToPort(byte[] bytes) throws Exception {
        try {
            Method m = SerialPort.class.getMethod("writeBytes", byte[].class, long.class);
            return (int) m.invoke(serialPort, bytes, (long) bytes.length);
        } catch (NoSuchMethodException ignored) {}
        try {
            Method m = SerialPort.class.getMethod("writeBytes", byte[].class, int.class);
            return (int) m.invoke(serialPort, bytes, bytes.length);
        } catch (NoSuchMethodException ignored) {}
        Method m = SerialPort.class.getMethod("writeBytes", byte[].class);
        return (int) m.invoke(serialPort, (Object) bytes);
    }

    /**
     * 发送数据到串口
     * 
     * @param data 要发送的字符串数据
     * @return 发送成功返回true
     */
    public boolean sendData(String data) {
        if (!isConnected()) {
            System.err.println("[串口] 发送失败：串口未打开");
            if (errorHandler != null) {
                errorHandler.accept("串口未打开，无法发送数据");
            }
            return false;
        }
        
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            int bytesWritten = writeToPort(bytes);
            if (bytesWritten == bytes.length) {
                System.out.println("[串口] 发送数据成功: " + data + " (字节数=" + bytesWritten + ")");
                return true;
            }
            System.err.println("[串口] 发送数据不完整: 期望" + bytes.length + "字节，实际" + bytesWritten + "字节");
            return false;
        } catch (Exception e) {
            System.err.println("[串口] 发送数据异常: " + e.getMessage());
            if (errorHandler != null) {
                errorHandler.accept("发送数据异常: " + e.getMessage());
            }
            return false;
        }
    }
}

