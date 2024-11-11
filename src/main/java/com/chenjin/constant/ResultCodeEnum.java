package com.chenjin.constant;

import lombok.Getter;

/**
 * 返回编码枚举
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-05 19:28
 **/
@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "请求成功"),
    FAIL(500, "服务器异常")
    ;

    private final int code;
    private final String msg;
    ResultCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
