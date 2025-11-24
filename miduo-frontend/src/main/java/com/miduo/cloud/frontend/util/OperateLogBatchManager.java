package com.miduo.cloud.frontend.util;

import com.miduo.cloud.entity.po.OperateLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 操作日志批量管理器
 * 使用队列缓冲+定时批量保存的方式，避免大量异步任务导致CPU占用过高
 */
public class OperateLogBatchManager {
    
    private static final OperateLogBatchManager INSTANCE = new OperateLogBatchManager();
    
    /**
     * 日志队列（容量1万条，避免内存溢出）
     */
    private final BlockingQueue<OperateLog> logQueue = new LinkedBlockingQueue<>(10000);
    
    /**
     * 定时批量保存线程池（单线程）
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "OperateLogBatchSaver");
        thread.setDaemon(true);
        return thread;
    });
    
    /**
     * HTTP调用线程池（用于批量发送HTTP请求）
     */
    private final ExecutorService httpExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "OperateLogHttpSender");
        thread.setDaemon(true);
        return thread;
    });
    
    /**
     * 是否已启动
     */
    private volatile boolean started = false;
    
    /**
     * 批量大小（每次最多保存500条）
     */
    private static final int BATCH_SIZE = 500;
    
    /**
     * 批量保存间隔（秒）
     */
    private static final int BATCH_INTERVAL_SECONDS = 5;
    
    private OperateLogBatchManager() {
        // 私有构造函数
    }
    
    public static OperateLogBatchManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 启动批量保存任务
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        
        System.out.println("[操作日志批量管理器] 启动批量保存任务，间隔=" + BATCH_INTERVAL_SECONDS + "秒，批量大小=" + BATCH_SIZE);
        
        // 定时批量保存日志
        scheduler.scheduleWithFixedDelay(
            this::batchSaveLogs,
            BATCH_INTERVAL_SECONDS,  // 初始延迟
            BATCH_INTERVAL_SECONDS,  // 间隔
            TimeUnit.SECONDS
        );
        
        started = true;
    }
    
    /**
     * 停止批量保存任务
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        
        System.out.println("[操作日志批量管理器] 停止批量保存任务");
        
        // 先保存剩余日志
        batchSaveLogs();
        
        // 关闭线程池
        scheduler.shutdown();
        httpExecutor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!httpExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                httpExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            httpExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        started = false;
    }
    
    /**
     * 添加日志到队列（异步）
     * 
     * @param operateLog 操作日志
     * @return 是否成功添加
     */
    public boolean addLog(OperateLog operateLog) {
        if (operateLog == null) {
            return false;
        }
        
        boolean offered = logQueue.offer(operateLog);
        if (!offered) {
            System.err.println("[操作日志批量管理器] 日志队列已满，丢弃日志: " + operateLog.getOperateContent());
        }
        
        return offered;
    }
    
    /**
     * 批量保存日志
     */
    private void batchSaveLogs() {
        try {
            // 从队列中取出日志（最多BATCH_SIZE条）
            List<OperateLog> logs = new ArrayList<>(BATCH_SIZE);
            logQueue.drainTo(logs, BATCH_SIZE);
            
            if (logs.isEmpty()) {
                return;
            }
            
            System.out.println("[操作日志批量管理器] 开始批量保存 " + logs.size() + " 条日志");
            
            // 分批发送（每100条一批，避免HTTP请求过大）
            int subBatchSize = 100;
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < logs.size(); i += subBatchSize) {
                int end = Math.min(i + subBatchSize, logs.size());
                List<OperateLog> subBatch = logs.subList(i, end);
                
                // 异步发送HTTP请求
                Future<?> future = httpExecutor.submit(() -> sendLogsToBackend(subBatch));
                futures.add(future);
            }
            
            // 等待所有批次完成（最多等待30秒）
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    System.err.println("[操作日志批量管理器] 批量保存超时");
                    future.cancel(true);
                } catch (ExecutionException e) {
                    System.err.println("[操作日志批量管理器] 批量保存失败: " + e.getMessage());
                }
            }
            
            System.out.println("[操作日志批量管理器] 批量保存完成");
            
        } catch (InterruptedException e) {
            System.err.println("[操作日志批量管理器] 批量保存被中断: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[操作日志批量管理器] 批量保存异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送日志到后端
     * 
     * @param logs 日志列表
     */
    private void sendLogsToBackend(List<OperateLog> logs) {
        try {
            // 批量发送日志到后端
            for (OperateLog log : logs) {
                try {
                    String json = HttpUtil.getObjectMapper().writeValueAsString(log);
                    HttpUtil.doPost("/api/log/operate", json);
                } catch (Exception e) {
                    // 单条日志失败不影响其他日志
                    System.err.println("[操作日志批量管理器] 单条日志保存失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[操作日志批量管理器] 发送日志到后端失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取队列中待保存的日志数量
     * 
     * @return 队列大小
     */
    public int getQueueSize() {
        return logQueue.size();
    }
    
    /**
     * 立即保存所有待保存的日志（用于程序关闭时）
     */
    public void flush() {
        System.out.println("[操作日志批量管理器] 立即保存所有待保存的日志，队列大小=" + logQueue.size());
        batchSaveLogs();
    }
}

