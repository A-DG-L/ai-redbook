package com.acorner.airedbook.common.context;

/**
 * 基于 ThreadLocal 的用户上下文
 * 面试常问：为什么要用 remove()？答：防止 Tomcat 线程池复用导致的脏数据和内存泄漏。
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_THREAD_LOCAL.set(userId);
    }

    public static Long getUserId() {
        return USER_THREAD_LOCAL.get();
    }

    public static void remove() {
        USER_THREAD_LOCAL.remove();
    }
}