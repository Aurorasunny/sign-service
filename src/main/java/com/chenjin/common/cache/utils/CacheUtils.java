package com.chenjin.common.cache.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.extra.spring.SpringUtil;
import com.chenjin.common.cache.exception.CacheException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Map;
import java.util.Set;

/**
 * 缓存工具类
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 12:04
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheUtils {

    /**
     * caffeine缓存管理器
     */
    private static final CacheManager CACHE_MANAGER = SpringUtil.getBean(CacheManager.class);

    /**
     * 保存缓存
     *
     * @param cacheName 缓存组名称
     * @param key       缓存key
     * @param value     缓存值
     */
    public static void put(String cacheName, String key, Object value) {
        Cache caffeine = CACHE_MANAGER.getCache(cacheName);
        if (null == caffeine) {
            throw new CacheException("保存缓存失败，失败原因：没有找到该缓存对象");
        }
        caffeine.put(key, value);
    }

    /**
     * 保存缓存（使用默认的caffeine缓存组）
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    public static void put(String key, Object value) {
        put("caffeine", key, value);
    }

    /**
     * 获取缓存值
     *
     * @param cacheName 缓存组名称
     * @param key       缓存key
     * @return 缓存值
     */
    public static Object get(String cacheName, String key) {
        Cache caffeine = CACHE_MANAGER.getCache(cacheName);
        if (null == caffeine) {
            throw new CacheException("获取缓存失败，失败原因：没有找到该缓存对象");
        }
        Cache.ValueWrapper wrapper = caffeine.get(key);
        return wrapper != null ? wrapper.get() : null;
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存key
     * @return 缓存值
     */
    public static Object get(String key) {
        return get("caffeine", key);
    }

    /**
     * 清除指定的缓存key
     *
     * @param cacheName 缓存组名称
     * @param key       缓存key
     */
    public static void evict(String cacheName, String key) {
        Cache caffeine = CACHE_MANAGER.getCache(cacheName);
        if (null == caffeine) {
            throw new CacheException("清除缓存失败，失败原因：没有找到该缓存对象");
        }
        caffeine.evict(key);
    }

    /**
     * 清除指定的缓存key（默认caffeine组）
     *
     * @param key 缓存key
     */
    public static void evict(String key) {
        evict("caffeine", key);
    }

    /**
     * 清空缓存组的所有缓存
     *
     * @param cacheName 缓存组
     */
    public static void clear(String cacheName) {
        Cache caffeine = CACHE_MANAGER.getCache(cacheName);
        if (null == caffeine) {
            throw new CacheException("清空缓存失败，失败原因：没有找到该缓存对象");
        }
        caffeine.clear();
    }

}
