package com.cyh.cyhpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.cyh.cyhpicturebackend.config.CosClientConfig;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.manager.CosManager;
import com.cyh.cyhpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 处理文件
     * @param inputSource 输入源
     */
    protected abstract void processFile(Object inputSource,File file) throws Exception;

    /**
     * 获取原始文件名
     * @param inputSource 输入源
     * @return 原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验图片
     * @param inputSource 输入源
     */
    protected abstract void validatePicture(Object inputSource);

    /**
     * 上传图片
     * @param inputSource 图片文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(Object inputSource,String uploadPathPrefix) {
        //1.校验图片
        validatePicture(inputSource);

        //2.图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginalFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);

        /*解析结果并返回(临时本地文件方式)*/
        File file = null;
        try {
            //3.创建临时文件
            file = File.createTempFile(uploadPath, null);
            //4.处理文件
            processFile(inputSource,file);
            //5.上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //6.获取图片信息并封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollectionUtils.isNotEmpty(objectList)) {
                //webp对象对象
                CIObject compressedCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                //封装返回结果
                return buildResult(originFilename, compressedCiObject,thumbnailCiObject,imageInfo);
            }
            return  buildResult(imageInfo, originFilename, file, uploadPath);
        }catch (Exception e) {
            log.error("file upload error, fileName:"+ e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图片上传失败");
        }finally {
            //7.删除临时文件
            deleteTempFile(file);
        }
        /*解析结果并返回(流方式上传文件，更快，更省空间)*/
//        try(InputStream inputStream = getInputStream(inputSource)) {
//            //4.获取内容长度
//            long contentLength = getContentLength(inputSource);
//            //5.上传图片
//            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, inputStream, contentLength);
//            //6.获取图片信息并封装返回结果
//            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
//            List<CIObject> objectList = processResults.getObjectList();
//            if (CollectionUtils.isNotEmpty(objectList)) {
//                //webp对象对象
//                CIObject compressedCiObject = objectList.get(0);
//                CIObject thumbnailCiObject = compressedCiObject;
//                // 有生成缩略图，才得到缩略图
//                if (objectList.size() > 1) {
//                    thumbnailCiObject = objectList.get(1);
//                }
//                //封装返回结果
//                return buildResult(originFilename, compressedCiObject, thumbnailCiObject, imageInfo);
//            }
//            // 如果没有处理结果，使用原始信息构建结果
//            return buildResult(imageInfo, originFilename, contentLength, uploadPath);
//        } catch (Exception e) {
//            log.error("file upload error, fileName:" + e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
//        }
    }

    /**
     * 封装返回结果
     * @param imageInfo 对象存储返回的图片信息
     * @param originFilename 原始文件名
     * @param file 临时文件
     * @param uploadPath 上传路径
     * @return 上传结果
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String originFilename, File file, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picwidth = imageInfo.getWidth();
        int picheight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picwidth * 1.0 / picheight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picwidth);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicHeight(picheight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+ uploadPath);
        return uploadPictureResult;
    }

    /**
     * 封装返回结果（压缩图片和缩略图）
     * @param originFilename 原始文件名
     * @param compressedCiObject 压缩后的图片对象
     * @param thumbnailCiObject 缩略图对象
     * @return 上传结果
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject,CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片为压缩后的地址
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());

        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果（流方式）
     * @param imageInfo 对象存储返回的图片信息
     * @param originFilename 原始文件名
     * @param contentLength 内容长度
     * @param uploadPath 上传路径
     * @return 上传结果
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String originFilename, long contentLength, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picwidth = imageInfo.getWidth();
        int picheight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picwidth * 1.0 / picheight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picwidth);
        uploadPictureResult.setPicColor(imageInfo.getAve());
        uploadPictureResult.setPicHeight(picheight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(contentLength);
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     * @param file 临时文件
     */
    private void deleteTempFile(File file) {
        if (file != null) {
            //删除临时文件
            boolean deleteResult = file.delete();
            if (!deleteResult) {
                log.error("file delete error, filePath:"+file.getAbsolutePath());
            }
        }
    }

    /**
     * 获取输入流
     * @param inputSource 输入源
     * @return 输入流
     */
    protected abstract java.io.InputStream getInputStream(Object inputSource) throws Exception;

    /**
     * 获取内容长度
     * @param inputSource 输入源
     * @return 内容长度
     */
    protected abstract long getContentLength(Object inputSource) throws Exception;


}
