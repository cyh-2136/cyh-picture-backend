package com.cyh.cyhpicturebackend.controller;

import com.cyh.cyhpicturebackend.annotation.AuthCheck;
import com.cyh.cyhpicturebackend.common.BaseResponse;
import com.cyh.cyhpicturebackend.common.ResultUtils;
import com.cyh.cyhpicturebackend.constant.UserConstant;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试上传文件到COS
     * @param multipartFile 要上传的文件
     * @return 上传到COS的路径
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUpload(@RequestParam("file") MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);

        File file = null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filePath, file);
            //返回上传到COS的路径
            return ResultUtils.success(filePath);
        }catch (Exception e) {
            log.error("file upload error, fileName:"+fileName + e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }finally {
            if (file != null) {
                //删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, fileName:"+fileName);
                }
            }
        }
    }

    /**
     * 测试从COS下载文件
     * @param filepath 要下载的文件路径
     * @return 下载到本地的文件
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownloadFile(String filepath , HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            //处理下载文件
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            //设置响应头
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            //写入响应体
            response.getOutputStream().write(bytes);
            //刷新响应体
            response.getOutputStream().flush();
        }catch (Exception e) {
            log.error("file download error, filepath:"+filepath + e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        }finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }


}

