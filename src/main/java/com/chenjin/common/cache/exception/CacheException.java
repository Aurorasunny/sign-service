package com.chenjin.common.cache.exception;

import java.io.Serial;

/**
 * 缓存异常
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 12:22
 **/
public class CacheException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CacheException(String message) {
        super(message);
    }
}
