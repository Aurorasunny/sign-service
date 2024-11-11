package com.chenjin.constant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 签章服务系统常量
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 15:11
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SignConstants {

    /**
     * 对象存储配置前缀
     */
    public final static String OSS_CONFIG_KEY_PREFIX = "sign-service:oss-config:";

    /**
     * 默认对象存储key
     */
    public final static String OSS_DEFAULT_KEY = OSS_CONFIG_KEY_PREFIX + "default";

    /**
     * 签章类型
     */
    @Getter
    public enum SIGN_TYPE {
        /**
         * 坐标
         */
        POS(0),
        /**
         * 关键词
         */
        KEYWORD(1);

        private Integer type;
        SIGN_TYPE(Integer type) {
            this.type = type;
        }
    }

}
