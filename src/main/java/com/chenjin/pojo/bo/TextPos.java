package com.chenjin.pojo.bo;

import lombok.Builder;
import lombok.Data;

/**
 * 图片文字坐标
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 16:39
 **/
@Data
@Builder
public class TextPos {
    /**
     * 文字
     */
    private String text;
    /**
     * x坐标
     */
    private Float xPos;
    /**
     * y坐标
     */
    private Float YPos;

}
