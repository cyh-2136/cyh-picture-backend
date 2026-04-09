package com.cyh.cyhpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.model.enums.PictureFormatEnum;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        String originalFilename = FileUtil.mainName(fileUrl);
        return originalFilename;
    }

    @Override
    protected void validatePicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        //校验图片是否为空
        ThrowUtils.throwIf(StringUtils.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "图片url不能为空");
        //校验图片url是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片url格式错误");
        }

        //校验url的协议
        ThrowUtils.throwIf(!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://"), ErrorCode.PARAMS_ERROR, "仅支持https和http协议");

        //发送head请求校验文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl)
                    .execute();
            //未正常返回
            if (httpResponse.getStatus() != HttpStatus.SC_OK) {
                return;
            }
            //文件存在，文件类型是否为图片(老版)
            String contentType = httpResponse.header("Content-Type");
            //不为空，才需要检验是否合法
            if (StringUtils.isBlank(contentType)) {
                //允许的文件类型(老版)
                /*final List<String> types = Arrays.asList("image/jpeg","image/jpg", "image/png", "image/webp", "image/gif");
                ThrowUtils.throwIf(!types.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");*/
                // 获取URL的文件后缀(枚举类校验)
                String fileSuffix = FileUtil.getSuffix(fileUrl);
                // 使用枚举校验文件格式
                if (!PictureFormatEnum.isSupported(fileSuffix)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片格式错误，仅支持：" + PictureFormatEnum.getAllFormats());
                }
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

    @Override
    protected java.io.InputStream getInputStream(Object inputSource) throws Exception {
        String fileUrl = (String) inputSource;
        return HttpUtil.createGet(fileUrl).execute().bodyStream();
    }

    @Override
    protected long getContentLength(Object inputSource) throws Exception {
        String fileUrl = (String) inputSource;
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (httpResponse.getStatus() == HttpStatus.SC_OK) {
                String contentLength = httpResponse.header("Content-Length");
                if (StringUtils.isNotBlank(contentLength)) {
                    return Long.parseLong(contentLength);
                }
            }
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法获取文件大小");
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }
}
