package com.chenjin.service.image;

import com.chenjin.exception.SignException;
import com.chenjin.pojo.dto.SignParams;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 图片签章服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-13 23:36
 **/
public interface ImageSignService {
    /**
     * 对输入流的图片进行签章
     * @param signParams 签章参数
     * @param imgIsArr 图片输入流数组
     * @param sealIs 印章输入流
     * @return 签章后输出流
     * @throws SignException 签章异常
     */
    ByteArrayOutputStream signImg(SignParams signParams, InputStream[] imgIsArr, String[] fileNames, InputStream sealIs) throws SignException;

    /**
     * 对输入流的图片进行签章（内部文件）
     * @param signParams 签章参数
     * @return 签章后输出流
     * @throws SignException 签章异常
     */
    ByteArrayOutputStream signImg(SignParams signParams) throws SignException;
}
