package com.cyh.cyhpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest  implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片 id(用于更新图片)
     *
     */
     private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 文件地址
     */
    private String fileUrl;
}
