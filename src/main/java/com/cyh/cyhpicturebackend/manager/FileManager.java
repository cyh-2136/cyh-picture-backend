package com.cyh.cyhpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cyh.cyhpicturebackend.config.CosClientConfig;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * 文件服务
 * @deprecated 该服务已被 upload包 替代
 */
@Deprecated
@Slf4j
@Service
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片
     * @param multipartFile 图片文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //校验图片
        validatePicture(multipartFile);

        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);

        //解析结果并返回
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picwidth = imageInfo.getWidth();
            int picheight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picwidth * 1.0 / picheight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picwidth);
            uploadPictureResult.setPicHeight(picheight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return uploadPictureResult;
        }catch (Exception e) {
            log.error("file upload error, fileName:"+ e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图片上传失败");
        }finally {
            //删除临时文件
            deleteTempFile(file);
        }
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
     * 校验图片
     * @param multipartFile 图片文件
     */
    private void validatePicture(MultipartFile multipartFile) {
        //校验图片是否为空
        ThrowUtils.throwIf(multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "图片文件不能为空");

        //校验图片大小是否超过10MB
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "图片大小不能超过2MB");

        //检验图片格式是否正确
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许的文件格式
        final List<String> suffixList = Arrays.asList("jpg", "png", "jpeg", "webp");
        if (!suffixList.contains(fileSuffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式错误");
        }
    }


    /**
     * 上传图片(通过url)
     * @param fileUrl 图片url
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl,String uploadPathPrefix) {
        //校验图片(方法重载)
        validatePicture(fileUrl);

        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = FileUtil.mainName(fileUrl);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);

        //解析结果并返回
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);

            //multipartFile.transferTo(file);
            //下载图片到临时文件
            HttpUtil.downloadFile(fileUrl,file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picwidth = imageInfo.getWidth();
            int picheight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picwidth * 1.0 / picheight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicWidth(picwidth);
            uploadPictureResult.setPicHeight(picheight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+uploadPath);
            return uploadPictureResult;
        }catch (Exception e) {
            log.error("file upload error, fileName:"+ e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"图片上传失败");
        }finally {
            //删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验图片url是否合法
     * @param fileUrl 图片url
     */
    private void validatePicture(String fileUrl) {
        //校验图片是否为空
        ThrowUtils.throwIf(StringUtils.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片url不能为空");
        //校验图片url是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片url格式错误");
        }

        //校验url的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("https://") || !fileUrl.startsWith("http://"), ErrorCode.PARAMS_ERROR, "仅支持https和http协议");

        //发送head请求校验文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl)
                    .execute();
            //未正常返回
            if (httpResponse.getStatus() != HttpStatus.SC_OK) {
                return;
            }
            //文件存在，文件类型是否为图片
            String contentType = httpResponse.header("Content-Type");
            //不为空，才需要检验是否合法
            if (StringUtils.isBlank(contentType)) {
                //允许的文件类型
                final List<String> types = Arrays.asList("image/jpeg","image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!types.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //文件存在，校验文件大小

            try {
                long fileSize = Long.parseLong(httpResponse.header("Content-Length"));
                final long ONE_MB = 1024 * 1024;
                ThrowUtils.throwIf(fileSize > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "图片大小不能超过2MB");
            }catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小格式错误");
            }
        }finally {
            if (httpResponse != null) {
                //关闭连接
                httpResponse.close();
            }
        }
    }


}
