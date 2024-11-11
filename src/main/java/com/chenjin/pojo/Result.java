package com.chenjin.pojo;

import com.chenjin.constant.ResultCodeEnum;
import lombok.Builder;
import lombok.Data;

/**
 * 返回数据对象
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-05 19:26
 **/
@Data
@Builder
public class Result<T> {
    private int code;
    private String msg;
    private T data;
    public Result() {}
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
