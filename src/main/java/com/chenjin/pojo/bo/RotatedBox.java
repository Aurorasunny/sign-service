package com.chenjin.pojo.bo;

import ai.djl.ndarray.NDArray;
import lombok.Data;

/**
 * 旋转检测框
 */
@Data
public class RotatedBox implements Comparable<RotatedBox> {
    private NDArray box;
    private String text;

    public RotatedBox(NDArray box, String text) {
        this.box = box;
        this.text = text;
    }

    /**
     * 将左上角 Y 坐标升序排序rn
     */
    @Override
    public int compareTo(RotatedBox o) {
        NDArray lowBox = this.getBox();
        NDArray highBox = o.getBox();
        float lowY = lowBox.toFloatArray()[1];
        float highY = highBox.toFloatArray()[1];
        return Float.compare(lowY, highY);
    }
}