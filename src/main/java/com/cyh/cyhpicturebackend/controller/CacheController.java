package com.cyh.cyhpicturebackend.controller;

import com.cyh.cyhpicturebackend.annotation.AuthCheck;
import com.cyh.cyhpicturebackend.common.BaseResponse;
import com.cyh.cyhpicturebackend.common.ResultUtils;
import com.cyh.cyhpicturebackend.constant.UserConstant;
import com.cyh.cyhpicturebackend.manager.CacheManager;
import com.cyh.cyhpicturebackend.model.dto.cache.RefreshCacheRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/admin/cache")
public class CacheController {

    @Resource
    private CacheManager cacheManager;

    /**
     * 刷新所有缓存
     */
    @PostMapping("/refresh/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refreshAllCache() {
        cacheManager.refreshAllCache();
        log.info("管理员刷新了所有缓存");
        return ResultUtils.success(true);
    }

    /**
     * 刷新指定缓存
     */
    @PostMapping("/refresh")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refreshCache(@RequestBody RefreshCacheRequest request) {
        cacheManager.refreshCache(request.getCacheName());
        log.info("管理员刷新了缓存：{}", request.getCacheName());
        return ResultUtils.success(true);
    }

}