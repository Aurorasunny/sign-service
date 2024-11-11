package com.chenjin.service.pdf;

import com.chenjin.exception.SignException;
import com.chenjin.pojo.dto.SignParams;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * pdf操作服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-13 23:11
 **/
public interface PdfService {
    /* 提供服务
        1、pdf指定位置签章
        2、pdf指定关键词位置签章
     */

    /**
     * 对输入流的pdf进行签章
     * @param signParams 签章参数
     * @param pdfIs pdf输入流
     * @param sealIs 印章输入流
     * @return 签章后的pdf输出流
     * @throws SignException 签章异常
     */
    ByteArrayOutputStream normalSignPdf(SignParams signParams, InputStream pdfIs, InputStream sealIs) throws SignException;

    /**
     * 对输入流的pdf进行签章（内部文件）
     * @param signParams 签章参数
     * @return 签章后的pdf输出流
     * @throws SignException 签章异常
     */
    ByteArrayOutputStream normalSignPdf(SignParams signParams) throws SignException;

}
