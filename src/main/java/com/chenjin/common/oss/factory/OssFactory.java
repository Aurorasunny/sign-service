package com.chenjin.common.oss.factory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.chenjin.common.cache.utils.CacheUtils;
import com.chenjin.common.oss.core.OssClient;
import com.chenjin.common.oss.exception.OssException;
import com.chenjin.common.oss.properties.OssProperties;
import com.chenjin.constant.SignConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * oss客服端工厂
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 15:20
 **/
public class OssFactory {

    private final static Map<String, OssClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    private final static ReentrantLock LOCK = new ReentrantLock();

    /**
     * 获取对象存储客户端（默认配置）
     *
     * @return 客户端
     */
    public static OssClient getInstance() {
        // 获取默认的
        Object defaultConfigKey = CacheUtils.get(SignConstants.OSS_DEFAULT_KEY);
        String configKey = Convert.toStr(defaultConfigKey);
        if (CLIENT_CACHE.containsKey(configKey) && CLIENT_CACHE.get(configKey) != null) {
            return CLIENT_CACHE.get(configKey);
        }
        return getOssClient(configKey);
    }

    /**
     * 获取对象存储客户端
     *
     * @param configKey oss配置key
     * @return oss客户端对象
     */
    public static OssClient getInstance(String configKey) {
        return getOssClient(configKey);
    }

    /**
     * 获取oss客户端
     *
     * @param configKey oss配置key
     * @return oss客户端
     */
    private static OssClient getOssClient(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            throw new OssException("oss配置key不能为空");
        }
        Object ossConfig = CacheUtils.get(SignConstants.OSS_CONFIG_KEY_PREFIX + configKey);
        if (ossConfig == null) {
            throw new OssException(StrUtil.format("没有查询{}到配置信息，请检查", configKey));
        }
        // 转换配置
        OssProperties ossProperties = JSONUtil.toBean(Convert.toStr(ossConfig), OssProperties.class);
        // 缓存中获取
        OssClient ossClient = CLIENT_CACHE.get(configKey);
        if (ossClient == null || !ossClient.checkIsSameConfig(ossProperties)) {
            // 为空或配置不相同则进行创建
            try {
                LOCK.lock();
                ossClient = CLIENT_CACHE.get(configKey);
                // 上锁后双检
                if (ossClient == null || !ossClient.checkIsSameConfig(ossProperties)) {
                    // 创建客户端对象
                    ossClient = new OssClient(configKey, ossProperties);
                    // 放入缓存
                    CLIENT_CACHE.put(configKey, ossClient);
                    return ossClient;
                }
            } finally {
                LOCK.unlock();
            }
        }
        return ossClient;
    }

}
