package com.chenjin.controller.file;

import cn.hutool.core.lang.Dict;
import com.chenjin.common.enums.StoreTypeEnum;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.FileStore;
import com.chenjin.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件接口
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 15:34
 **/
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件
     * @param file 文件流
     * @param storeType 存储方式
     * @return 上传结果
     */
    @PostMapping("/uploadFile")
    public FileStore uploadFile(@RequestParam MultipartFile file, @RequestParam String storeType) {
        if (StoreTypeEnum.LOCAL.name().equalsIgnoreCase(storeType)) {
            return fileService.uploadFileToLocal(file);
        } else if (StoreTypeEnum.OSS.name().equalsIgnoreCase(storeType)) {
            return fileService.uploadFileToOss(file);
        } else {
            throw new SignException("请输入正确的存储方式");
        }
    }

    /**
     * 下载文件（多个文件的时候打成压缩包）
     * @param param 文件参数（fileCodes/fileCode）
     * @param response 文件流
     */
    @PostMapping("/downloadFile")
    public void downloadFile(@RequestBody Dict param, HttpServletResponse response) {
        fileService.downloadFile(param, response);
    }

    /**
     * 通过id获取文件信息
     */
    @GetMapping("getById")
    public FileStore getById(@RequestParam Long id) {
        return fileService.getById(id);
    }

}
