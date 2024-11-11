package com.chenjin.common.oss.properties;

import lombok.Data;

/**
 * oss对象存储属性
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-15 21:42
 **/
@Data
public class OssProperties {
    /**
     * 访问站点
     */
    private String endpoint;
    /**
     * 前缀
     */
    private String prefix;
    /**
     * 域名
     */
    private String region;
    /**
     * 自定义域名
     */
    private String domain;
    /**
     * 访问凭证
     */
    private String accessKey;
    /**
     * 访问密钥
     */
    private String secretKey;
    /**
     * 存储桶名称
     */
    private String bucketName;
    /**
     * 是否为https协议（Y/N）
     */
    private String isHttps;
    /**
     * 访问策略
     */
    private String accessPolicy;

}
