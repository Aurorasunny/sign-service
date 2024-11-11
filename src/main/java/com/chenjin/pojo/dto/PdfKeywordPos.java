package com.chenjin.pojo.dto;

import lombok.Builder;
import lombok.Data;

/**
 * pdf关键词坐标对象
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-19 17:51
 **/
@Data
@Builder
public class PdfKeywordPos {
    /**
     * 关键词
     */
    private String keyword;
    /**
     * 所在页码
     */
    private Integer pageNo;
    /**
     * x坐标
     */
    private Float pageX;
    /**
     * y坐标
     */
    private Float pageY;

}
