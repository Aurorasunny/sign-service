package com.chenjin.common.upload;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.chenjin.exception.SignException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * 文件上传校验处理类
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-09-01 21:19
 **/
@Component
@Aspect
public class UploadCheckHandler {

    /**
     * 定义切点
     */
    @Pointcut("@annotation(com.chenjin.common.upload.UploadCheck)")
    public void pointCut() {}

    /**
     * 处理切点逻辑
     */
    @Around("pointCut()")
    public Object handler(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = Convert.convert(MethodSignature.class, signature);
        Method method = methodSignature.getMethod();
        UploadCheck check = method.getAnnotation(UploadCheck.class);
        if (check != null) {
            Object[] args = joinPoint.getArgs();
            // 获取文件上传流参数
            Optional<Object> uploadParam = Arrays.stream(args).filter(item -> item instanceof MultipartFile)
                    .findFirst();
            if (uploadParam.isPresent()) {
                // 获取上传参数
                MultipartFile multipartFile = Convert.convert(MultipartFile.class, uploadParam.get());
                // 存在文件上传流时进行校验
                String[] excludeTypes = check.excludeTypes();
                String originalFilename = multipartFile.getOriginalFilename();
                String suffix = FileUtil.getSuffix(originalFilename);
                boolean exist = ArrayUtil.containsAny(excludeTypes, suffix);
                if (!exist) {
                    throw new SignException("非法文件格式，不能上传");
                }
                joinPoint.proceed();
            }
        }
        return joinPoint.proceed();
    }
}
