package com.cyh.cyhpicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 空间等级
     */
    private int value;

    /**
     * 空间等级文本
     */
    private String text;

    /**
     * 最大图片个数
     */
    private long maxCount;


    /**
     * 最大图片体积
     */
    private long maxSize;
}
