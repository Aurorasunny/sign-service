package com.chenjin.service.file;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chenjin.pojo.FileStore;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

/**
 * 文件服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-29 01:01
 **/
public interface FileService extends IService<FileStore> {

    /**
     * 保存文件对象
     *
     * @param fileStore 文件对象
     * @return 保存数据
     */
    FileStore saveFile(FileStore fileStore);

    /**
     * 上传文件
     *
     * @param file 文件流
     * @return 文件对象
     */
    FileStore uploadFileToOss(MultipartFile file);

    /**
     * 上传文件到本地
     *
     * @param file 文件流
     * @return 文件对象
     */
    FileStore uploadFileToLocal(MultipartFile file);

    /**
     * 删除文件
     *
     * @param fileId 文件id
     * @return 是否删除成功
     */
    boolean deleteFile(Integer fileId);

    /**
     * 通过id获取文件对象
     *
     * @param fileId 文件id
     * @return 文件对象
     */
    FileStore getFileById(Integer fileId);

    /**
     * 下载文件
     *
     * @param fileId   文件id
     * @param response 文件输出流
     */
    void downloadFile(Integer fileId, HttpServletResponse response);

    /**
     * 下载文件
     *
     * @param fileIds 文件ids
     * @return 输入流列表
     */
    List<InputStream> downloadFiles(List<Integer> fileIds);

    /**
     * 下载文件
     *
     * @param fileId 文件id
     * @return 输入流
     */
    InputStream downloadFile(Integer fileId);

    /**
     * 通过文件编码查询文件
     *
     * @param fileCode 文件编码
     * @return 文件数据
     */
    FileStore getFileStoreByFileCode(String fileCode);

    /**
     * 通过文件编码查询文件
     *
     * @param fileCodes 文件编码数组
     * @return 文件数据
     */
    List<FileStore> getFilesStoreByFileCodes(String[] fileCodes);

    /**
     * 下载文件（多个文件下载为压缩包）
     *
     * @param params   下载参数（fileCode/fileCodes）
     * @param response 文件流
     */
    void downloadFile(Dict params, HttpServletResponse response);
}
