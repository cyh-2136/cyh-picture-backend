package com.cyh.cyhpicturebackend.service;

import com.cyh.cyhpicturebackend.model.dto.space.analyze.*;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.vo.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService {

    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest 空间分析请求参数
     * @param loginUser 登录用户
     */
    void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片分类分析数据
     * @param spaceCategoryAnalyzeRequest SpaceCategoryAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceCategoryAnalyzeResponse 分析结果
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);


    /**
     * 获取空间标签分析数据
     * @param spaceTagAnalyzeRequest SpaceTagAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceTagAnalyzeResponse 分析结果
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);


    /**
     * 获取空间图片大小分析数据
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户分析数据
     * @param spaceUserAnalyzeRequest SpaceUserAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceUserAnalyzeResponse 分析结果
     **/
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);


    /**
     * 获取空间排名分析数据
     * @param spaceRankAnalyzeRequest SpaceRankAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return List<Space> 排名前 N 的空间
     **/
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
