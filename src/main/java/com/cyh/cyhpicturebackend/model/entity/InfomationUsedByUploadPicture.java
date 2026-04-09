package com.cyh.cyhpicturebackend.model.entity;


import lombok.Data;

@Data
public class InfomationUsedByUploadPicture {
    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片个数
     */
    private Long picCount;
}
