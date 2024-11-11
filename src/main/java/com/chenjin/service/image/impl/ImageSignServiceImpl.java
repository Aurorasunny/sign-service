package com.chenjin.service.image.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.chenjin.constant.SignConstants;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.FileStore;
import com.chenjin.pojo.bo.SignPair;
import com.chenjin.pojo.bo.TextPos;
import com.chenjin.pojo.dto.SignParams;
import com.chenjin.service.file.FileService;
import com.chenjin.service.image.ImageSignService;
import com.chenjin.service.ocr.OcrService;
import com.chenjin.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoBytes;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片签章服务实现类
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 20:48
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageSignServiceImpl implements ImageSignService {

    private final OcrService ocrService;

    private final FileService fileService;

    private final Validator validator;

    private PrivateKey privateKey;

    private final ResourceLoader resourceLoader;

    /**
     * 初始化私钥和证书信息
     */
    public void loadingSecretInfo(SignParams signParams) {
        try {
            if (null != this.privateKey) {
                return;
            }
            // 加载 PKCS#12 (.p12) 文件以获取私钥和证书链
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            char[] password = "Pa$$w0rd".toCharArray();
            // 判断密钥文件code是否为空
            InputStream keystoreStream = null;
            if (StrUtil.isNotEmpty(signParams.getCertFileCode())) {
                FileStore fileStore = fileService.getFileStoreByFileCode(signParams.getCertFileCode());
                if (null != fileStore) {
                    keystoreStream = fileService.downloadFile(fileStore.getId());
                }
            }
            if (null == keystoreStream) {
                Resource secretResource = resourceLoader.getResource("classpath:secretfile/sign-pdf.jks");
                keystoreStream = secretResource.getInputStream();
            }
            try {
                keystore.load(keystoreStream, password);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                IoUtil.close(keystoreStream);
            }
            // 获取私钥和证书链
            String alias = keystore.aliases().nextElement();
            this.privateKey = (PrivateKey) keystore.getKey(alias, password);
        } catch (Exception e) {
            log.error("加载私钥信息失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteArrayOutputStream signImg(SignParams signParams, InputStream[] imgIsArr, String[] fileNames, InputStream sealIs) throws SignException {
        return commonSignImg(signParams, imgIsArr, fileNames, sealIs);
    }

    @Override
    public ByteArrayOutputStream signImg(SignParams signParams) throws SignException {
        // 获取相关文件
        if (StrUtil.isEmpty(signParams.getImgFileCodes())) {
            throw new SignException("图片编码不能为空");
        }
        if (StrUtil.isEmpty(signParams.getSealFileCode())) {
            throw new SignException("印章文件编码不能为空");
        }
        String[] imgFileCodes = StrSplitter.split(signParams.getImgFileCodes(), ",", true, true).toArray(new String[0]);
        InputStream[] imgIsArr = new ByteArrayInputStream[imgFileCodes.length];
        String[] fileNames = new String[imgFileCodes.length];
        for (int i = 0; i < imgFileCodes.length; i++) {
            String imgFileCode = imgFileCodes[i];
            FileStore imgFile = fileService.getFileStoreByFileCode(imgFileCode);
            InputStream imgIs = fileService.downloadFile(imgFile.getId());
            byte[] imgBytes = IoUtil.readBytes(imgIs);
            imgIsArr[i] = new ByteArrayInputStream(imgBytes);
            fileNames[i] = imgFile.getFileName();
        }
        FileStore sealFile = fileService.getFileStoreByFileCode(signParams.getSealFileCode());
        InputStream sealIs = fileService.downloadFile(sealFile.getId());
        return commonSignImg(signParams, imgIsArr, fileNames, sealIs);
    }

    /**
     * 通用图片签章
     */
    private ByteArrayOutputStream commonSignImg(SignParams signParams, InputStream[] imgIsArr, String[] fileNames, InputStream sealIs) throws SignException {
        // 验证参数
        Set<ConstraintViolation<SignParams>> errInfo = validator.validate(signParams);
        if (CollUtil.isNotEmpty(errInfo)) {
            String errMsg = errInfo.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("、"));
            throw new SignException(StrUtil.format("缺少{}必要参数，请检查", errMsg));
        }
        if (null == imgIsArr) {
            throw new SignException("签章文件为空，请检查");
        }
        if (null == sealIs) {
            throw new SignException("印章文件为空，请检查");
        }
        // 验证印章
        ByteArrayOutputStream sealOs = new ByteArrayOutputStream();
        IoUtil.copy(sealIs, sealOs);
        boolean sealVerified = ImageUtils.isJpgOrPng(new ByteArrayInputStream(sealOs.toByteArray()));
        if (!sealVerified) {
            throw new SignException("只支持jpg/png格式的印章文件");
        }
        // 验证图片格式以及印章是否超过图片的1/3
        ByteArrayOutputStream[] imgOsArr = new ByteArrayOutputStream[imgIsArr.length];
        for (int i = 0; i < imgIsArr.length; i++) {
            imgOsArr[i] = new ByteArrayOutputStream();
            IoUtil.copy(imgIsArr[i], imgOsArr[i]);
            boolean verified = ImageUtils.isJpgOrPng(new ByteArrayInputStream(imgOsArr[i].toByteArray()));
            if (!verified) {
                throw new SignException("只支持jpg/png格式的文件签章");
            }
        }
        // 返回数据类型
        ByteArrayOutputStream resultOs = new ByteArrayOutputStream();
        // 是否合并
        boolean mergeFlag = false;
        Integer mergeFlagParam = signParams.getImgMergeFlag();
        if (mergeFlagParam != null && mergeFlagParam == 1) {
            mergeFlag = true;
        }
        // 判断签章类型
        if (Objects.equals(signParams.getSignType(), SignConstants.SIGN_TYPE.POS.getType())) {
            // 固定位置签章
            Integer pageNo = null;
            String posStr = signParams.getPos();
            if (StrUtil.isEmpty(posStr)) {
                throw new SignException("签章位置不能为空");
            }
            String[] posArr = posStr.split(",");
            for (int i = 0; i < posArr.length; i++) {
                posArr[i] = posArr[i].trim();
            }
            Integer x = null;
            Integer y = null;
            // 判断数量
            if (posArr.length < 2) {
                throw new SignException("非法签章坐标，请检查!");
            }
            x = Integer.parseInt(posArr[0]);
            y = Integer.parseInt(posArr[1]);
            if (posArr.length == 3) {
                // 当坐标中含有页数的时候判断是否超出了签章文件的数量
                pageNo = Integer.parseInt(posArr[0]);
                if (pageNo > imgIsArr.length) {
                    pageNo = null;
                }
                x = Integer.parseInt(posArr[1]);
                y = Integer.parseInt(posArr[2]);
            }
            // 叠加图片
            if (pageNo != null) {
                try {
                    ByteArrayOutputStream srcImgOs = imgOsArr[pageNo - 1];
                    BufferedImage srcImg = ImageIO.read(new ByteArrayInputStream(srcImgOs.toByteArray()));
                    int imgHeight = srcImg.getHeight();
                    y = imgHeight - y;
                    imgOsArr[pageNo - 1] = ImageUtils.overlayImg(imgOsArr[pageNo - 1], sealOs, x, y);
                } catch (IOException e) {
                    throw new SignException("签章失败，没有读取到待签文件");
                }
            } else {
                // 每一张都签章
                try {
                    int tmpY = y;
                    for (int i = 0; i < imgOsArr.length; i++) {
                        ByteArrayOutputStream srcImgOs = imgOsArr[i];
                        BufferedImage srcImg = ImageIO.read(new ByteArrayInputStream(srcImgOs.toByteArray()));
                        int imgHeight = srcImg.getHeight();
                        y = imgHeight - tmpY;
                        imgOsArr[i] = ImageUtils.overlayImg(imgOsArr[i], sealOs, x, y);
                    }
                } catch (Exception e) {
                    throw new SignException("签章失败，没有读取到待签文件");
                }
            }
        } else if (Objects.equals(signParams.getSignType(), SignConstants.SIGN_TYPE.KEYWORD.getType())) {
            // 判断关键词是否为空
            if (StrUtil.isEmpty(signParams.getKeyword())) {
                throw new SignException("关键词不能为空，请检查");
            }
            for (int i = 0; i < imgOsArr.length; i++) {
                List<TextPos> textPos =
                        ocrService.recognizeSegmentText(new ByteArrayInputStream(imgOsArr[i].toByteArray()), signParams.getKeyword());
                if (CollUtil.isEmpty(textPos)) {
                    continue;
                }
                // 获取长宽信息
                SignPair<Float, Float> imgWH = ImageUtils.calcWH(new ByteArrayInputStream(imgOsArr[i].toByteArray()));
                Float height = imgWH.getValue();
                for (TextPos textPo : textPos) {
                    Float x = textPo.getXPos();
                    Float y = textPo.getYPos();
                    if (x == null || y == null) {
                        continue;
                    }
                    y = height - y;
                    imgOsArr[i] = ImageUtils.overlayImg(imgOsArr[i], sealOs, x.intValue(), y.intValue());
                }
            }
        }
        // 判断是否需要数字签名
        boolean signatureFlag = signParams.getSignatureFlag() != null && 1 == signParams.getSignatureFlag();
        if (signatureFlag) {
            // 加载密钥信息
            this.loadingSecretInfo(signParams);
        }
        // 判断是否需要合并
        if (mergeFlag) {
            resultOs = ImageUtils.verticalStacked(imgOsArr);
            if (resultOs.size() == 0) {
                throw new SignException("叠加图片失败，自动转为zip格式");
            }
            if (signatureFlag) {
                resultOs = signatureImage(new ByteArrayInputStream(resultOs.toByteArray()));
            }
        } else {
            // 不合并，判断数量，大于1打包为zip
            if (imgOsArr.length > 1) {
                ByteArrayInputStream[] tmpIsArr = new ByteArrayInputStream[imgOsArr.length];
                for (int i = 0; i < imgOsArr.length; i++) {
                    if (signatureFlag) {
                        imgOsArr[i] = signatureImage(new ByteArrayInputStream(imgOsArr[i].toByteArray()));
                    }
                    tmpIsArr[i] = new ByteArrayInputStream(imgOsArr[i].toByteArray());
                }
                ZipUtil.zip(resultOs, fileNames, tmpIsArr);
            } else {
                if (signatureFlag) {
                    resultOs = signatureImage(new ByteArrayInputStream(imgOsArr[0].toByteArray()));
                } else {
                    resultOs = imgOsArr[0];
                }
            }
        }
        return resultOs;
    }

    /**
     * 图片嵌入签名元数据
     */
    private ByteArrayOutputStream signatureImage(ByteArrayInputStream imgIs) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(this.privateKey);
            byte[] imgBytes = IoUtil.readBytes(imgIs);
            signature.update(imgBytes);
            byte[] signBytes = signature.sign();
            String signStr = Base64.getEncoder().encodeToString(signBytes);
            signBytes = signStr.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream resultOs = new ByteArrayOutputStream();

            ExifRewriter rewriter = new ExifRewriter();
            TiffOutputSet tiffOutputSet = new TiffOutputSet();
            TiffOutputDirectory directory = tiffOutputSet.getOrCreateExifDirectory();
            TagInfoBytes tagInfo = new TagInfoBytes("signature", Integer.MAX_VALUE, signBytes.length, TiffDirectoryType.EXIF_DIRECTORY_MAKER_NOTES);
            directory.add(tagInfo, signBytes);
            rewriter.updateExifMetadataLossless(imgBytes, resultOs, tiffOutputSet);

            return resultOs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
