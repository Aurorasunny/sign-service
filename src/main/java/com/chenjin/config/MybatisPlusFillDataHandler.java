package com.chenjin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * mybatisPlus数据填充配置
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 14:28
 **/
@Configuration
public class MybatisPlusFillDataHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 插入填充
        this.strictInsertFill(metaObject, "insertDatetime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新填充
        this.strictUpdateFill(metaObject, "updateDatetime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
