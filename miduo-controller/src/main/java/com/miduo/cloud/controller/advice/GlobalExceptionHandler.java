package com.miduo.cloud.controller.advice;

import com.miduo.cloud.common.dto.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理控制器层抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        e.printStackTrace();
        return ApiResult.error("系统异常: " + e.getMessage());
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResult.error("参数错误: " + e.getMessage());
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResult<Void> handleNullPointerException(NullPointerException e) {
        e.printStackTrace();
        return ApiResult.error("空指针异常，请联系管理员");
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResult<Void> handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        return ApiResult.error("运行时异常: " + e.getMessage());
    }
}

