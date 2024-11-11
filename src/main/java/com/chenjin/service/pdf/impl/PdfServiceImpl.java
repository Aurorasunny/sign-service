package com.chenjin.service.pdf.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.chenjin.constant.SignConstants;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.FileStore;
import com.chenjin.pojo.bo.TextPos;
import com.chenjin.pojo.dto.PdfKeywordPos;
import com.chenjin.pojo.dto.SignParams;
import com.chenjin.service.file.FileService;
import com.chenjin.service.ocr.OcrService;
import com.chenjin.service.pdf.PdfService;
import com.chenjin.util.ImageUtils;
import com.chenjin.util.KeywordPositionFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.filetypedetector.FileType;
import org.apache.pdfbox.util.filetypedetector.FileTypeDetector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * pdf签章服务实现类
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-09-28 20:43
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final Validator validator;

    private final FileService fileService;

    private final ResourceLoader resourceLoader;

    private final OcrService ocrService;

    private PrivateKey privateKey;

    Certificate[] certificateChain;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 初始化私钥和证书信息
     */
    public void loadingSecretInfo(SignParams signParams) {
        try {
            if (null != this.privateKey && null != this.certificateChain) {
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
            this.certificateChain = keystore.getCertificateChain(alias);
        } catch (Exception e) {
            log.error("加载私钥信息失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteArrayOutputStream normalSignPdf(SignParams signParams, InputStream pdfIs, InputStream sealIs) throws SignException {
        return commonSignPdf(signParams, pdfIs, sealIs);
    }

    @Override
    public ByteArrayOutputStream normalSignPdf(SignParams signParams) throws SignException {
        return commonSign(signParams);
    }

    /**
     * 通用pdf文件签章
     *
     * @param signParams 签章参数
     * @param pdfIs      待签pdf文件
     * @param sealIs     印章信息
     */
    private ByteArrayOutputStream commonSignPdf(SignParams signParams, InputStream pdfIs, InputStream sealIs) {
        InputStream[] verifiedIs = paramVerify(signParams, pdfIs, sealIs);
        pdfIs = verifiedIs[0];
        sealIs = verifiedIs[1];
        // 印章抠图
        ByteArrayOutputStream sealOs = ImageUtils.matting(sealIs);
        sealIs = new ByteArrayInputStream(sealOs.toByteArray());
        // 获取签章方式
        boolean encipherFlag = null != signParams.getSignatureFlag() && 1 == signParams.getSignatureFlag();
        try (PDDocument doc = PDDocument.load(pdfIs)) {
            if (null == doc) {
                throw new SignException("没有读取到待签章的pdf数据");
            }
            if (Objects.equals(signParams.getSignType(), SignConstants.SIGN_TYPE.POS.getType())) {
                if (StrUtil.isBlank(signParams.getPos())) {
                    throw new SignException("坐标不能为空");
                }
                // 坐标签章：切割字符串获取页面以及坐标信息
                String posStr = signParams.getPos();
                String[] posArr = posStr.split(",");
                if (3 != posArr.length) {
                    throw new SignException("坐标信息非法，请检查");
                }
                // 取出空格信息
                for (int i = 0; i < posArr.length; i++) {
                    posArr[i] = posArr[i].trim();
                }
                // 页码
                String pageNum = posArr[0];
                String pageX = posArr[1];
                String pageY = posArr[2];
                int totalPages = doc.getNumberOfPages();
                Integer pageIndex = Convert.toInt(pageNum, Integer.MAX_VALUE);
                if (pageIndex > totalPages) {
                    throw new SignException("签章页码非法");
                }
                PDPage page = doc.getPage(pageIndex - 1);
                if (null == page) {
                    throw new SignException("没有读取到待签章的pdf页面");
                }
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();
                Float signX = Convert.toFloat(pageX, Float.MAX_VALUE);
                Float signY = Convert.toFloat(pageY, Float.MAX_VALUE);
                if (signX > pageWidth || signY > pageHeight) {
                    throw new SignException("签章坐标非法");
                }
                if (encipherFlag) {
                    return encipherSign(signParams, doc, sealIs, pageIndex, signX, signY);
                } else {
                    return normalSign(doc, sealIs, page, signX, signY);
                }
            } else if (Objects.equals(signParams.getSignType(), SignConstants.SIGN_TYPE.KEYWORD.getType())) {
                // 关键字签章
                if (StrUtil.isEmpty(signParams.getKeyword())) {
                    throw new SignException("请输入关键词信息");
                }
                String keyword = signParams.getKeyword();
                if (StrUtil.isBlank(keyword)) {
                    throw new SignException("关键词签章时，关键词不能为空");
                }
                // 获取关键词坐标列表
                KeywordPositionFinder finder = new KeywordPositionFinder(keyword);
                finder.setSortByPosition(true);
                finder.setStartPage(0);
                int totalPageNum = doc.getNumberOfPages();
                finder.setEndPage(totalPageNum);
                finder.getText(doc);
                // 获取列表
                LinkedList<PdfKeywordPos> keywordPosList = finder.getKeywordPosList();
                if (CollUtil.isEmpty(keywordPosList)) {
                    // 直接检索为空的时候进行ocr检索
                    PDFRenderer renderer = new PDFRenderer(doc);
                    AtomicInteger index = new AtomicInteger(0);
                    for (int i = 0; i < totalPageNum; i++) {
                        index.set(i);
                        BufferedImage image = renderer.renderImageWithDPI(i, 300, ImageType.GRAY);
                        ByteArrayOutputStream imgOs = new ByteArrayOutputStream();
                        ImageIO.write(image, "jpg", imgOs);
                        List<TextPos> textPosList = ocrService.recognizeSegmentText(new ByteArrayInputStream(imgOs.toByteArray()), keyword);
                        if (CollUtil.isNotEmpty(textPosList)) {
                            List<PdfKeywordPos> tmpKeywordPosList = textPosList.stream().map(item -> PdfKeywordPos.builder()
                                    .keyword(keyword)
                                    .pageNo(index.intValue())
                                    .pageX(item.getXPos())
                                    .pageY(item.getYPos())
                                    .build()).toList();
                            keywordPosList.addAll(tmpKeywordPosList);
                        }
                    }
                }
                if (CollUtil.isEmpty(keywordPosList)) {
                    throw new SignException("没有获取到指定关键词，请检查");
                }
                // 判断是否是要多签
                Integer multiPos = signParams.getMultiPos();
                if (null == multiPos) {
                    // 取最后一个
                    PdfKeywordPos posInfo = keywordPosList.getLast();
                    PDPage page = doc.getPage(posInfo.getPageNo());
                    Float pageX = posInfo.getPageX();
                    Float pageY = posInfo.getPageY();
                    if (encipherFlag) {
                        return encipherSign(signParams, doc, sealIs, posInfo.getPageNo(), pageX, pageY);
                    } else {
                        return normalSign(doc, sealIs, page, pageX, pageY);
                    }
                } else if (0 == multiPos) {
                    if (keywordPosList.size() != 1) {
                        throw new SignException("存在多个关键词，请检查");
                    }
                    PdfKeywordPos posInfo = keywordPosList.getFirst();
                    PDPage page = doc.getPage(posInfo.getPageNo());
                    Float pageX = posInfo.getPageX();
                    Float pageY = posInfo.getPageY();
                    if (encipherFlag) {
                        return encipherSign(signParams, doc, sealIs, posInfo.getPageNo(), pageX, pageY);
                    } else {
                        return normalSign(doc, sealIs, page, pageX, pageY);
                    }
                } else if (1 == multiPos) {
                    // 多签-循环一个一个签
                    ByteArrayOutputStream pdfOs = new ByteArrayOutputStream();
                    PDDocument tmpDoc = doc;
                    for (PdfKeywordPos posInfo : keywordPosList) {
                        PDPage page = tmpDoc.getPage(posInfo.getPageNo());
                        Float pageX = posInfo.getPageX();
                        Float pageY = posInfo.getPageY();
                        ByteArrayOutputStream tempPdfOs = null;
                        if (encipherFlag) {
                            tempPdfOs = encipherSign(signParams, doc, sealIs, posInfo.getPageNo(), pageX, pageY);
                        } else {
                            tempPdfOs = normalSign(doc, sealIs, page, pageX, pageY);
                        }
                        ByteArrayInputStream tempIs = new ByteArrayInputStream(tempPdfOs.toByteArray());
                        tmpDoc = PDDocument.load(tempIs);
                    }
                    tmpDoc.save(pdfOs);
                    return pdfOs;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SignException(e.getMessage());
        }
        throw new SignException("签章失败，没有读取到签章文件");
    }

    /**
     * 通用pdf文件签章（内部文件）
     *
     * @param signParams 签章参数（包含签章相关文件的编码）
     */
    private ByteArrayOutputStream commonSign(SignParams signParams) {
        // 文件编码不能为空
        if (null == signParams) {
            throw new SignException("签章参数不能为空");
        }
        if (StrUtil.isEmpty(signParams.getPdfFileCode())) {
            throw new SignException("pdf文件编码不能为空");
        }
        if (StrUtil.isEmpty(signParams.getSealFileCode())) {
            throw new SignException("印章文件编码不能为空");
        }
        FileStore pdfFile = fileService.getFileStoreByFileCode(signParams.getPdfFileCode());
        InputStream pdfIs = fileService.downloadFile(pdfFile.getId());
        FileStore sealFile = fileService.getFileStoreByFileCode(signParams.getSealFileCode());
        InputStream sealIs = fileService.downloadFile(sealFile.getId());
        return commonSignPdf(signParams, pdfIs, sealIs);
    }

    /**
     * 普通签章
     *
     * @param doc    pdf对象
     * @param sealIs 印章文件
     * @param page   签章页面
     * @param signX  签章x坐标
     * @param signY  签章y坐标
     */
    private ByteArrayOutputStream normalSign(PDDocument doc, InputStream sealIs, PDPage page, Float signX, Float signY) throws IOException {
        // 创建签章图片
        // 限制签章文件大小
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        ByteArrayOutputStream resultOs = ImageUtils.scalePercent((int) Math.ceil(width * height), sealIs, 5);
        sealIs = new ByteArrayInputStream(resultOs.toByteArray());
        PDImageXObject sealImage = createSealImage(doc, sealIs);
        if (sealImage == null) {
            throw new SignException("不支持此印章信息");
        }
        try (PDPageContentStream pageContentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            BufferedImage sealBImage = ImageIO.read(new ByteArrayInputStream(resultOs.toByteArray()));
            int sealHeight = sealBImage.getHeight();
            int sealWidth = sealBImage.getWidth();
            pageContentStream.drawImage(sealImage, signX, signY, sealWidth, sealHeight);
        }
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        doc.save(result);
        return result;
    }

    /**
     * 通过签章输入流创建pdf图片对象
     *
     * @param sealIs 签章输入流
     * @return pdf图片对象
     */
    private PDImageXObject createSealImage(PDDocument doc, InputStream sealIs) {
        BufferedInputStream imageBuffers = null;
        FileType fileType = null;
        try {
            imageBuffers = IoUtil.toBuffered(sealIs);
            fileType = FileTypeDetector.detectFileType(imageBuffers);
            // 判断文件类型
            if (fileType == null) {
                throw new SignException("印章信息不正确");
            }
            if (FileType.JPEG.equals(fileType)) {
                return JPEGFactory.createFromStream(doc, imageBuffers);
            } else if (FileType.PNG.equals(fileType)) {
                BufferedImage image = ImageIO.read(imageBuffers);
                return LosslessFactory.createFromImage(doc, image);
            }
            return null;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new SignException(e.getMessage());
        } finally {
            IoUtil.close(imageBuffers);
        }
    }

    /**
     * 数字签名签章
     *
     * @param signParams 签章参数
     * @param doc        签章文件
     * @param sealIs     印章信息
     * @param pageNo     页码
     * @param signX      签章x坐标
     * @param signY      签章y坐标
     */
    private ByteArrayOutputStream encipherSign(SignParams signParams, PDDocument doc, InputStream sealIs, Integer pageNo, Float signX, Float signY) throws IOException {
        // 加载密钥信息
        loadingSecretInfo(signParams);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // 创建签名对象
        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE); // 使用 Adobe PPKLite 签名机制
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED); // 使用 PKCS#7 Detached 签名格式
        signature.setName("YanRui");
        signature.setLocation("HangZhou ZheJiang");
        signature.setReason("Is used to protect file from being modified");
        signature.setSignDate(Calendar.getInstance());

        // 限制签章文件大小
        PDPage page = doc.getPage(pageNo - 1);
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        ByteArrayOutputStream resultOs = ImageUtils.scalePercent((int) Math.ceil(width * height), sealIs, 5);
        sealIs = new ByteArrayInputStream(resultOs.toByteArray());

        // 剧中
        BufferedImage sealImage = ImageIO.read(new ByteArrayInputStream(resultOs.toByteArray()));
        int sealWidth = sealImage.getWidth();
        int sealHeight = sealImage.getHeight();
        signY = height - signY - (sealHeight / 2);
        // 设置签名外观
        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(doc, sealIs, pageNo);
        visibleSignDesigner.xAxis(signX).yAxis(signY).zoom(1).signatureFieldName("signature");

        // 自定义签名区域（可根据需要调整）
        visibleSignDesigner.width(sealWidth).height(sealHeight); // 设置签名框的宽度和高度

        PDVisibleSigProperties visibleSigProperties = new PDVisibleSigProperties();
        visibleSigProperties.setPdVisibleSignature(visibleSignDesigner)
                .page(pageNo)
                .visualSignEnabled(true) // 启用可视签名
                .buildSignature();

        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setVisualSignature(visibleSigProperties.getVisibleSignature());
        signatureOptions.setPage(pageNo - 1);

        doc.addSignature(signature, content -> {
            // 签名数据
            return signData(content, privateKey, certificateChain);
        }, signatureOptions);

        // 保存签名后的pdf
        doc.save(result);
        doc.close();
        return result;
    }

    /**
     * 签名数据
     *
     * @param content          待签数据
     * @param privateKey       私钥
     * @param certificateChain 证书链
     */
    public byte[] signData(InputStream content, PrivateKey privateKey, Certificate[] certificateChain) {
        byte[] signedData = null;
        try {
            // 读取输入流数据（PDF 内容）
            byte[] dataToSign = content.readAllBytes();

            // 创建签名者
            ContentSigner signer = new JcaContentSignerBuilder("SM3withSM2").build(privateKey);

            // 创建 CMSSignedDataGenerator 并添加证书
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                            .build(signer, (X509Certificate) certificateChain[0])
            );

            // 将证书链添加到签名生成器中
            List<Certificate> certList = new ArrayList<>(Arrays.asList(certificateChain));
            generator.addCertificates(new JcaCertStore(certList));

            // 生成签名数据
            CMSProcessableByteArray cmsData = new CMSProcessableByteArray(dataToSign);
            signedData = generator.generate(cmsData, false).getEncoded();
        } catch (Exception e) {
            log.error("签名失败", e);
            throw new RuntimeException(e);
        }
        return signedData;
    }

    /**
     * 验证流是否为pdf流
     *
     * @param pdfIs 待验证流数据
     * @return 是否为pdf流
     */
    private boolean verifyPdfIs(InputStream pdfIs) {
        try {
            // 读取头5个字节->pdf开头是"%PDF-"
            byte[] bytes = new byte[5];
            int readLength = pdfIs.read(bytes, 0, 5);
            if (readLength != 5) {
                return false;
            }
            String flagStr = new String(bytes);
            return flagStr.startsWith("%PDF-");
        } catch (IOException e) {
            log.error("判断失败", e);
            return false;
        }
    }

    /**
     * 验证签章相关参数
     *
     * @param signParams 签章参数
     * @param pdfIs      待签章pdf流数据
     * @param sealIs     印章数据
     */
    private InputStream[] paramVerify(SignParams signParams, InputStream pdfIs, InputStream sealIs) {
        Set<ConstraintViolation<SignParams>> errSet = validator.validate(signParams);
        if (!errSet.isEmpty()) {
            String errMsg = errSet.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("、"));
            throw new SignException(StrUtil.format("缺少{}必要参数，请检查", errMsg));
        }
        // 拷贝流数据用于验证
        ByteArrayOutputStream tmpPdfOs = new ByteArrayOutputStream();
        IoUtil.copy(pdfIs, tmpPdfOs);
        ByteArrayInputStream verifyPdfIs = new ByteArrayInputStream(tmpPdfOs.toByteArray());
        boolean verified = verifyPdfIs(verifyPdfIs);
        if (!verified) {
            throw new SignException("只支持pdf文件的签章");
        }
        pdfIs = new ByteArrayInputStream(tmpPdfOs.toByteArray());
        ByteArrayOutputStream tmpSealOs = new ByteArrayOutputStream();
        IoUtil.copy(sealIs, tmpSealOs);
        ByteArrayInputStream verifySealIs = new ByteArrayInputStream(tmpSealOs.toByteArray());
        verified = ImageUtils.isJpgOrPng(verifySealIs);
        if (!verified) {
            throw new SignException("只支持png/jpg格式的印章数据");
        }
        sealIs = new ByteArrayInputStream(tmpSealOs.toByteArray());
        return new InputStream[]{pdfIs, sealIs};
    }

}
