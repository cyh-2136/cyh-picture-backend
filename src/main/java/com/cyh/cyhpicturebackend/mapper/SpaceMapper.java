package com.cyh.cyhpicturebackend.mapper;

import com.cyh.cyhpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author 21369
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2026-03-21 14:28:38
* @Entity com.cyh.cyhpicturebackend.model.entity.Space
*/
public interface SpaceMapper extends BaseMapper<Space> {
    @Select("SELECT id,spaceName,spaceLevel,spaceType,maxSize,maxCount,totalSize,totalCount,userId,createTime,editTime,updateTime,isDelete FROM space " +
            "WHERE userId = #{userId} AND spaceType = #{spaceType} AND isDelete = 0" +
            " FOR UPDATE")
    Space selectByUserIdAndTypeForUpdate(@Param("userId") Long userId, @Param("spaceType") Integer spaceType);
}




