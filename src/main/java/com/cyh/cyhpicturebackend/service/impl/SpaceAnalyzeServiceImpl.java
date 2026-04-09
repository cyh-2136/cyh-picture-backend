package com.cyh.cyhpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.model.dto.space.analyze.*;
import com.cyh.cyhpicturebackend.model.dto.user.analyze.UserUploadAnalyzeRequest;
import com.cyh.cyhpicturebackend.model.entity.Picture;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.vo.analyze.*;
import com.cyh.cyhpicturebackend.service.PictureService;
import com.cyh.cyhpicturebackend.service.SpaceAnalyzeService;
import com.cyh.cyhpicturebackend.service.SpaceService;
import com.cyh.cyhpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl  implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;


    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest 空间分析请求参数
     * @param loginUser 登录用户
     */
    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 校验空间分析权限
        // 全空间分析需要管理员权限
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问公共图库");
        }else {
            // 私有空间权限校验
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }


    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeRequest SpaceUsageAnalyzeRequest 请求参数
     * @param loginUser                当前登录用户
     * @return SpaceUsageAnalyzeResponse 分析结果
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser){
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();

        //1.校验参数
        //公共图库需要从picture表中查询
        if (queryPublic || queryAll) {
            //权限校验
            this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询条件
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            //查询图库使用空间
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            //objects转化为long类型
            long usedSize = pictureObjList.stream().mapToLong(object->(Long) object).sum();
            long usedCount = pictureObjList.size();
            //返回结果(公共图库，无数量空间限制，也没有比例)
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //(公共图库，无数量空间限制，也没有比例)
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        }else {
            //私有空间需要从space表中查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //构造返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算空间使用比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize()* 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            //计算图片数量占比
            double countUsageRatio = NumberUtil.round(space.getTotalCount()* 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 获取空间图片分类分析数据
     * @param spaceCategoryAnalyzeRequest SpaceCategoryAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceCategoryAnalyzeResponse 分析结果
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest==null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        //2.校验空间分析权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //补充查询条件
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        //分组查询图片分类
        queryWrapper.select("category", "count(*) as count","sum(picSize) as totalSize")
                .groupBy("category");

        List<SpaceCategoryAnalyzeResponse> collect = pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(object -> {
                    String category = (String) object.get("category");
                    Long count = ((Number) object.get("count")).longValue();
                    Long totalSize = ((Number) object.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 获取空间标签分析数据
     * @param spaceTagAnalyzeRequest SpaceTagAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceTagAnalyzeResponse 分析结果
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser){
        //1.校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeRequest==null, ErrorCode.PARAMS_ERROR);
        //2.校验空间分析权限
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //补充查询条件
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags")
                .groupBy("tags");
        //查询标签分析数据
        //3.解析标签分析数据
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        Map<String, Long> tagCountMap = tagsJsonList.stream()
                //将["java","python"] -> "java","python"
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        //4.构造返回结果
        List<SpaceTagAnalyzeResponse> collect = tagCountMap.entrySet().stream()
                //降序排序
                .sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return collect;
    }

    /**
     * 根据请求参数填充空间分析查询包装器
     * @param spaceAnalyzeRequest 空间分析请求参数
     * @param queryWrapper 查询包装器
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 获取空间图片大小分析数据
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(object -> (Long) object)
                .collect(Collectors.toList());

        //定义图片分段范围
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        List<SpaceSizeAnalyzeResponse> collect = sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return collect;
    }


    /**
     * 获取空间用户分析数据
     * @param spaceUserAnalyzeRequest SpaceUserAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceUserAnalyzeResponse 分析结果
     **/
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        //补充用户id查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                // 每日分析
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                // 每周分析
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                // 每月分析
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的时间维度");
        }

        //分组排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 执行查询
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);

        List<SpaceUserAnalyzeResponse> collect = queryResult.stream()
                .map(object -> {
                    String period = object.get("period").toString();
                    Long count = ((Number) object.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * 获取空间排名分析数据
     * @param spaceRankAnalyzeRequest SpaceRankAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return List<Space> 排名前 N 的空间
     **/
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalCount")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

        // 执行查询
        return spaceService.list(queryWrapper);
    }

    /**
     * 获取用户上传图片行为分析数据
     * @param userUploadAnalyzeRequest UserUploadAnalyzeRequest 请求参数
     * @param loginUser 当前登录用户
     * @return SpaceUserAnalyzeResponse 分析结果
     **/
    @Override
    public List<UserUploadAnalyzeResponse> getUserUploadAnalyze(UserUploadAnalyzeRequest userUploadAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(userUploadAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限
        checkSpaceAnalyzeAuth(userUploadAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(userUploadAnalyzeRequest, queryWrapper);

        // 获取参数
        Long userId = userUploadAnalyzeRequest.getUserId();

        // 当 userId 非空时，只查询该用户的上传数据
        if (userId != null) {
            queryWrapper.eq("userId", userId);
        }
        /**
         * 时间范围查询
         */
        Date startTime = userUploadAnalyzeRequest.getStartTime();
        Date endTime = userUploadAnalyzeRequest.getEndTime();
        if (ObjUtil.isNotNull(startTime)) {
            queryWrapper.ge("updateTime", startTime);
        }
        if (ObjUtil.isNotNull(endTime)) {
            queryWrapper.le("updateTime", endTime);
        }

        // 按用户分组统计上传次数
        queryWrapper.select("userId", "COUNT(*) AS count")
                .groupBy("userId")
                .orderByDesc("count");

        // 当 userId 为空时，只取前 10 个高活跃用户
        if (userId == null) {
            queryWrapper.last("LIMIT 10");
        }

        // 执行查询
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);

        List<UserUploadAnalyzeResponse> collect = queryResult.stream()
                .map(object -> {
                    Long userIdValue = ((Number) object.get("userId")).longValue();
                    Long count = ((Number) object.get("count")).longValue();
                    return new UserUploadAnalyzeResponse(userIdValue, count);
                })
                .collect(Collectors.toList());

        return collect;
    }
}
