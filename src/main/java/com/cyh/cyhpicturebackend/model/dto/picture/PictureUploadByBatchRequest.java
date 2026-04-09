package com.cyh.cyhpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadByBatchRequest implements Serializable {


    /**
     * 名称前缀
     */
    private String namePrefix;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count=10;

    private Integer offset;

    private String category;

    private List<String> tags;

    private static final long serialVersionUID = 1L;
}