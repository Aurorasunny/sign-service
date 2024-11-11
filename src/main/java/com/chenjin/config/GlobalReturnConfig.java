package com.chenjin.config;

import com.chenjin.pojo.Result;
import com.chenjin.util.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局返回配置
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-05 19:43
 **/
@Slf4j
@RestControllerAdvice
public class GlobalReturnConfig implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            return body;
        }
        return ResultUtil.success(body);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result<Object> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return ResultUtil.fail(ex.getMessage());
    }
}
