package com.chenjin.common.oss.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.chenjin.common.oss.common.AccessPolicyEnum;
import com.chenjin.common.oss.common.OssConstants;
import com.chenjin.common.oss.common.PolicyEnum;
import com.chenjin.common.oss.entity.UploadResult;
import com.chenjin.common.oss.exception.OssException;
import com.chenjin.common.oss.properties.OssProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.*;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;

/**
 * oss客户端
 *
 * @author <yanrui yanrui0910@163.com>
 * @since 2024-08-21 23:35
 **/
public class OssClient {
    /**
     * 服务商（每种服务商配置一个）
     */
    private String configKey;
    /**
     * oss配置信息
     */
    private OssProperties ossProperties;
    /**
     * s3异步客户端
     */
    private S3AsyncClient client;
    /**
     * s3客服端管理服务（对原始的客户端接口进行封装）
     */
    private S3TransferManager transferManager;
    /**
     * s3 预签名url（提供无需访问权限的临时url）
     */
    private S3Presigner presigner;

    /**
     * 客户端构造方法
     *
     * @param configKey     配置key
     * @param ossProperties 存储配置
     */
    public OssClient(String configKey, OssProperties ossProperties) {
        this.configKey = configKey;
        this.ossProperties = ossProperties;

        try {
            // 创建认证信息
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ossProperties.getAccessKey(), ossProperties.getSecretKey())
            );

            // 判断当前是否为云厂商（MinIO 需要启用路径样式访问）
            boolean isPathStyle = !ArrayUtil.containsAny(OssConstants.CLOUD_SERVICE, configKey);

