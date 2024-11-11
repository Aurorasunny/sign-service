package com.chenjin.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * 签章参数
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-09-28 20:32
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignParams implements Serializable {
    @Serial
    private final static long serialVersionUID = 1L;
    /**
     * 签章用户
     */
    @NotEmpty(message = "签章用户不能为空")
    private String signUsername;
    /**
     * 签章类型（0:坐标签章，1:关键词签章）
     */
    @NotNull(message = "签章类型不能为空")
    private Integer signType;
    /**
     * 坐标签章位置（eg: 1)pdf:1,100,200->第一页左下角偏上100像素，偏右200像素；2) img:1,100,200/100,200->多张的时候需填写第几页，并强制合并）
     */
    private String pos;
    /**
     * 关键词
     */
    private String keyword;
    /**
     * 关键词出现多次的时候是否签多个地方（0-否/1-是，null默认为否且取最后一个，为否时出现多次进行报错）
     */
    private Integer multiPos;
    /**
     * 签章pdf文件编码
     */
    private String pdfFileCode;
    /**
     * 图片文件列表
     */
    private String imgFileCodes;
    /**
     * 多个图片时是否合并为一张（默认否打包成zip，1为是）
     */
    private Integer imgMergeFlag;
    /**
     * 签章印章文件编码
     */
    private String sealFileCode;
    /**
     * 是否需要进行数字签名（默认否，1为是）
     */
    private Integer signatureFlag;
    /**
     * 证书文件编码（内部文件）
     */
    private String certFileCode;
}
