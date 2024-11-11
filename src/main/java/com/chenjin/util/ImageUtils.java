package com.chenjin.util;

import cn.hutool.core.io.IoUtil;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.bo.SignPair;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片工具
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-10-22 22:44
 **/
public class ImageUtils {

    /**
     * 验证流数据是否为png格式
     *
     * @param inputStream 待验证流数据
     */
    public static boolean isPNG(InputStream inputStream) throws IOException {
        byte[] header = new byte[8];  // PNG 文件头通常是前 8 个字节
        int readLength = inputStream.read(header, 0, 8);
        if (readLength != 8) {
            return false;
        }
        // PNG 文件以 89 50 4E 47 0D 0A 1A 0A 开头
        return header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                header[2] == (byte) 0x4E && header[3] == (byte) 0x47 &&
                header[4] == (byte) 0x0D && header[5] == (byte) 0x0A &&
                header[6] == (byte) 0x1A && header[7] == (byte) 0x0A;
    }

    /**
     * 验证流数据是否为jpg格式
     *
     * @param inputStream 待验证流数据
     */
    public static boolean isJPG(InputStream inputStream) throws IOException {
        byte[] header = new byte[3];  // JPG 文件头通常是前 3 个字节
        int readLength = inputStream.read(header, 0, 3);
        if (readLength != 3) {
            return false;
        }
        // JPG 文件以 FF D8 FF 开头
        return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 &&
                header[2] == (byte) 0xFF;
    }

