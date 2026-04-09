package com.cyh.cyhpicturebackend.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片格式枚举
 */
public enum PictureFormatEnum {
    JPG("jpg", "JPEG图片"),
    JPEG("jpeg", "JPEG图片"),
    PNG("png", "PNG图片"),
    WEBP("webp", "WebP图片"),
    GIF("gif", "GIF图片");

    private final String format;
    private final String description;

    PictureFormatEnum(String format, String description) {
        this.format = format;
        this.description = description;
    }

    public String getFormat() {
        return format;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取所有支持的图片格式
     */
    public static List<String> getAllFormats() {
        return Arrays.stream(values())
                .map(PictureFormatEnum::getFormat)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否支持指定的图片格式
     */
    public static boolean isSupported(String format) {
        return Arrays.stream(values())
                .anyMatch(p -> p.getFormat().equalsIgnoreCase(format));
    }

    /**
     * 根据格式获取枚举实例
     */
    public static PictureFormatEnum getByFormat(String format) {
        for (PictureFormatEnum formatEnum : values()) {
            if (formatEnum.getFormat().equalsIgnoreCase(format)) {
                return formatEnum;
            }
        }
        return null;
    }
}