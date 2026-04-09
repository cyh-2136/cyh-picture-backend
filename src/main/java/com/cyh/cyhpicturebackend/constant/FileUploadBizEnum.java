package com.cyh.cyhpicturebackend.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传业务场景枚举
 */
public enum FileUploadBizEnum {
    
    /**
     * 图片上传场景
     */
    PICTURE("picture", "图片", "picture", 2 * 1024 * 1024, Arrays.asList("jpg", "png", "jpeg", "webp")),
    
    /**
     * 用户头像上传场景
     */
    AVATAR("avatar", "用户头像", "avatar", 1 * 1024 * 1024, Arrays.asList("jpg", "png", "jpeg")),
    
    /**
     * AI生成图片上传场景
     */
    AI_GENERATED("ai_generated", "AI生成图片", "ai/generated", 5 * 1024 * 1024, Arrays.asList("jpg", "png", "jpeg", "webp")),
    
    /**
     * 文档上传场景
     */
    DOCUMENT("document", "文档", "document", 10 * 1024 * 1024, Arrays.asList("pdf", "doc", "docx", "txt")),
    
    /**
     * 视频上传场景
     */
    VIDEO("video", "视频", "video", 50 * 1024 * 1024, Arrays.asList("mp4", "mov", "avi", "wmv"));
    
    /**
     * 业务场景编码
     */
    private final String code;
    
    /**
     * 业务场景名称
     */
    private final String name;
    
    /**
     * 上传路径前缀
     */
    private final String uploadPathPrefix;
    
    /**
     * 文件大小限制（字节）
     */
    private final long maxFileSize;
    
    /**
     * 支持的文件格式列表
     */
    private final List<String> supportedFormats;
    
    FileUploadBizEnum(String code, String name, String uploadPathPrefix, long maxFileSize, List<String> supportedFormats) {
        this.code = code;
        this.name = name;
        this.uploadPathPrefix = uploadPathPrefix;
        this.maxFileSize = maxFileSize;
        this.supportedFormats = supportedFormats;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getUploadPathPrefix() {
        return uploadPathPrefix;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public List<String> getSupportedFormats() {
        return supportedFormats;
    }
    
    /**
     * 根据编码获取枚举实例
     * @param code 业务场景编码
     * @return 枚举实例
     */
    public static FileUploadBizEnum getByCode(String code) {
        for (FileUploadBizEnum bizEnum : values()) {
            if (bizEnum.code.equals(code)) {
                return bizEnum;
            }
        }
        return null;
    }
}