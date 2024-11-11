package com.chenjin.exception;

import java.io.Serial;

/**
 * 系统异常
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 15:08
 **/
public class SignException extends RuntimeException {
    @Serial
    private final static long serialVersionUID = 1L;

    public SignException(String message) {
        super(message);
    }
}
