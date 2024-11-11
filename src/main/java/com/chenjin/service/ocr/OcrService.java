package com.chenjin.service.ocr;

import com.chenjin.pojo.bo.TextPos;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * ocr服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 16:13
 **/
public interface OcrService {

    /**
     * 识别文字全文
     * @param imgIs 图片流数据
     */
    String recognizeFullText(InputStream imgIs);

    /**
     * 识别文字片段坐标（左下角开始）
     * @param imgIs 图片流数据
     * @param text 文字片段
     */
    List<TextPos> recognizeSegmentText(InputStream imgIs, String text);

}
