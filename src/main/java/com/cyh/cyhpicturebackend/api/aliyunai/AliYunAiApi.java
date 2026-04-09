package com.cyh.cyhpicturebackend.api.aliyunai;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.CreateImageGenerationTaskRequest;
import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.CreateImageGenerationTaskResponse;
import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.GetImageGenerationTaskResponse;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskRequest;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.GetOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.model.dto.picture.CreateGenerationImageTaskRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态GET https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 创建图像生成任务地址
    public static final String CREATE_IMAGE_GENERATION_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image-generation/generation";

    // 查询图像生成任务状态地址
    public static final String GET_IMAGE_GENERATION_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";



    /**
     * 创建扩图任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {

        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR,"扩图参数不能为空");

        /**
         * 请求格式
         * curl --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting' \
        --header "Authorization: Bearer $DASHSCOPE_API_KEY" \
        --header 'X-DashScope-Async: enable' \
        --header 'Content-Type: application/json' \
        --data '{
        "model": "image-out-painting",
                "input": {
            "image_url": "http://xxx/image.jpg"
        },
        "parameters":{
            "angle": 45,
                    "x_scale":1.5,
                    "y_scale":1.5
        }
        }'*/
        // 调用阿里云API
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                //必须设置为enable，异步处理
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        //处理响应
        try(HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()){
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"AI 扩图请求失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse.getCode()!= null){
                String errorMessage = createOutPaintingTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图请求失败"+errorMessage);
            }

            return createOutPaintingTaskResponse;
        }
    }


    /**
     * 查询扩图任务状态
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StringUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR,"任务id不能为空");

        //处理响应
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * 创建图像生成任务
     *
     * @param createImageGenerationTaskRequest
     * @return
     */
    public CreateImageGenerationTaskResponse createImageGenerationTask(CreateImageGenerationTaskRequest createImageGenerationTaskRequest) {
        ThrowUtils.throwIf(createImageGenerationTaskRequest == null, ErrorCode.PARAMS_ERROR,"图像生成参数不能为空");


        /**
         * 请求格式
         * curl -X POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis \
         *     -H 'X-DashScope-Async: enable' \
         *     -H "Authorization: Bearer $DASHSCOPE_API_KEY" \
         *     -H 'Content-Type: application/json' \
         *     -d '{
         *     "model": "qwen-image-plus",
         *     "input": {
         *         "prompt": "一副典雅庄重的对联悬挂于厅堂之中，房间是个安静古典的中式布置，桌子上放着一些青花瓷，对联上左书“义本生知人机同道善思新”，右书“通云赋智乾坤启数高志远”， 横批“智启千问”，字体飘逸，在中间挂着一幅中国风的画作，内容是岳阳楼。"
         *     },
         *     "parameters": {
         *         "negative_prompt":" ",
         *         "size": "1664*928",
         *         "n": 1,
         *         "prompt_extend": true,
         *         "watermark": false
         *     }
         * }'
         */
        // 调用阿里云API
        HttpRequest httpRequest = HttpRequest.post(CREATE_IMAGE_GENERATION_TASK_URL)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .body(JSONUtil.toJsonStr(createImageGenerationTaskRequest));

        //处理响应
        try(HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像生成请求失败");
            }
            CreateImageGenerationTaskResponse createImageGenerationTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateImageGenerationTaskResponse.class);
            if (createImageGenerationTaskResponse.getCode()!= null){
                String errorMessage = createImageGenerationTaskResponse.getMessage();
                log.error("请求异常：{}", errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像生成请求失败"+errorMessage);
            }

            return createImageGenerationTaskResponse;
        }


    }


    //b9745fac-ff18-405d-b167-e21648900d59
    /**
     * 查询图像生成任务状态
     * @param taskId
     * @return
     */
    public GetImageGenerationTaskResponse getImageGenerationTask(String taskId) {
        ThrowUtils.throwIf(StringUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR,"任务id不能为空");

        //处理响应
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_IMAGE_GENERATION_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            //将httpResponse转换为GetImageGenerationTaskResponse对象
            //将httpResponse.body()转换为JSON字符串，再转换为GetImageGenerationTaskResponse对象

            return JSONUtil.toBean(httpResponse.body(), GetImageGenerationTaskResponse.class);
        }
    }



}
