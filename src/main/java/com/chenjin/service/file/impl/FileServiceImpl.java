package com.chenjin.service.file.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenjin.common.enums.StoreTypeEnum;
import com.chenjin.common.oss.core.OssClient;
import com.chenjin.common.oss.entity.UploadResult;
import com.chenjin.common.oss.factory.OssFactory;
import com.chenjin.exception.SignException;
import com.chenjin.mapper.FileStoreMapper;
import com.chenjin.pojo.FileStore;
import com.chenjin.service.file.FileService;
import com.chenjin.util.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * 文件服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 15:41
 **/
@Service
public class FileServiceImpl extends ServiceImpl<FileStoreMapper, FileStore> implements FileService {

    @Override
    public FileStore saveFile(FileStore fileStore) {
        // 判断id是否存在
        if (fileStore.getId() == null) {
            save(fileStore);
        }
        updateById(fileStore);
        return fileStore;
    }

    @Override
    public FileStore uploadFileToOss(MultipartFile file) {
        // 获取oss客户端
        OssClient client = OssFactory.getInstance();
        String originalFilename = file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            byte[] fileBytes = inputStream.readAllBytes();
            UploadResult uploadResult = client.uploadWithSuffix(fileBytes, FileUtils.getSuffixWithPoint(originalFilename));
            if (null != uploadResult) {
                String path = uploadResult.getUrl();
                String ossKey = uploadResult.getFileName();
                // 保存数据库
                FileStore fileStore = FileStore.builder()
                        .fileCode(IdUtil.fastUUID())
                        .fileName(originalFilename)
                        .filePath(path)
                        .fileType(FileUtils.getSuffix(originalFilename))
                        .ossKey(ossKey)
                        .mimeType(FileUtils.getMimeType(originalFilename))
                        .storeType(StoreTypeEnum.OSS.name())
                        .build();
                save(fileStore);
                return fileStore;
            }
            return null;
        } catch (Exception e) {
            throw new SignException(StrUtil.format("上传文件失败，原因是：{}", e.getCause()));
        }
    }

    @Override
    public FileStore uploadFileToLocal(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new SignException("上传文件失败，请稍后重试");
        }
        File uploadFile = FileUtil.file("src/main/resources/upload/", originalFilename);
        try {
            file.transferTo(uploadFile);
        } catch (IOException e) {
            throw new SignException("上传文件失败，失败愿意是：" + e.getCause().getMessage());
        }
        // 构建返回对象
        FileStore fileStore = FileStore.builder()
                .fileCode(IdUtil.fastUUID())
                .fileName(originalFilename)
                .fileType(FileUtils.getSuffix(originalFilename))
                .storeType(StoreTypeEnum.LOCAL.name())
                .mimeType(FileUtils.getMimeType(originalFilename))
                .build();
        save(fileStore);
        return fileStore;
    }

    @Override
    public boolean deleteFile(Integer fileId) {
        FileStore fileStore = getById(fileId);
        if (null != fileStore) {
            // 判断是什么存储方式
            String storeType = fileStore.getStoreType();
            boolean success = false;
            if (StoreTypeEnum.LOCAL.name().equals(storeType)) {
                // 本地存储
                String filePath = fileStore.getFilePath();
                success = FileUtil.del(filePath);
            } else if (StoreTypeEnum.OSS.name().equals(storeType)) {
                String ossKey = fileStore.getOssKey();
                OssClient client = OssFactory.getInstance();
                client.delete(ossKey);
                success = true;
            }
            if (success) {
                removeById(fileId);
            }
            return success;
        }
        return false;
    }

    @Override
    public FileStore getFileById(Integer fileId) {
        return getById(fileId);
    }

    @Override
    public void downloadFile(Integer fileId, HttpServletResponse response) {
        FileStore fileStore = getFileById(fileId);
        if (null == fileStore) {
            throw new SignException("下载失败，找不到该文件");
        }
        String storeType = fileStore.getStoreType();
        if (StoreTypeEnum.LOCAL.name().equals(storeType)) {
            String filePath = fileStore.getFilePath();
            BufferedInputStream inputStream = FileUtil.getInputStream(filePath);
            try {
                response.setContentType(fileStore.getMimeType());
                String fileName = URLEncoder.encode(fileStore.getFileName(), StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");
                response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
                IoUtil.copy(inputStream, response.getOutputStream());
            } catch (IOException e) {
                throw new SignException("下载文件失败，失败原因是：" + e.getCause().getMessage());
            }
        } else if (StoreTypeEnum.OSS.name().equals(storeType)) {
            OssClient client = OssFactory.getInstance();
            String ossKey = fileStore.getOssKey();
            try {
                client.download(ossKey, response.getOutputStream());
            } catch (IOException e) {
                throw new SignException("下载文件失败，失败原因是：" + e.getCause().getMessage());
            }
        } else {
            throw new SignException("下载失败，找不到该文件");
        }
    }

    @Override
    public List<InputStream> downloadFiles(List<Integer> fileIds) {
        if (null == fileIds || fileIds.isEmpty()) {
            return null;
        }
        return fileIds.stream().map(this::getOssIs).toList();
    }

    @Override
    public InputStream downloadFile(Integer fileId) {
        return getOssIs(fileId);
    }

    @Override
    public FileStore getFileStoreByFileCode(String fileCode) {
        return getOne(Wrappers.lambdaQuery(FileStore.class).eq(FileStore::getFileCode, fileCode), false);
    }

    @Override
    public List<FileStore> getFilesStoreByFileCodes(String[] fileCodes) {
        if (null == fileCodes || fileCodes.length == 0) {
            return Collections.emptyList();
        }
        Set<String> fileCodeSet = Arrays.stream(fileCodes).collect(Collectors.toSet());
        LambdaQueryWrapper<FileStore> wrapper = Wrappers.lambdaQuery(FileStore.class)
                .in(FileStore::getFileCode, fileCodeSet);
        return list(wrapper);
    }

    @Override
    public void downloadFile(Dict params, HttpServletResponse response) {
        // 下载文件列表，打包zip
        if (params.containsKey("fileCodes")) {
            Object fileCodes = params.get("fileCodes");
            List<String> fileCodeList = Convert.toList(String.class, fileCodes);
            if (CollUtil.isNotEmpty(fileCodeList)) {
                // 下载列表
                List<Pair<Integer, String>> ids = fileCodeList.stream()
                        .filter(StrUtil::isNotBlank)
                        .map(item -> {
                            FileStore fileStore = getFileStoreByFileCode(item);
                            if (null != fileStore) {
                                return Pair.of(fileStore.getId(), fileStore.getFileName());
                            }
                            return null;
                        }).filter(Objects::nonNull)
                        .toList();
                if (CollUtil.isNotEmpty(ids)) {
                    // 构建压缩包格式
                    int length = ids.size();
                    String[] paths = new String[length];
                    InputStream[] zips = new InputStream[length];
                    int index = 0;
                    for (Pair<Integer, String> pair : ids) {
                        Integer fileId = pair.getKey();
                        InputStream inputStream = downloadFile(fileId);
                        if (null == inputStream) {
                            continue;
                        }
                        paths[index] = pair.getValue();
                        zips[index] = inputStream;
                        index++;
                    }
                    response.setContentType("application/zip");
                    response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode("文件列表") + ".zip");
                    try (ServletOutputStream outputStream = response.getOutputStream()) {
                        ZipOutputStream zipIs = new ZipOutputStream(outputStream, Charset.forName("GBK"));
                        ZipUtil.zip(zipIs, paths, zips);
                        return;
                    } catch (Exception e) {
                        throw new SignException("下载失败，请稍后重试");
                    }
                }
            }
        }
        // 单个文件下载
        if (params.containsKey("fileCode")) {
            Object fileCodeObj = params.get("fileCodes");
            String fileCode = Convert.toStr(fileCodeObj, "");
            FileStore fileStore = getFileStoreByFileCode(fileCode);
            if (null == fileStore) {
                return;
            }
            downloadFile(fileStore.getId(), response);
        }
    }

    /**
     * 获取oss对象的输入流
     * @param fileId 文件id
     * @return 数据流
     */
    private InputStream getOssIs(Integer fileId) {
        FileStore fileStore = getFileById(fileId);
        if (null == fileStore) {
            throw new SignException("下载失败，找不到该文件");
        }
        String storeType = fileStore.getStoreType();
        if (StoreTypeEnum.LOCAL.name().equals(storeType)) {
            String filePath = fileStore.getFilePath();
            return FileUtil.getInputStream(filePath);
        } else if (StoreTypeEnum.OSS.name().equals(storeType)) {
            OssClient client = OssFactory.getInstance();
            String ossKey = fileStore.getOssKey();
            try {
                return client.downloadFileIs(ossKey);
            } catch (Exception e) {
                throw new SignException("下载文件失败，失败原因是：" + e.getCause().getMessage());
            }
        } else {
            throw new SignException("下载失败，找不到该文件");
        }
    }
}
