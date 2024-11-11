package com.chenjin.common.upload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件上传校验注解
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 21:33
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UploadCheck {

    String[] excludeTypes() default {};

}
