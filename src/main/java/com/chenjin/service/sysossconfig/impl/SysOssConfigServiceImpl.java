package com.chenjin.service.sysossconfig.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenjin.mapper.sysossconfig.SysOssConfigMapper;
import com.chenjin.pojo.SysOssConfig;
import com.chenjin.service.sysossconfig.SysOssConfigService;
import org.springframework.stereotype.Service;

/**
 * oss配置服务实现
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-24 14:58
 **/
@Service("sysOssConfigService")
public class SysOssConfigServiceImpl extends ServiceImpl<SysOssConfigMapper, SysOssConfig> implements SysOssConfigService {
}
