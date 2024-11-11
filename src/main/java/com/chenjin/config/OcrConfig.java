package com.chenjin.config;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.opencv.OpenCVImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.chenjin.exception.SignException;
import com.chenjin.service.ocr.model.OcrV4Recognition;
import com.chenjin.service.ocr.translator.OCRDetectionTranslator;
import com.chenjin.service.ocr.translator.PpWordRecTranslator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ocr相关bean的自动加载
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-27 18:13
 **/
@Configuration
public class OcrConfig {

    /**
     * 文本框检测模型
     */
    @Bean
    public ZooModel<Image, NDList> detModel() {
        ZooModel<Image, NDList> detModel = null;
        try {
            ClassLoader classLoader = OcrConfig.class.getClassLoader();
            URL modelUrl = classLoader.getResource("models/det_inference.onnx");
            assert modelUrl != null;
            URI uri = null;
            try {
                uri = modelUrl.toURI();
            } catch (URISyntaxException e) {
                throw new SignException("没有找到检测模型");
            }
            Criteria<Image, NDList> criteria = Criteria.builder()
                    .optEngine("OnnxRuntime")
                    .setTypes(Image.class, NDList.class)
                    .optModelPath(Paths.get(uri))
                    .optTranslator(new OCRDetectionTranslator(new ConcurrentHashMap<String, String>()))
                    .optProgress(new ProgressBar())
                    .build();
            detModel = ModelZoo.loadModel(criteria);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new SignException("自动装配检测模型失败");
        }
        return detModel;
    }

    @Bean
    public OcrV4Recognition recognition(NDManager manager, Predictor<Image, NDList> detector, Predictor<Image, String> recognizer) {
        return new OcrV4Recognition(manager, detector, recognizer);
    }

    /**
     * 文本框识别模型
     */
    @Bean
    public ZooModel<Image, String> recModel() {
        ZooModel<Image, String> detModel = null;
        try {
            ClassLoader classLoader = OcrConfig.class.getClassLoader();
            URL modelUrl = classLoader.getResource("models/rec_inference.onnx");
            assert modelUrl != null;
            URI uri = null;
            try {
                uri = modelUrl.toURI();
            } catch (URISyntaxException e) {
                throw new SignException("没有找到识别模型");
            }
            Criteria<Image, String> criteria = Criteria.builder()
                    .optEngine("OnnxRuntime")
                    .setTypes(Image.class, String.class)
                    .optModelPath(Paths.get(uri))
                    .optProgress(new ProgressBar())
                    .optTranslator(new PpWordRecTranslator(new ConcurrentHashMap<String, String>()))
                    .build();
            detModel = ModelZoo.loadModel(criteria);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new SignException("自动装配识别模型失败");
        }
        return detModel;
    }

    /**
     * 文本框检测推理器
     */
    @Bean
    public Predictor<Image, NDList> detector(ZooModel<Image, NDList> detModel) {
        return detModel.newPredictor();
    }

    /**
     * 文本框识别推理器
     */
    @Bean
    public Predictor<Image, String> recognizer(ZooModel<Image, String> recModel) {
        return recModel.newPredictor();
    }

    /**
     * 模型等对象内存管理器
     */
    @Bean
    public NDManager manager() {
        return NDManager.newBaseManager();
    }

    /**
     * opencv实例对象（加载一些库）
     */
    @Bean
    public ImageFactory imageFactory() {
        return OpenCVImageFactory.getInstance();
    }
}
