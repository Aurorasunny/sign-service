package com.chenjin.common.oss.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

/**
 * 桶访问策略枚举
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-22 22:47
 **/
@Getter
@AllArgsConstructor
public enum AccessPolicyEnum {

    /**
     * private
     */
    PRIVATE("0", BucketCannedACL.PRIVATE, ObjectCannedACL.PRIVATE, PolicyEnum.WRITE),

    /**
     * public
     */
    PUBLIC("1", BucketCannedACL.PUBLIC_READ_WRITE, ObjectCannedACL.PUBLIC_READ_WRITE, PolicyEnum.READ_WRITE),

    /**
     * custom
     */
    CUSTOM("2", BucketCannedACL.PUBLIC_READ, ObjectCannedACL.PUBLIC_READ, PolicyEnum.READ);

    /**
     * 桶 权限类型（数据库值）
     */
    private final String type;

    /**
     * 桶 权限类型
     */
    private final BucketCannedACL bucketCannedACL;

    /**
     * 文件对象 权限类型
     */
    private final ObjectCannedACL objectCannedACL;

    /**
     * 桶策略类型
     */
    private final PolicyEnum policyEnum;

    public static AccessPolicyEnum getByType(String type) {
        for (AccessPolicyEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new RuntimeException("'type' not found By " + type);
    }
}
