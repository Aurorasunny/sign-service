package com.chenjin.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件对象
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-31 10:53
 **/
@Data
@Builder
public class FileStore implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 文件编码
     */
    private String fileCode;
    /**
     * 文件类型
     */
    private String fileType;
    /**
     * mime类型
     */
    private String mimeType;
    /**
     * 文件名称
     */
    private String fileName;
    /**
     * 存储类型（oss存储/本地存储）
     */
    private String storeType;
    /**
     * 对象存储key
     */
    private String ossKey;
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
