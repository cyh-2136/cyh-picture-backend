package com.cyh.cyhpicturebackend.model.dto.cache;

import lombok.Data;

import java.io.Serializable;

@Data
public class RefreshCacheRequest implements Serializable {
    /**
     * 缓存名称
     */
    private String cacheName;

    private static final long serialVersionUID = 1L;
}