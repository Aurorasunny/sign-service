package com.chenjin.controller.sign;

import com.chenjin.pojo.dto.SignParams;
import com.chenjin.service.sign.SignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 签章接口
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-11-09 16:47
 **/
@RequiredArgsConstructor
@RestController
@RequestMapping("/sign")
public class SignController {

    private final SignService signService;

    /**
     * 上传文件签章
     * @param signFiles 待签文件
     * @param sealFile 印章/签字文件
     * @param signParams 签章参数
     */
    @PostMapping("signByUpload")
    public void signByUpload(@RequestParam("signFiles") MultipartFile[] signFiles,
                             @RequestParam("sealFile") MultipartFile sealFile,
                             @ModelAttribute SignParams signParams,
                             HttpServletResponse response) {
        signService.signFile(signParams, signFiles, sealFile, response);
    }

    /**
     * 内部文件签章
     * @param signParams 签章参数
     */
    @PostMapping("/signByFileCode")
    public void signByFileCode(@RequestBody SignParams signParams, HttpServletResponse response) {
        signService.signFile(signParams, response);
    }

}
