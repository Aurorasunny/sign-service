package com.chenjin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenjin.pojo.FileStore;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件对象数据持久化操作对象
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 15:52
 **/
@Mapper
public interface FileStoreMapper extends BaseMapper<FileStore> {
}
