package com.chenjin.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 文件工具（拓展hutool）
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 17:48
 **/
public class FileUtils extends FileUtil {

    /**
     * 获取文件后缀，带"."
     * @param fileName 文件名
     * @return 文件后缀带"."
     */
    public static String getSuffixWithPoint(String fileName) {
        return StrUtil.sub(fileName, fileName.lastIndexOf("."), fileName.length());
    }
}
