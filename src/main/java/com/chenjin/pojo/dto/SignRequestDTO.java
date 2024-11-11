package com.chenjin.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;

/**
 * 签章请求参数
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-11-09 23:37
 **/
@EqualsAndHashCode(callSuper = false)
@Data
public class SignRequestDTO extends SignParams {
    /**
     * 待签章文件
     */
    private MultipartFile[] signFiles;

    /**
     * 印章/签名文件
     */
    private MultipartFile sealFile;
}
