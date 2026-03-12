package com.miduo.cloud.controller.filter;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 捕获“客户端在响应写完前断开连接”导致的异常，仅打 DEBUG 日志，避免堆栈刷屏。
 * 常见于前端快速切换页面、关闭弹窗或超时断开时。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientAbortExceptionFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ClientAbortExceptionFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            if (isClientAbort(e)) {
                if (request instanceof HttpServletRequest) {
                    HttpServletRequest req = (HttpServletRequest) request;
                    log.debug("客户端断开连接，忽略: {} {}", req.getMethod(), req.getRequestURI());
                } else {
                    log.debug("客户端断开连接，忽略: {}", e.getMessage());
                }
                return;
            }
            throw e;
        }
    }

    private static boolean isClientAbort(Throwable t) {
        if (t instanceof ClientAbortException) {
            return true;
        }
        if (t instanceof IOException) {
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                // 你的主机中的软件中止了一个已建立的连接 / Connection reset / Broken pipe
                if (lower.contains("中止") || lower.contains("connection reset")
                        || lower.contains("broken pipe") || lower.contains("connection closed")) {
                    return true;
                }
            }
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isClientAbort(cause);
    }
}
