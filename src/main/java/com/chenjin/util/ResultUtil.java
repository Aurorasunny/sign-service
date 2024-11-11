package com.chenjin.util;

import com.chenjin.constant.ResultCodeEnum;
import com.chenjin.pojo.Result;

/**
 * 返回值工具
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-07 16:37
 **/
public class ResultUtil {

    /**
     * 返回成功数据
     * @param data 返回数据
     * @return 封装结果对象
     * @param <T> 返回值类型
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ResultCodeEnum.SUCCESS.getCode(), msg, data);
    }

    /**
     * 返回成功数据
     */
    public static <T> Result<T> success(T data) {
        return success(ResultCodeEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 返回成功标识
     * @return 封装结果
     */
    public static <T> Result<T> success() {
        return success(ResultCodeEnum.SUCCESS.getMsg(), null);
    }

    /**
     * 返回失败对象
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResultCodeEnum.FAIL.getCode(), msg, null);
    }

    /**
     * 返回失败对象
     */
    public static <T> Result<T> fail() {
        return fail(ResultCodeEnum.FAIL.getMsg());
    }
}
