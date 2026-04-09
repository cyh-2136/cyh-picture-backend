package com.cyh.cyhpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validatePicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
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

    @Override
    protected java.io.InputStream getInputStream(Object inputSource) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getInputStream();
    }

    @Override
    protected long getContentLength(Object inputSource) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getSize();
    }
}
