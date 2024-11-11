package com.chenjin;

import cn.hutool.core.io.FileUtil;
import com.chenjin.common.oss.core.OssClient;
import com.chenjin.common.oss.entity.UploadResult;
import com.chenjin.common.oss.factory.OssFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import java.io.File;

/**
 * 签章服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-13 23:07
 **/
@SpringBootApplication
@EnableCaching
@MapperScan("com.chenjin.mapper")
public class SignServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignServiceApplication.class, args);
    }

}
