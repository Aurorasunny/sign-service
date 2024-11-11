/*
 Navicat Premium Dump SQL

 Source Server Type    : MySQL
 Source Server Version : 90000 (9.0.0)
 Source Schema         : sign_service

 Target Server Type    : MySQL
 Target Server Version : 90000 (9.0.0)
 File Encoding         : 65001

 Date: 12/11/2024 00:00:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_store
-- ----------------------------
DROP TABLE IF EXISTS `file_store`;
CREATE TABLE `file_store` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '文件id',
  `file_type` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文件类型',
  `file_code` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文件编码',
  `mime_type` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'mime类型',
  `file_name` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文件名称',
  `store_type` varchar(16) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '存储方式',
  `oss_key` varchar(128) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'oss存储key',
  `del_flag` varchar(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT '0' COMMENT '删除标记',
  `file_path` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '文件路径',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='文件存储记录表';

-- ----------------------------
-- Table structure for sys_oss_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_config`;
CREATE TABLE `sys_oss_config` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '配置名',
  `access_key` varchar(128) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '访问key',
  `secret_key` varchar(128) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '访问密钥',
  `bucket_name` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'oss桶名称',
  `prefix` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'oss前缀',
  `endpoint` varchar(128) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'oss存储站点',
  `region` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT 'oss存储桶区域',
  `domain` varchar(128) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '自定义访问域名',
  `status` smallint DEFAULT NULL COMMENT '是否默认',
  `remark` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `is_https` char(2) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '是否为https',
  `access_policy` varchar(16) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '访问策略',
  `insert_datetime` datetime DEFAULT NULL COMMENT '创建时间',
  `update_datetime` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;

SET FOREIGN_KEY_CHECKS = 1;
