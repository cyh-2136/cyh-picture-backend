package com.cyh.cyhpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceAddRequest;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceQueryRequest;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.vo.SpaceVO;
import com.cyh.cyhpicturebackend.model.entity.User;


import javax.servlet.http.HttpServletRequest;

/**
* @author 21369
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-03-21 14:28:38
*/
public interface SpaceService extends IService<Space> {


    /**
     * 创建空间
     * @param spaceAddRequest 空间添加请求参数
     * @param loginUser 登录用户
     * @return 空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest,User loginUser);

    /**
     * 校验空间是否有效
     * @param space 空间
     * @param add 是否新增
     */
    void validSpace(Space space, boolean add);



    /**
     * 获取查询包装器
     * @param spaceQueryRequest 空间查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 空间封装
     * @param space 空间实体
     * @param request HttpServletRequest对象
     * @return 空间视图对象
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 分页获取空间封装
     * @param spacePage 空间分页对象
     * @param request HttpServletRequest对象
     * @return 空间视图分页对象
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 根据空间级别填充空间数据
     * @param space 空间实体
     */
    public void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     * @param loginUser 登录用户
     * @param space 空间实体
     */
    void checkSpaceAuth(User loginUser, Space space);

}
