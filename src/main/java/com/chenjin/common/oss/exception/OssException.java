package com.chenjin.common.oss.exception;

import java.io.Serializable;

/**
 * oss异常封装
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-22 22:23
 **/
public class OssException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1L;

    public OssException(String message) {
        super(message);
    }
}