    /**
     * 判断图片是否为jpg或png格式
     */
    public static boolean isJpgOrPng(InputStream imgIs) {
        try {
            ByteArrayOutputStream imgOs = new ByteArrayOutputStream();
            IoUtil.copy(imgIs, imgOs);
            return isPNG(new ByteArrayInputStream(imgOs.toByteArray())) ||
                    isJPG(new ByteArrayInputStream(imgOs.toByteArray()));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 获取图片的长宽信息
     */
    public static SignPair<Float, Float> calcWH(InputStream imgIs) {
        try {
            BufferedImage image = ImageIO.read(imgIs);
            if (image == null) {
                throw new SignException("没有获取到图片信息");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            return SignPair.of((float) width, (float) height);
        } catch (IOException e) {
            throw new SignException(e.getMessage());
        }
    }

    /**
     * 在原图上叠加另一张图
     */
    public static ByteArrayOutputStream overlayImg(ByteArrayOutputStream srcOs, ByteArrayOutputStream destOs, int x, int y) {
        try {
            // 对destOs进行抠图
            destOs = matting(new ByteArrayInputStream(destOs.toByteArray()));
            // 对destOs进行缩放保证其大小为srcOs的7%
            destOs = scalePercent(new ByteArrayInputStream(srcOs.toByteArray()), new ByteArrayInputStream(destOs.toByteArray()), 7);
            BufferedImage srcImage = ImageIO.read(new ByteArrayInputStream(srcOs.toByteArray()));
            BufferedImage destImage = ImageIO.read(new ByteArrayInputStream(destOs.toByteArray()));

            Graphics2D g = srcImage.createGraphics();

            // 从左下角开始，剧中叠加
            int destHeight = destImage.getHeight();
            int middleDest = destHeight / 2;
            int srcHeight = srcImage.getHeight();
//            y = srcHeight - y;
            y = Math.max(y - middleDest, 0);
            // 判断是否会超出最低边
            y = y + middleDest > srcHeight ? srcHeight - middleDest : y;

            g.drawImage(destImage, x, y, null);
            g.dispose();

            ByteArrayOutputStream resultOs = new ByteArrayOutputStream();
            ImageIO.write(srcImage, "jpg", resultOs);
            return resultOs;
        } catch (IOException e) {
            throw new SignException(e.getMessage());
        }
    }

    /**
     * 按照指定比例缩放图片
     */
    public static ByteArrayOutputStream scalePercent(InputStream referIs, InputStream needIs, double percent) {
        ByteArrayOutputStream resultOs;
        try {
            if (null == referIs || null == needIs) {
                throw new SignException("图片数据为空");
            }
            resultOs = new ByteArrayOutputStream();
            IoUtil.copy(needIs, resultOs);
            BufferedImage referImage = ImageIO.read(referIs);
            BufferedImage needImage = ImageIO.read(new ByteArrayInputStream(resultOs.toByteArray()));
            int referArea = referImage.getWidth() * referImage.getHeight();
            int needArea = needImage.getWidth() * needImage.getHeight();
            double scaleSize = referArea * (percent / 100);
            if (scaleSize <= needArea) {
                double ceilSize = Math.ceil(scaleSize);
                // 计算最相近的两个乘数
                double sqrtLength = Math.sqrt(ceilSize);
                int width = (int) Math.round(sqrtLength);
                Image scaleImage = needImage.getScaledInstance(width, width, Image.SCALE_SMOOTH);
                BufferedImage image = new BufferedImage(width, width, needImage.getType());
                Graphics2D g = image.createGraphics();
                g.drawImage(scaleImage, 0, 0, null);
                g.dispose();
                resultOs = new ByteArrayOutputStream();
                ImageIO.write(image, "jpg", resultOs);
            }
            return resultOs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 按照指定比例缩放图片
     * @param referArea 原图面积
     */
    public static ByteArrayOutputStream scalePercent(int referArea, InputStream needIs, double percent) {
        ByteArrayOutputStream resultOs = new ByteArrayOutputStream();
        try {
            if (needIs == null) {
                throw new SignException("图片数据为空");
            }

            // 复制 InputStream 到 ByteArrayOutputStream
            IoUtil.copy(needIs, resultOs);

            // 将数据读入 BufferedImage
            BufferedImage needImage = ImageIO.read(new ByteArrayInputStream(resultOs.toByteArray()));
            int needArea = needImage.getWidth() * needImage.getHeight();

            // 计算目标区域
            double scaleSize = referArea * (percent / 100);

            // 仅当缩放目标小于原始面积时才进行缩放
            if (scaleSize < needArea) {
                // 计算缩放比例
                double scaleRatio = Math.sqrt(scaleSize / needArea);
                int newWidth = (int) Math.round(needImage.getWidth() * scaleRatio);
                int newHeight = (int) Math.round(needImage.getHeight() * scaleRatio);

                // 创建缩放后的图像
                BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = scaledImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.drawImage(needImage, 0, 0, newWidth, newHeight, null);
                g2d.dispose();

                // 将缩放后的图像写入结果流
                resultOs = new ByteArrayOutputStream();
                ImageIO.write(scaledImage, "png", resultOs);
            }
            return resultOs;
        } catch (IOException e) {
            throw new RuntimeException("缩放图片失败", e);
        }
    }


    /**
     * 竖向堆叠图片
     */
    public static ByteArrayOutputStream verticalStacked(ByteArrayOutputStream[] imgArr) {
        if (null == imgArr || imgArr.length == 0) {
            throw new SignException("请输入正确的图像集合!");
        }
        try {
            BufferedImage[] images = new BufferedImage[imgArr.length];
            int width = 0;
            int height = 0;
            for (int i = 0; i < imgArr.length; i++) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imgArr[i].toByteArray()));
                width = Math.max(image.getWidth(), width);
                height += image.getHeight();
                images[i] = image;
            }
            if (width == 0 || height == 0) {
                throw new SignException("请输入正确的图像集合!!!");
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            int currentHeight = 0;
            if (images.length > 1) {
                g.drawImage(images[0], 0, 0, null);
                currentHeight = images[0].getHeight();
                for (int i = 1; i < images.length; i++) {
                    g.drawImage(images[i], 0, currentHeight, null);
                    currentHeight += images[i].getHeight(); // 累加高度
                }
            } else {
                g.drawImage(images[0], 0, 0, null);
            }
            g.dispose();

            ByteArrayOutputStream resultOs = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", resultOs);
            return resultOs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 计算图片的通道数
     */
    public static int calcChannelNum(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IoUtil.copy(is, os);
        boolean isJpgOrPng = isJpgOrPng(new ByteArrayInputStream(os.toByteArray()));
        if (!isJpgOrPng) {
            throw new SignException("不支持jpg/png以外的格式数据");
        }
        MatOfByte matOfByte = new MatOfByte(os.toByteArray());
        Mat image = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
        int channels = image.channels();
        image.release();
        return channels;
    }

    /**
     * 对签名或者印章进行抠图
     */
    public static ByteArrayOutputStream matting(InputStream is) {
        if (null == is) {
            throw new SignException("没有读取到图片数据");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IoUtil.copy(is, os);
        boolean isJpgOrPng = isJpgOrPng(new ByteArrayInputStream(os.toByteArray()));
        if (!isJpgOrPng) {
            throw new SignException("不支持jpg/png以外的格式数据");
        }
        MatOfByte matOfByte = new MatOfByte(os.toByteArray());
        Mat image = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
        int channelsNum = image.channels();
        // 四通道直接返回
        if (channelsNum == 4) {
            return os;
        }
        // 转为灰度图
        Mat img2gray = new Mat();
        Imgproc.cvtColor(image, img2gray, Imgproc.COLOR_BGR2GRAY);
        // 二值化
        Mat mask = new Mat();
        Imgproc.threshold(img2gray, mask, 165 , 255, Imgproc.THRESH_BINARY);
        // 取反计算掩图轮廓
        Mat mask_inv = new Mat();
        Core.bitwise_not(mask, mask_inv);
        // 抠出主体
        Mat img2_fg = new Mat();
        Core.bitwise_and(image, image, img2_fg, mask_inv);
        // 创建一个带有 alpha 通道的 Mat
        Mat img2_fg_alpha = new Mat(img2_fg.size(), CvType.CV_8UC4);
        // 将 img2_fg 复制到 img2_fg_alpha 的前三个通道
        List<Mat> channels = new ArrayList<>();
        Core.split(img2_fg, channels);
        // 添加 alpha 通道
        channels.add(mask_inv);
        // 合并通道
        Core.merge(channels, img2_fg_alpha);
        // 返回
        ByteArrayOutputStream resultOs = new ByteArrayOutputStream();
        MatOfByte resultMatOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img2_fg_alpha, resultMatOfByte);
        byte[] array = resultMatOfByte.toArray();
        IoUtil.write(resultOs, false,array);

        // 释放资源
        image.release();
        img2gray.release();
        mask.release();
        mask_inv.release();
        img2_fg.release();
        img2_fg_alpha.release();
        return resultOs;
    }

}
