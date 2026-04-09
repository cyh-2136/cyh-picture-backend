package com.cyh.cyhpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyh.cyhpicturebackend.api.aliyunai.AliYunAiApi;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskRequest;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.config.CosClientConfig;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.manager.CacheManager;
import com.cyh.cyhpicturebackend.manager.CosManager;
import com.cyh.cyhpicturebackend.manager.upload.FilePictureUpload;
import com.cyh.cyhpicturebackend.manager.upload.PictureUploadTemplate;
import com.cyh.cyhpicturebackend.manager.upload.UrlPictureUpload;
import com.cyh.cyhpicturebackend.model.dto.file.UploadPictureResult;
import com.cyh.cyhpicturebackend.model.dto.picture.*;
import com.cyh.cyhpicturebackend.model.entity.InfomationUsedByUploadPicture;
import com.cyh.cyhpicturebackend.model.entity.Picture;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.enums.PictureReviewStatusEnum;
import com.cyh.cyhpicturebackend.model.vo.PictureVO;
import com.cyh.cyhpicturebackend.model.vo.UserVO;
import com.cyh.cyhpicturebackend.service.PictureService;
import com.cyh.cyhpicturebackend.service.SpaceService;
import com.cyh.cyhpicturebackend.service.UserService;
import com.cyh.cyhpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.cyh.cyhpicturebackend.mapper.PictureMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 21369
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-18 11:57:34
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    private static final String CACHE_NAME = "cyhpicture";

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private CacheManager cacheManager;

    /**
     * 上传图片
     * @param inputSource 要上传的文件或url
     * @param pictureUploadRequest 图片上传请求参数
     * @param loginUser 登录用户
     * @return 图片VO
     */
    @Override
    public PictureVO uploadPicture(Object inputSource,
                                   PictureUploadRequest pictureUploadRequest,
                                    User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NO_AUTH_ERROR);

        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传（权限校验已在 PictureController 中进行）
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        //判断是新增还是修改
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        Picture oldPicture = null;
        //如果是更新，校验图片是否存在
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture==null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            //仅本人或者管理员可以更新（权限校验已在 PictureController 中进行）
//            if (oldPicture.getUserId()!=loginUser.getId() && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您没有权限更新该图片");
//            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }

            //校验图片是否存在
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR);
        }
        //上传图片
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        //根据inputSource 类型判断是文件上传还是url上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        // 补充设置 spaceId
        log.info("开始设置图片参数 - picture-spaceId: {}", spaceId);
        picture.setSpaceId(spaceId);
        // 设置压缩后的地址
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置缩略图地址
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        //补充其他字段
        this.fillReviewParams(picture,loginUser);
        //如果pictureId不为空，表示更新，否则表示新增
        if (pictureId != null) {
            //如果更新，校验图片是否存在，补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            // TODO： 清除旧图片的COS文件
            clearPictureFile(oldPicture);
        }
        //构造跟新前的图片大小和图片数量，用于更新空间额度
        InfomationUsedByUploadPicture beforePicture = new InfomationUsedByUploadPicture();
        if (pictureId != null) {
            beforePicture.setPicSize(picture.getPicSize());
            beforePicture.setPicCount(1L);
        }else{
            beforePicture.setPicSize(0L);
            beforePicture.setPicCount(0L);
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + (picture.getPicSize()-beforePicture.getPicSize()))
                        .setSql("totalCount = totalCount + " + (1L-beforePicture.getPicCount()))
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度信息更新失败");
            }
            return picture;
        });

        // 上传成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);

        //返回VO
        return PictureVO.objToVo(picture);
    }

    /**
     * 管理员批量抓取上传图片(一定是新增)
     * @param inputSource 要上传的文件或url
     * @param pictureUploadRequest 图片上传请求参数
     * @param loginUser 登录用户
     * @return 图片VO
     */
    @Override
    public PictureVO batchUploadPicture(Object inputSource,
                                        PictureUploadRequest pictureUploadRequest,
                                        User loginUser, String category, List<String> tags) {
        //校验参数
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NO_AUTH_ERROR);

        //判断是新增还是修改
        Long pictureId = pictureUploadRequest.getId();

        //上传图片
        //是文件上传
        PictureUploadTemplate pictureUploadTemplate = urlPictureUpload;
        String uploadPathPrefix=String.format("public/%s", loginUser.getId());

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        // 设置压缩后的地址
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置缩略图地址
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        //补充其他字段
        this.fillReviewParams(picture,loginUser);
        if (category != null) {
            picture.setCategory(category);
        }
        if (tags != null) {
            picture.setTags(JSONUtil.toJsonStr(tags));
        }

        // 开启事务
        transactionTemplate.execute(status -> {
            boolean result = this.save(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            return picture;
        });

        // 上传成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
        //返回VO
        return PictureVO.objToVo(picture);
    }

    /**
     * 删除图片
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        // 改为使用注解校验权限
        //checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            //私有空间的图片才需要更新额度信息
            if (oldPicture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });

        // 删除成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);

        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    /**
     * 获取查询包装器
     * @param pictureQueryRequest 图片查询请求参数
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // 空间查询逻辑
        if (nullSpaceId) {
            // 只查询公共图库（spaceId 为 null）
            queryWrapper.isNull("spaceId");
        } else if (spaceId != null) {
            // 查询指定空间
            queryWrapper.eq("spaceId", spaceId);
        }


        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取查询包装器（管理员查询，查询所有图片，包括公共图库和空间图库）
     * @param pictureQueryRequest 图片查询请求参数
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapperForAdmin(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // 空间查询逻辑（管理员查询）
//        if (nullSpaceId) {
//            // 只查询公共图库（spaceId 为 null）
//            queryWrapper.isNull("spaceId");
//        } else if (spaceId != null) {
//            // 查询指定空间
//            queryWrapper.eq("spaceId", spaceId);
//        }


        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 图片封装
     * @param picture 图片实体
     * @param request HttpServletRequest对象
     * @return 图片视图对象
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }


    /**
     * 分页获取图片封装
     * @param picturePage 图片分页对象
     * @param request HttpServletRequest对象
     * @return 图片视图分页对象
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 校验图片是否合法
     * @param picture 图片实体
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    /**
     * 审核图片
     * @param pictureReviewRequest 图片审核请求参数
     * @param loginUser 登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        if (id == null  || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }
        //2.校验图片是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //3.校验审核状态是否重复
        if (picture.getReviewStatus() == reviewStatusEnum.getValue()) {
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR);
        }
        //4.更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        updatePicture.setEditTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 审核成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
    }

    /**
     * 填充审核参数
     * @param picture 图片实体
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture,User loginUser) {
        log.info("开始设置审核参数 - userId: {}, spaceId: {}, isAdmin: {}",
                loginUser.getId(), picture.getSpaceId(), userService.isAdmin(loginUser));
        //管理员审核
        //如果是上传到私有空间的图片，即使是管理员也默认审核未通过
        if (userService.isAdmin(loginUser) && picture.getSpaceId() == null) {
            log.info("管理员上传到公共空间，自动过审");
            //自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
            picture.setEditTime(new Date());
            picture.setReviewerId(loginUser.getId());
        }else{
            log.info("设置为未审核状态");
            //非管理员默认不通过
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
        log.info("审核状态设置完成 - reviewStatus: {}", picture.getReviewStatus());
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1.校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        //抓取数量
        Integer offset = pictureUploadByBatchRequest.getOffset();
        String category = pictureUploadByBatchRequest.getCategory();
        List<String> tags = pictureUploadByBatchRequest.getTags();
        ThrowUtils.throwIf(count>=30, ErrorCode.PARAMS_ERROR, "抓取数量不能超过30");
        //校验偏移量
        ThrowUtils.throwIf(offset<0 || offset>100, ErrorCode.PARAMS_ERROR, "抓取偏移量不能小于0或大于100");
        //名称前缀
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isNotBlank(namePrefix)) {
            namePrefix = searchText;
        }
        //2.抓取内容
        String fetchUrl = StrUtil.format("https://cn.bing.com/images/async?q="+searchText+"&mmasync=1");
        log.info("抓取地址:{}", fetchUrl);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        //3.解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到图片内容");
        }
        Elements imgElements = div.select("img.ming");
        if (imgElements.isEmpty()) {
            // 备选选择器
            imgElements = div.select("img[class*=mimg]");
        }
        if (imgElements.isEmpty()) {
            // 通用备选
            imgElements = div.select(".img_cont img");
        }
        if (imgElements.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到图片元素");
        }
        int uploadCount = 0;
        int start = 0;
        //利用offset偏移量，只抓取指定数量的图片
        for (Element imgElement : imgElements) {
            if (start < offset) {
                start++;
                continue;
            }
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前连接为空，跳过:{}", fileUrl);
                continue;
            }
            //处理图片上传地址，防止转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex >= -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //4.上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.batchUploadPicture(fileUrl, pictureUploadRequest, loginUser, category, tags);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        // 上传成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
        return uploadCount;
    }


    /**
     * 清除图片
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //获取文件地址
        String fileUrl = oldPicture.getUrl();

        //判断该图片是否被多条记录引用
        Long count = this.lambdaQuery().eq(Picture::getUrl, fileUrl)
                .count();
        if (count > 1) {
            //被多条记录引用，不删除
            return;
        }

        // 从完整 URL 中提取 key（存储路径）
        String key = extractKeyFromUrl(oldPicture.getUrl());
        //删除webp图
        cosManager.deleteObject(key);

        //获取原始文件的保存路径
        String originalUrl = key.substring(0,key.length()-4)+"jpg";
        //删除原始文件
        if (StrUtil.isNotBlank(originalUrl)) {
            cosManager.deleteObject(originalUrl);
        }

        String thumbUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbUrl)) {
            thumbUrl = extractKeyFromUrl(thumbUrl);
            cosManager.deleteObject(thumbUrl);
        }
        // 删除成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
    }

    /**
     * 编辑图片
     * @param pictureEditRequest 图片编辑请求参数
     * @param loginUser 登录用户
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string(手动转化，以废弃)
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //picture.setTags(pictureEditRequest.getTags());

        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        // 改为使用注解校验权限
        //checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        log.info("开始设置图片参数 - picture-spaceId: {}", picture.getSpaceId());
        // 保持旧图片的 spaceId
        picture.setSpaceId(oldPicture.getSpaceId());
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 编辑成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
    }


    /**
     * 校验图片权限
     * @param loginUser 登录用户
     * @param picture 图片实体
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 通过颜色查找图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(spaceId == null || StringUtils.isBlank(picColor) || loginUser==null, ErrorCode.PARAMS_ERROR);
        //2.校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间访问权限");
        }

        //3.查询该空间内的所有图片（有主色调属性的）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();

        //如果没有图片就返回
        if (CollUtil.isEmpty(pictureList)){
            return new ArrayList<>();
        }

        //4.将颜色字符串转化为主色调
        Color targetColor = Color.decode(picColor);

        //5.计算相似度并按相似度排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    //计算相似度
                   return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
                }))
                // 取前 12 个
                .limit(12)
                .collect(Collectors.toList());

        //6.返回
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 批量编辑处理图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        //1.获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null , ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2.校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间访问权限");
        }
        //3.查询图片是否存在（进选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId,Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        //4.更新图片
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量重命名
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        //5.操作数据库
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 编辑成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule)|| CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        }catch (Exception e){
            log.error("名称解析失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"名称解析失败");
        }

    }

    /**
     * 创建图片扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        // 改为使用注解校验权限
        //checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    /**
     * 删除空间下的所有图片
     * @param id
     * @return
     */
    @Override
    public boolean removeAllBySpaceId(long id) {
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR, "空间ID不能为空");
        // 获取空间下的所有图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, id)
                .list();

        // 校验图片是否存在
        if (CollUtil.isEmpty(pictureList)) {
            return true;
        }

        // 删除空间下的所有图片
        for (Picture picture : pictureList) {
            boolean result = this.removeById(picture.getId());
            this.clearPictureFile(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        }
        // 删除成功后刷新缓存
        cacheManager.refreshCache(CACHE_NAME);
        return true;
    }

    /**
     * 批量编辑图片分类和标签(线程池示例代码)
    @Resource
    private ThreadPoolExecutor customExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchEditPictureMetadata(PictureBatchEditRequest request, Long spaceId, Long loginUserId) {
        // 参数校验
        validateBatchEditRequest(request, spaceId, loginUserId);

        // 查询空间下的图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, request.getPictureIds())
                .list();

        if (pictureList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "指定的图片不存在或不属于该空间");
        }

        // 分批处理避免长事务
        int batchSize = 100;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            List<Picture> batch = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));

            // 异步处理每批数据
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                batch.forEach(picture -> {
                    // 编辑分类和标签
                    if (request.getCategory() != null) {
                        picture.setCategory(request.getCategory());
                    }
                    if (request.getTags() != null) {
                        picture.setTags(String.join(",", request.getTags()));
                    }
                });
                boolean result = this.updateBatchById(batch);
                if (!result) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量更新图片失败");
                }
            }, customExecutor);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }*/


    /**
     * 从完整 URL 中提取文件 key
     *
     * @param url 完整图片 URL，如 https://bucket.cos.ap-guangzhou.myqcloud.com/images/photo.jpg
     * @return 文件 key，如 images/photo.jpg
     */
    private String extractKeyFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }

        // 获取配置中的域名
        String host = cosClientConfig.getHost();

        // 移除域名部分，得到 key
        String key = url;
        if (url.startsWith(host)) {
            key = url.substring(host.length());
        }

        // 移除开头的斜杠
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        return key;
    }



}




