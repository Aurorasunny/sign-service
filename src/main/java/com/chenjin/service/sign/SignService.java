package com.chenjin.service.sign;

import com.chenjin.pojo.dto.SignParams;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 签章服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-11-06 21:33
 **/
public interface SignService {

    /**
     * 流数据签章
     * @param signParams 签章参数
     * @param signIs 待签文件
     * @param sealIs 印章文件
     * @param response 签后文件
     */
    void signFile(SignParams signParams, MultipartFile[] signIs, MultipartFile sealIs, HttpServletResponse response);

    /**
     * 内部文件签章
     * @param signParams 签章参数
     * @param response 签后文件
     */
    void signFile(SignParams signParams, HttpServletResponse response);


}
