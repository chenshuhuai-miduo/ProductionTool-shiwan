package com.miduo.cloud.frontend.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * TCP客户端服务类
 * 负责建立连接、接收消息和管理连接状态
 */
public class TcpConnectionService {
    
    private Socket socket;
    private InputStream inputStream;
    private Thread receiveThread;
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
     * 连接到TCP服务器（使用默认超时5秒）
     */
    public void connect(String host, int port) throws IOException {
        connect(host, port, 5000); // 默认5秒超时
    }
    
    /**
     * 连接到TCP服务器（带超时配置）
     * @param host 主机地址
     * @param port 端口号
     * @param timeoutMs 连接超时时间（毫秒）
     */
    public void connect(String host, int port, int timeoutMs) throws IOException {
        if (isConnected()) {
            throw new IOException("已经连接到服务器");
        }
        
        System.out.println("[TCP] 正在连接到 " + host + ":" + port + " (连接超时: " + timeoutMs + "ms)");
        
        // 使用带超时的连接方式
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        // 注意：不设置 setSoTimeout，避免读取时超时导致误断连
        // 连接超时用于快速失败，读取操作阻塞等待数据
        
        System.out.println("[TCP] Socket连接成功");
        
        inputStream = socket.getInputStream();
        running = true;
        
        // 启动接收消息的线程
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.setName("TCP-Receive-Thread");
        receiveThread.start();
        System.out.println("[TCP] 接收线程已启动");
    }
    
    /**
     * 接收消息的循环
     */
    private void receiveMessages() {
        System.out.println("[TCP] 接收消息线程已启动");
        byte[] buffer = new byte[1024];
        
        try {
            while (running) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    System.out.println("[TCP] 连接已关闭");
                    break;
                }
                
                if (bytesRead > 0) {
                    String data = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    System.out.println("[TCP] 接收到数据: " + data.replace("\n", "\\n"));
                    processReceivedData(data);
                }
            }
            
            // 输出缓冲区剩余数据
            if (messageBuffer.length() > 0) {
                String message = messageBuffer.toString().trim();
                if (!message.isEmpty() && messageHandler != null) {
                    messageHandler.accept(message);
                }
            }
        } catch (IOException e) {
            System.err.println("[TCP] 接收消息异常: " + e.getMessage());
            if (running && errorHandler != null) {
                errorHandler.accept("接收消息错误: " + e.getMessage());
            }
        } finally {
            if (running) {
                disconnect();
                if (errorHandler != null) {
                    errorHandler.accept("与服务器的连接已断开");
                }
            }
        }
    }
    
    /**
     * 处理接收到的数据
     */
    private void processReceivedData(String data) {
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
     * 断开连接
     */
    public void disconnect() {
        running = false;
        
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {}
        
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {}
        
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && running;
    }
    
    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (isConnected()) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
        return "未连接";
    }
}

