package com.chenjin.common.oss.common;

/**
 * oss常量池
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-21 23:45
 **/
public interface OssConstants {
    /**
     * 云服务商
     */
    String[] CLOUD_SERVICE = new String[] {"aliyun", "qcloud", "qiniu", "obs"};
    /**
     * https 状态
     */
    String IS_HTTPS = "Y";
    /**
     * http协议头
     */
    String HTTP = "http://";
    /**
     * https协议头
     */
    String HTTPS = "https://";

}
