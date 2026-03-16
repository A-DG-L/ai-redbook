package com.acorner.airedbook.common;

/**
 * 自定义业务异常，用于封装逻辑错误
 */
public class BusinessException extends RuntimeException {

    // 序列化版本号（可选，建议加上，IDEA 可以自动生成）
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}