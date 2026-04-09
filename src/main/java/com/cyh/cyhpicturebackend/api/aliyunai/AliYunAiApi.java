package com.cyh.cyhpicturebackend.api.aliyunai;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskRequest;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.CreateOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.api.aliyunai.model.OutPainting.GetOutPaintingTaskResponse;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
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

    /**
     * 创建任务
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
     * 查询创建的任务
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
}
