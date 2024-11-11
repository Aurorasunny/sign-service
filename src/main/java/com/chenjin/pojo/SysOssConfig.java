package com.chenjin.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * oss配置实体
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-15 21:53
 **/
@Data
@TableName("sys_oss_config")
public class SysOssConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 配置名
     */
    private String configKey;
    /**
     * 访问密钥
     */
    private String accessKey;
    /**
     * 密钥
     */
    private String secretKey;
    /**
     * 桶名称
     */
    private String bucketName;
    /**
     * 前缀
     */
    private String prefix;
    /**
     * 访问站点
     */
    private String endpoint;
    /**
     * 存储区域
     */
    private String region;
    /**
     * 自定义域名
     */
    private String domain;
    /**
     * 是否默认配置 0：否   1：是
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 是否为https协议（Y/N）
     */
    private String isHttps;
    /**
     * 访问策略
     */
    private String accessPolicy;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GTM%8")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GTM%8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;
}
