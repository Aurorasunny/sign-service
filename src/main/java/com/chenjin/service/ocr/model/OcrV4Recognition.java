package com.chenjin.service.ocr.model;

import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.Point;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.opencv.OpenCVImageFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.bo.RotatedBox;
import com.chenjin.service.ocr.translator.PpWordRecTranslator;
import com.chenjin.util.NDArrayUtils;
import com.chenjin.util.OpenCVUtils;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文字识别
 */
public class OcrV4Recognition {

    private final NDManager manager;

    private final Predictor<Image, NDList> detector;

    private final Predictor<Image, String> recognizer;

    public OcrV4Recognition(NDManager manager, Predictor<Image, NDList> detector, Predictor<Image, String> recognizer) {
        this.manager = manager;
        this.detector = detector;
        this.recognizer = recognizer;
    }


    /**
     * 图像推理(先分割文字框，在识别文字框)
     */
    public List<RotatedBox> predict(Image image) throws TranslateException {
        NDList boxes = this.detector.predict(image);
        // 交给 NDManager自动管理内存
        boxes.attach(this.manager);

        List<RotatedBox> result = new ArrayList<>();

        Mat mat = (Mat) image.getWrappedImage();

        for (NDArray box : boxes) {
            float[] pointsArr = box.toFloatArray();
            float[] lt = java.util.Arrays.copyOfRange(pointsArr, 0, 2);
            float[] rt = java.util.Arrays.copyOfRange(pointsArr, 2, 4);
            float[] rb = java.util.Arrays.copyOfRange(pointsArr, 4, 6);
            float[] lb = java.util.Arrays.copyOfRange(pointsArr, 6, 8);
            int img_crop_width = (int) Math.max(distance(lt, rt), distance(rb, lb));
            int img_crop_height = (int) Math.max(distance(lt, lb), distance(rt, rb));
            List<Point> srcPoints = new ArrayList<>();
            srcPoints.add(new Point(lt[0], lt[1]));
            srcPoints.add(new Point(rt[0], rt[1]));
            srcPoints.add(new Point(rb[0], rb[1]));
            srcPoints.add(new Point(lb[0], lb[1]));
            List<Point> dstPoints = new ArrayList<>();
            dstPoints.add(new Point(0, 0));
            dstPoints.add(new Point(img_crop_width, 0));
            dstPoints.add(new Point(img_crop_width, img_crop_height));
            dstPoints.add(new Point(0, img_crop_height));

            Mat srcPoint2f = NDArrayUtils.toMat(srcPoints);
            Mat dstPoint2f = NDArrayUtils.toMat(dstPoints);

            Mat cvMat = OpenCVUtils.perspectiveTransform(mat, srcPoint2f, dstPoint2f);

            Image subImg = OpenCVImageFactory.getInstance().fromImage(cvMat);

            subImg = subImg.getSubImage(0, 0, img_crop_width, img_crop_height);
            if (subImg.getHeight() * 1.0 / subImg.getWidth() > 1.5) {
                subImg = rotateImg(this.manager, subImg);
            }

            String name = this.recognizer.predict(subImg);
            RotatedBox rotatedBox = new RotatedBox(box, name);
            result.add(rotatedBox);

            cvMat.release();
            srcPoint2f.release();
            dstPoint2f.release();

        }

        return result;
    }

    private BufferedImage get_rotate_crop_image(Image image, NDArray box) {
        return null;
    }

    /**
     * 欧式距离计算
     */
    private float distance(float[] point1, float[] point2) {
        float disX = point1[0] - point2[0];
        float disY = point1[1] - point2[1];
        return (float) Math.sqrt(disX * disX + disY * disY);
    }

    /**
     * 图片旋转
     */
    private Image rotateImg(NDManager manager, Image image) {
        NDArray rotated = NDImageUtils.rotate90(image.toNDArray(manager), 1);
        return ImageFactory.getInstance().fromNDArray(rotated);
    }
}