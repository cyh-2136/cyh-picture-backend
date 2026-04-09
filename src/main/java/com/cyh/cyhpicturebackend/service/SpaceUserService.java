package com.cyh.cyhpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceAddRequest;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceQueryRequest;
import com.cyh.cyhpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.cyh.cyhpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.vo.SpaceUserVO;
import com.cyh.cyhpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 21369
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-03-26 19:26:08
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 创建空间成员
     * @param spaceUserAddRequest 空间成员添加请求参数
     * @return 空间成员id
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     * @param spaceUser 空间成员
     * @param add 是否新增
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 空间成员封装
     * @param spaceUser 空间成员实体
     * @param request HttpServletRequest对象
     * @return 空间成员视图对象
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员查询包装器
     * @param spaceUserQueryRequest 空间成员查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);




    /**
     * 获取空间成员封装
     * @param spaceUserList 空间成员列表
     * @return 空间成员视图列表
     */
    List<SpaceUserVO> getSpaceuserVOList(List<SpaceUser> spaceUserList);



}
