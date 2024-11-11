package com.chenjin.common.oss.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * minio策略枚举
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-22 22:48
 **/
@Getter
@AllArgsConstructor
public enum PolicyEnum {
    /**
     * 只读
     */
    READ("read-only"),

    /**
     * 只写
     */
    WRITE("write-only"),

    /**
     * 读写
     */
    READ_WRITE("read-write");

    /**
     * 类型
     */
    private final String type;
}