            // 构建基于crt的s3客户端，有更高的性能
            this.client = S3AsyncClient.crtBuilder()
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(getEndpoint()))
                    .region(getRegion())
                    .targetThroughputInGbps(20.0)
                    .minimumPartSizeInBytes(10 * 1025 * 1024L)
                    .checksumValidationEnabled(false)
                    .forcePathStyle(isPathStyle)
                    .build();

            // 创建一个S3TransferManager封装crt客户端信息
            this.transferManager = S3TransferManager.builder().s3Client(this.client).build();

            // 创建一个s3配置
            S3Configuration config = S3Configuration.builder()
                    .checksumValidationEnabled(false)
                    .pathStyleAccessEnabled(isPathStyle)
                    .build();

            // 初始化url预签名
            this.presigner = S3Presigner.builder()
                    .credentialsProvider(credentialsProvider)
                    .region(getRegion())
                    .endpointOverride(URI.create(getDomain()))
                    .serviceConfiguration(config)
                    .build();

            // 创建桶信息
            createBucket();
        } catch (Exception e) {
            if (e instanceof OssException) {
                throw e;
            }
            throw new OssException("初始化对象存储失败");
        }

    }

    /**
     * 上传文件（临时文件）
     *
     * @param filePath    文件路径（临时文件）
     * @param key         文件名
     * @param contentType 文件类型
     * @return 上传结果
     */
    public UploadResult upload(Path filePath, String key, String contentType) {
        try {
            FileUpload fileUpload = transferManager.uploadFile(builder ->
                    builder.putObjectRequest(putObj ->
                                    putObj.bucket(ossProperties.getBucketName())
                                            .key(key)
                                            .contentType(contentType)
                                            .acl(getAccessPolicy().getObjectCannedACL())
                                            .build())
                            .addTransferListener(LoggingTransferListener.create())
                            .source(filePath).build());
            // 等待上传结果
            CompletedFileUpload uploadResult = fileUpload.completionFuture().join();
            String eTag = uploadResult.response().eTag();

            // 构建返回
            return UploadResult.builder().url(getUrl() + StrUtil.SLASH + key).fileName(key).eTag(eTag).build();
        } catch (Exception e) {
            throw new OssException("上传文件失败，失败原因：" + e.getMessage());
        } finally {
            // 删除上传的临时文件
            FileUtil.del(filePath);
        }
    }

    /**
     * 上传文件（输入流）
     *
     * @param inputStream 输入流
     * @param key         文件名
     * @param length      文件长度
     * @param contentType 文件类型
     * @return 上传结果
     */
    public UploadResult upload(InputStream inputStream, String key, Long length, String contentType) {
        if (!(inputStream instanceof ByteArrayInputStream)) {
            inputStream = new ByteArrayInputStream(IoUtil.readBytes(inputStream));
        }
        try {
            // 创建异步请求对象
            BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(length);
            // 上传对象
            Upload upload = transferManager.upload(
                    uploadReq -> uploadReq.requestBody(body).putObjectRequest(
                            putObj -> putObj.bucket(ossProperties.getBucketName())
                                    .key(key)
                                    .contentType(contentType)
                                    .acl(getAccessPolicy().getObjectCannedACL())
                                    .build()).build()
            );

            // 将结果写到输入流中
            body.writeInputStream(inputStream);

            // 等待返回结果
            CompletedUpload uploadResult = upload.completionFuture().join();
            String eTag = uploadResult.response().eTag();

            return UploadResult.builder().url(getUrl() + StrUtil.SLASH + key).fileName(key).eTag(eTag).build();
        } catch (Exception e) {
            throw new OssException("上传文件失败，失败原因：" + e.getMessage());
        }
    }

    /**
     * 下载文件对临时目录
     *
     * @param path 文件在oss中的路径
     * @return 本地临时文件路径
     */
    public Path downloadInFile(String path) {
        try {
            // 创建临时文件
            Path tempFilePath = FileUtil.createTempFile().toPath();
            FileDownload fileDownload = transferManager.downloadFile(
                    downloadReq -> downloadReq.getObjectRequest(
                                    getObj -> getObj.bucket(ossProperties.getBucketName())
                                            .key(removeBaseUrl(path))
                                            .build())
                            .addTransferListener(LoggingTransferListener.create())
                            .destination(tempFilePath)
                            .build());
            // 等待文件下载完成
            fileDownload.completionFuture().join();
            return tempFilePath;
        } catch (IORuntimeException e) {
            throw new OssException("下载文件失败，失败原因：" + e.getMessage());
        }
    }

    /**
     * 下载oss对象
     *
     * @param key 文件名（oss对象key）
     * @return 对象输入流
     */
    public InputStream downloadFileIs(String key) {
        try {
            DownloadRequest<ResponseInputStream<GetObjectResponse>> downloadRequest = DownloadRequest.builder()
                    .getObjectRequest(getObj -> getObj.bucket(ossProperties.getBucketName())
                            .key(key)
                            .build())
                    .addTransferListener(LoggingTransferListener.create())
                    // 使用订阅转换器
                    .responseTransformer(AsyncResponseTransformer.toBlockingInputStream())
                    .build();
            // 从oss中下载数据
            Download<ResponseInputStream<GetObjectResponse>> downloadResponse = transferManager.download(downloadRequest);
            try (ResponseInputStream<GetObjectResponse> responseStream = downloadResponse.completionFuture().join().result()) {
                return new ByteArrayInputStream(responseStream.readAllBytes());
            }
        } catch (Exception e) {
            throw new OssException("下载文件失败，失败原因：" + e.getMessage());
        }
    }

    /**
     * 下载oss对象到流对象中
     *
     * @param key          文件名（oss对象key）
     * @param outputStream 输出流
     * @return 输出流中写入的字节数（长度）
     */
    public long download(String key, OutputStream outputStream) {
        try {
            DownloadRequest<ResponseInputStream<GetObjectResponse>> downloadRequest = DownloadRequest.builder()
                    .getObjectRequest(getObj -> getObj.bucket(ossProperties.getBucketName())
                            .key(key)
                            .build())
                    .addTransferListener(LoggingTransferListener.create())
                    // 使用订阅转换器
                    .responseTransformer(AsyncResponseTransformer.toBlockingInputStream())
                    .build();
            // 从oss中下载数据
            Download<ResponseInputStream<GetObjectResponse>> downloadResponse = transferManager.download(downloadRequest);
            try (ResponseInputStream<GetObjectResponse> responseStream = downloadResponse.completionFuture().join().result()) {
                return responseStream.transferTo(outputStream);
            }
        } catch (Exception e) {
            throw new OssException("下载文件失败，失败原因：" + e.getMessage());
        }
    }

    /**
     * 删除oss中的对象
     *
     * @param path oss对象路径
     */
    public void delete(String path) {
        try {
            client.deleteObject(
                    deleteReq -> deleteReq.bucket(ossProperties.getBucketName())
                            .key(removeBaseUrl(path))
                            .build());
        } catch (Exception e) {
            throw new OssException("删除文件失败，失败原因：" + e.getMessage());
        }
    }

    /**
     * 获取一个有效期内的临时url访问链接
     *
     * @param objectKey oss对象key
     * @param second    有效时间（秒）
     * @return 临时url地址
     */
    public String getPrivateUrl(String objectKey, Integer second) {
        // 使用预签名生成器生成一个临时的链接用于临时访问
        URL url = presigner.presignGetObject(
                        getObj -> getObj.signatureDuration(Duration.ofSeconds(second))
                                .getObjectRequest(
                                        objReq -> objReq.bucket(ossProperties.getBucketName())
                                                .key(objectKey)
                                                .build())
                                .build())
                .url();
        return url.toString();
    }

    /**
     * 上传字节数组到oss中
     *
     * @param bytes  字节数组
     * @param suffix 对象后缀
     * @return 上传结果
     */
    public UploadResult uploadWithSuffix(byte[] bytes, String suffix) {
        return upload(new ByteArrayInputStream(bytes), getOssKey(suffix), Long.valueOf(bytes.length), FileUtil.getMimeType(suffix));
    }

    /**
     * 上传输入流到oss中
     *
     * @param inputStream 输入流
     * @param length      输入流字节长度
     * @param suffix      后缀
     * @return 上传结果
     */
    public UploadResult uploadWithInputStream(InputStream inputStream, Long length, String suffix) {
        return upload(inputStream, getOssKey(suffix), length, FileUtil.getMimeType(suffix));
    }

    /**
     * 上传文件到oss中
     *
     * @param file 文件
     * @return 上传结果
     */
    public UploadResult uploadWithFile(File file) {
        String fileName = file.getName();
        String suffix = StrUtil.sub(fileName, fileName.lastIndexOf("."), fileName.length());
        return upload(file.toPath(), getOssKey(suffix), FileUtil.getMimeType(suffix));
    }


    /**
     * 创建桶信息
     */
    public void createBucket() {
        String bucketName = ossProperties.getBucketName();
        // 首先尝试获取桶，获取不到进行创建
        try {
            this.client.headBucket(bucketRequest ->
                            bucketRequest.bucket(bucketName).build())
                    .join();
        } catch (Exception ex) {
            if (ex.getCause() instanceof NoSuchBucketException) {
                try {
                    // 没有查询到桶信息，则进行创建
                    this.client.createBucket(builder -> builder.bucket(bucketName).build())
                            .join();
                    // 设置桶的访问策略
                    this.client.putBucketPolicy(builder ->
                                    builder.bucket(bucketName)
                                            .policy(getPolicy(bucketName, getAccessPolicy().getPolicyEnum())).build())
                            .join();
                } catch (S3Exception e) {
                    // 创建桶失败或者设置访问策略失败
                    throw new OssException("创建桶失败，错误原因是：" + e.getMessage());
                }
            } else {
                throw new OssException("创建桶失败，错误原因是：" + ex.getMessage());
            }
        }

    }

    /**
     * 生成oss存储的唯一key
     *
     * @param suffix 对象后缀
     * @return oss中唯一的key
     */
    private String getOssKey(String suffix) {
        // 生成规则为 前缀 + 时间路径 + uuid + 后缀的方式保证唯一
        String uuid = IdUtil.fastSimpleUUID();
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String prefix = ossProperties.getPrefix();
        String keyPrefix = StrUtil.isNotEmpty(prefix) ?
                prefix + StrUtil.SLASH + datePath + StrUtil.SLASH + uuid : datePath + StrUtil.SLASH + uuid;
        return keyPrefix + suffix;
    }

    /**
     * 获取对象存储的相对路径
     *
     * @param path 对象全路径
     * @return 去除基础路径后的相对路径
     */
    private String removeBaseUrl(String path) {
        return StrUtil.removePrefix(path, getUrl() + StrUtil.SLASH);
    }

    /**
     * 获取oss对象存储的url
     *
     * @return 对象url
     */
    private String getUrl() {
        String domain = ossProperties.getDomain();
        String endpoint = ossProperties.getEndpoint();
        String httpProtocol = getHttpProtocolHeader();

        // 判断是否云厂商
        if (ArrayUtil.containsAny(OssConstants.CLOUD_SERVICE, configKey)) {
            return httpProtocol + (StrUtil.isNotEmpty(domain) ? domain : ossProperties.getBucketName() + "." + endpoint);
        }

        // MinIO需单独处理
        if (StrUtil.isNotEmpty(domain)) {
            return (StrUtil.startWith(domain, OssConstants.HTTP) || StrUtil.startWith(domain, OssConstants.HTTPS)) ?
                    domain + StrUtil.SLASH + ossProperties.getBucketName() : httpProtocol + domain + StrUtil.SLASH + ossProperties.getBucketName();
        }
        // 返回默认
        return httpProtocol + endpoint + StrUtil.SLASH + ossProperties.getBucketName();
    }

    /**
     * 获取s3客户端url地址（自定义域名）
     *
     * @return 对象存储地址
     */
    public String getDomain() {
        // 获取域名，站点，是否https
        String domain = ossProperties.getDomain();
        String endpoint = ossProperties.getEndpoint();
        String httpProtocol = getHttpProtocolHeader();

        // 如果是云服务商，则直接返回域名或者站点
        if (ArrayUtil.containsAny(OssConstants.CLOUD_SERVICE, configKey)) {
            return StrUtil.isEmpty(domain) ? httpProtocol + endpoint : httpProtocol + domain;
        }

        // 若为minio需单独处理，返回协议加域名信息
        if (StrUtil.isNotEmpty(domain)) {
            return StrUtil.startWith(OssConstants.HTTP, domain) || StrUtil.startWith(OssConstants.HTTPS, domain) ? domain : httpProtocol + domain;
        }

        // 默认返回（协议加站点）
        return httpProtocol + endpoint;

    }

    /**
     * 获取桶访问策略
     *
     * @return 策略值
     */
    public AccessPolicyEnum getAccessPolicy() {
        return AccessPolicyEnum.getByType(ossProperties.getAccessPolicy());
    }

    /**
     * 生成s3存储桶访问策略
     *
     * @param bucketName 存储桶名称
     * @param policyEnum 访问策略
     * @return 访问策略字符串
     */
    private static String getPolicy(String bucketName, PolicyEnum policyEnum) {
        String policy;
        switch (policyEnum) {
            case WRITE:
                policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": []\n" +
                        "}";
                break;
            case READ_WRITE:
                policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\n" +
                        "        \"s3:GetBucketLocation\",\n" +
                        "        \"s3:ListBucket\",\n" +
                        "        \"s3:ListBucketMultipartUploads\"\n" +
                        "      ],\n" +
                        "      \"Resource\": \"arn:aws:s3:::bucketName\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\n" +
                        "        \"s3:AbortMultipartUpload\",\n" +
                        "        \"s3:DeleteObject\",\n" +
                        "        \"s3:GetObject\",\n" +
                        "        \"s3:ListMultipartUploadParts\",\n" +
                        "        \"s3:PutObject\"\n" +
                        "      ],\n" +
                        "      \"Resource\": \"arn:aws:s3:::bucketName/*\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                break;
            case READ:
                policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\"s3:GetBucketLocation\"],\n" +
                        "      \"Resource\": \"arn:aws:s3:::bucketName\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"Effect\": \"Deny\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": [\"s3:ListBucket\"],\n" +
                        "      \"Resource\": \"arn:aws:s3:::bucketName\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": \"s3:GetObject\",\n" +
                        "      \"Resource\": \"arn:aws:s3:::bucketName/*\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                break;
            default:
                throw new OssException("非法的策略值: " + policyEnum);
        }
        return policy.replaceAll("bucketName", bucketName);
    }


    /**
     * 获取oss对象存储区域
     *
     * @return 若存在值则封装后进行返回，若为空则返回默认的(US_EAST_1)
     */
    public Region getRegion() {
        String region = ossProperties.getRegion();
        return StrUtil.isEmpty(region) ? Region.US_EAST_1 : Region.of(region);
    }

    /**
     * 获取s3站点url
     *
     * @return 协议拼接站点信息
     */
    public String getEndpoint() {
        // 判断协议头部
        String httpProtocolHeader = getHttpProtocolHeader();
        // 协议 + 站点
        return httpProtocolHeader + ossProperties.getEndpoint();
    }

    /**
     * 获取http协议头信息
     *
     * @return 配置中isHttps为Y返回 "https://" 否则返回 "http://"
     */
    public String getHttpProtocolHeader() {
        return OssConstants.IS_HTTPS.equals(ossProperties.getIsHttps()) ? OssConstants.HTTPS : OssConstants.HTTP;
    }

    /**
     * 判断配置信息是否相同
     *
     * @param ossProperties oss配置信息
     * @return 是否相同
     */
    public boolean checkIsSameConfig(OssProperties ossProperties) {
        return this.ossProperties.equals(ossProperties);
    }
}
