package com.miduo.cloud.frontend.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * UDP客户端服务类
 * 负责接收UDP消息
 */
public class UdpConnectionService {
    
    private DatagramSocket socket;
    private Thread receiveThread;
    private volatile boolean running = false;
    private Consumer<String> messageHandler;
    private Consumer<String> errorHandler;
    private int port;
    
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
     * 连接到UDP服务器（开始监听指定端口）
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) {
            throw new IOException("已经在监听UDP消息");
        }
        
        this.port = port;
        
        System.out.println("[UDP] 正在创建UDP Socket，端口: " + port);
        socket = new DatagramSocket(port);
        System.out.println("[UDP] Socket创建成功");
        
        running = true;
        
        // 启动接收消息的线程
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.setName("UDP-Receive-Thread");
        receiveThread.start();
        System.out.println("[UDP] 接收线程已启动");
    }
    
    /**
     * 接收消息的循环
     */
    private void receiveMessages() {
        System.out.println("[UDP] 接收消息线程已启动");
        byte[] buffer = new byte[65535];
        
        try {
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                String senderAddress = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                
                System.out.println("[UDP] 接收到消息 from " + senderAddress + ": " + message);
                
                if (messageHandler != null) {
                    messageHandler.accept(message);
                }
            }
        } catch (IOException e) {
            if (running && errorHandler != null) {
                System.err.println("[UDP] 接收消息异常: " + e.getMessage());
                errorHandler.accept("接收消息错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        running = false;
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && running;
    }
    
    /**
     * 获取连接信息
     */
    public String getConnectionInfo() {
        if (isConnected()) {
            return "UDP监听端口:" + port;
        }
        return "未连接";
    }
}

