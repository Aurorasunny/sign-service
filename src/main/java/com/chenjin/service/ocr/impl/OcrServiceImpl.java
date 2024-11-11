package com.chenjin.service.ocr.impl;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.opencv.OpenCVImageFactory;
import ai.djl.translate.TranslateException;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.bo.RotatedBox;
import com.chenjin.pojo.bo.SignPair;
import com.chenjin.pojo.bo.TextPos;
import com.chenjin.service.ocr.OcrService;
import com.chenjin.service.ocr.model.OcrV4Recognition;
import com.chenjin.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ocr服务实现类
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 16:44
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private final OcrV4Recognition recognition;

    @Override
    public String recognizeFullText(InputStream imgIs) {
        ByteArrayOutputStream tmpImgOs = new ByteArrayOutputStream();
        IoUtil.copy(imgIs, tmpImgOs);
        List<RotatedBox> imgTexts = convertImgToText(tmpImgOs);
        // 排序
        List<List<RotatedBox>> sortedBoxList = sortRotatedBox(imgTexts);
        // 拼接字符串
        StringBuilder fullText = new StringBuilder();
        for (List<RotatedBox> rotatedBoxes : sortedBoxList) {
            for (RotatedBox rotatedBox : rotatedBoxes) {
                String text = rotatedBox.getText();
                if (text.trim().isEmpty())
                    continue;
                fullText.append(text).append("\t");
            }
            fullText.append('\n');
        }
        return fullText.toString();
    }

    @Override
    public List<TextPos> recognizeSegmentText(InputStream imgIs, String text) {
        ByteArrayOutputStream tmpImgOs = new ByteArrayOutputStream();
        IoUtil.copy(imgIs, tmpImgOs);
        List<RotatedBox> imgTexts = convertImgToText(tmpImgOs);
        List<TextPos> textPosList = new ArrayList<>();
        for (RotatedBox imgText : imgTexts) {
            if (imgText.getText().equals(text)) {
                NDArray posArr = imgText.getBox();
                float[] floatArray = posArr.toFloatArray();
                float originalX = floatArray[4];
                float originalY = floatArray[5];
                TextPos textPos = TextPos.builder()
                        .text(imgText.getText())
                        .xPos(originalX)
                        .YPos(originalY)
                        .build();
                textPosList.add(textPos);
            }
        }
        // 转换坐标
        if (CollUtil.isNotEmpty(textPosList)) {
            // 计算图片长宽
            SignPair<Float, Float> wHInfo =
                    ImageUtils.calcWH(new ByteArrayInputStream(tmpImgOs.toByteArray()));
            Float height = wHInfo.getValue();
            textPosList.forEach(item -> {
                item.setYPos(Math.abs(height - item.getYPos()));
            });
            return textPosList;
        } else {
            throw new SignException("没有识别到指定关键词信息，请检查");
        }
    }

    /**
     * 获取图片中文字信息（分割的文字框）
     */
    private List<RotatedBox> convertImgToText(ByteArrayOutputStream imgOs) {
        boolean verified = ImageUtils.isJpgOrPng(new ByteArrayInputStream(imgOs.toByteArray()));
        if (!verified) {
            throw new SignException("非法图片格式，请检查");
        }
        try {
            Image image = OpenCVImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imgOs.toByteArray()));
            // 识别信息
            List<RotatedBox> detections = recognition.predict(image);
            if (CollUtil.isEmpty(detections)) {
                throw new SignException("识别失败，请确认图片是否正确后重试");
            }
            return detections;
        } catch (IOException e) {
            throw new SignException("读取图片失败：" + e.getMessage());
        } catch (TranslateException e) {
            log.error("图片识别失败：", e);
            throw new SignException("图片识别失败：" + e.getMessage());
        }
    }

    /**
     * 对识别的文字框进行排序（从上到下，从左到右）
     */
    private List<List<RotatedBox>> sortRotatedBox(List<RotatedBox> boxes) {
        // 先按照y进行排序
        Collections.sort(boxes);
        // 按照x进行排序
        List<List<RotatedBox>> lines = new ArrayList<>();
        List<RotatedBox> line = new ArrayList<>();
        RotatedBox firstBox = new RotatedBox(boxes.get(0).getBox(), boxes.get(0).getText());
        line.add(firstBox);
        lines.add(line);
        for (int i = 1; i < boxes.size(); i++) {
            RotatedBox tmpBox = new RotatedBox(boxes.get(i).getBox(), boxes.get(i).getText());
            float y1 = firstBox.getBox().toFloatArray()[1];
            float y2 = tmpBox.getBox().toFloatArray()[1];
            float dis = Math.abs(y2 - y1);
            if (dis < 20) { // 认为是同 1 行  - Considered to be in the same line
                line.add(tmpBox);
            } else { // 换行 - Line break
                firstBox = tmpBox;
                line.sort((o1, o2) -> {
                    NDArray leftBox = o1.getBox();
                    NDArray rightBox = o2.getBox();
                    float leftX = leftBox.toFloatArray()[0];
                    float rightX = rightBox.toFloatArray()[0];
                    return Float.compare(leftX, rightX);
                });
                line = new ArrayList<>();
                line.add(firstBox);
                lines.add(line);
            }
        }
        return lines;
    }
}
