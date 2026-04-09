package com.cyh.cyhpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.CreateImageGenerationTaskRequest;
import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.CreateImageGenerationTaskResponse;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.model.dto.picture.*;
import com.cyh.cyhpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 21369
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-03-18 11:57:34
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param inputSource 图片文件或url
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return 图片视图对象
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    PictureVO batchUploadPicture(Object inputSource,
                                 PictureUploadRequest pictureUploadRequest,
                                 User loginUser, String category, List<String> tags);

    /**
     * 删除图片
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    void deletePicture(long pictureId, User loginUser);


    /**
     * 获取查询包装器
     * @param pictureQueryRequest 图片查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取查询包装器（管理员查询）
     * @param pictureQueryRequest 图片查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<Picture> getQueryWrapperForAdmin(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片封装
     * @param picture 图片实体
     * @param request HttpServletRequest对象
     * @return 图片视图对象
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * @param picturePage 图片分页对象
     * @param request HttpServletRequest对象
     * @return 图片视图分页对象
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片是否合法
     * @param picture 图片实体
     */
    void validPicture(Picture picture);

    /**
     * 审核图片
     * @param pictureReviewRequest 图片审核请求参数
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture 图片实体
     * @param loginUser 登录用户
     */
     void fillReviewParams(Picture picture,User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


    /**
     * 清除图片
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 编辑图片
     * @param pictureEditRequest 图片编辑请求参数
     * @param loginUser 登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 校验图片权限
     * @param loginUser 登录用户
     * @param picture 图片实体
     */
    void checkPictureAuth(User loginUser,Picture picture);

    /**
     * 通过颜色查找图片
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑处理图片
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建图片扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    CreateImageGenerationTaskResponse createImageGenerationTask(CreateGenerationImageTaskRequest createGenerationImageTaskRequest, User loginUser);

    boolean removeAllBySpaceId(long id);
}
