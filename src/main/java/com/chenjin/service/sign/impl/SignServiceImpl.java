package com.chenjin.service.sign.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrSplitter;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.FileStore;
import com.chenjin.pojo.bo.SignPair;
import com.chenjin.pojo.dto.SignParams;
import com.chenjin.service.file.FileService;
import com.chenjin.service.image.ImageSignService;
import com.chenjin.service.pdf.PdfService;
import com.chenjin.service.sign.SignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 签章服务
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-11-06 22:29
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class SignServiceImpl implements SignService {

    private final PdfService pdfService;

    private final ImageSignService imageSignService;

    private final FileService fileService;

    @Override
    public void signFile(SignParams signParams, MultipartFile[] signIsArr, MultipartFile sealIs, HttpServletResponse response) {
        if (null == signIsArr || null == sealIs) {
            throw new SignException("请上传正确的文件");
        }
        // 判断待签文件类型
        try {
            InputStream[] signIs = new InputStream[signIsArr.length];
            String[] fileNames = new String[signIsArr.length];
            SignPair<Integer, Boolean> pdfFlag = SignPair.of(signIsArr.length + 1, false);
            for (int i = 0; i < signIsArr.length; i++) {
                signIs[i] = signIsArr[i].getInputStream();
                fileNames[i] = signIsArr[i].getOriginalFilename();
                if ("application/pdf".equals(signIsArr[i].getContentType())) {
                    pdfFlag.setKey(i);
                    pdfFlag.setValue(true);
                    break;
                }
            }
            ByteArrayOutputStream resultOs = null;
            if (pdfFlag.getValue()) {
                Integer key = pdfFlag.getKey();
                resultOs = pdfService.normalSignPdf(signParams, signIs[key], sealIs.getInputStream());
                response.setContentType("application/pdf");
                // 文件名称
                String fileName = fileNames[key];
                fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            } else {
                resultOs = imageSignService.signImg(signParams, signIs, fileNames, sealIs.getInputStream());
                String fileName = fileNames[0];
                String[] fileNameArr = fileName.split("\\.");
                if (null != signParams.getImgMergeFlag() && 0 == signParams.getImgMergeFlag()) {
                    // zip流
                    fileName = fileNameArr[0] + "等" + fileNames.length + "个签章文件" + ".zip";
                    fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    response.setContentType("application/zip");
                } else {
                    fileName = fileNameArr[0] + "-签章文件." + fileNameArr[1];
                    fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    response.setContentType("image/jpeg");
                }
            }
            // 将结果进行返回
            if (null == resultOs || resultOs.size() == 0) {
                throw new SignException("签章失败，请稍后重试");
            }
            ServletOutputStream outputStream = response.getOutputStream();
            IoUtil.copy(new ByteArrayInputStream(resultOs.toByteArray()), outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SignException("签章失败：" + e.getMessage());
        }
    }

    @Override
    public void signFile(SignParams signParams, HttpServletResponse response) {
        try {
            // 获取文件
            if (null == signParams) {
                throw new SignException("签章参数不合规，请检查后重试");
            }
            ByteArrayOutputStream resultOs = null;
            if (null != signParams.getPdfFileCode()) {
                // pdf类型
                FileStore fileStore = fileService.getFileStoreByFileCode(signParams.getPdfFileCode());
                if (null == fileStore) {
                    throw new SignException("没有查询呢到待签章的文件，请检查");
                }
                if (!"application/pdf".equals(fileStore.getMimeType())) {
                    throw new SignException("待签文件格式不正确，请检查");
                }
                resultOs = pdfService.normalSignPdf(signParams);
                response.setContentType("application/pdf");
                String fileName = fileStore.getFileName();
                fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
            } else {
                // 获取所有的图片
                String[] imgCodes = StrSplitter.split(signParams.getImgFileCodes(), ",", true, true).toArray(new String[0]);
                List<FileStore> imgFiles = fileService.getFilesStoreByFileCodes(imgCodes);
                if (CollUtil.isEmpty(imgFiles)) {
                    throw new SignException("没有查询到待签章的文件，请检查");
                }
                String fileName = imgFiles.get(0).getFileName();
                String[] fileNameArr = fileName.split("\\.");
                if (null != signParams.getImgMergeFlag() && 0 == signParams.getImgMergeFlag()) {
                    // zip流
                    String fileNum = Convert.digitToChinese(imgFiles.size());
                    fileName = fileNameArr[0] + "等" + fileNum + "个签章文件" + fileNameArr[1];
                    fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    response.setContentType("application/zip");
                } else {
                    fileName = fileNameArr[0] + "-签章文件" + fileNameArr[1];
                    fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                    response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                    response.setContentType("image/jpeg");
                }
                resultOs = imageSignService.signImg(signParams);
            }
            // 将结果进行返回
            if (null == resultOs || resultOs.size() == 0) {
                throw new SignException("签章失败，请稍后重试");
            }
            ServletOutputStream outputStream = response.getOutputStream();
            IoUtil.copy(new ByteArrayInputStream(resultOs.toByteArray()), outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            throw new SignException("签章失败：" + e.getMessage());
        }
    }
}
