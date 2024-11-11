package com.chenjin.common.oss.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 上传结果
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 00:02
 **/
@Data
@Builder
public class UploadResult {

    /**
     * oss对象路径
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 上传成功标记（用于文件校验）
     */
    private String eTag;

}
