package com.chenjin.config;

import cn.hutool.json.JSONUtil;
import com.chenjin.common.cache.utils.CacheUtils;
import com.chenjin.constant.SignConstants;
import com.chenjin.exception.SignException;
import com.chenjin.pojo.SysOssConfig;
import com.chenjin.service.sysossconfig.SysOssConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 初始化oss对象配置
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 15:04
 **/
@Component
@RequiredArgsConstructor
public class InitOssConfigRunner implements CommandLineRunner {

    private final SysOssConfigService sysOssConfigService;

    @Override
    public void run(String... args) throws Exception {
        // 获取所有的对象存储配置
        try {
            List<SysOssConfig> ossConfigs = sysOssConfigService.list();
            ossConfigs.forEach(sysOssConfig -> {
                // 将系统配置保存到缓存中
                CacheUtils.put(SignConstants.OSS_CONFIG_KEY_PREFIX + sysOssConfig.getConfigKey(), JSONUtil.toJsonStr(sysOssConfig));
                // 判断是否是默认对象存储
                if (1 == sysOssConfig.getStatus()) {
                    // 存入缓存
                    CacheUtils.put(SignConstants.OSS_DEFAULT_KEY, sysOssConfig.getConfigKey());
                }
            });
        } catch (Exception e) {
            throw new SignException("初始化oss配置失败，失败原因：" + e.getCause());
        }
    }
}
