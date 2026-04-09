package com.cyh.cyhpicturebackend.model.dto.picture;

import com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration.CreateImageGenerationTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateGenerationImageTaskRequest implements Serializable {

    /**
     * 图像描述
     */
    private CreateImageGenerationTaskRequest.Input.Message.Content content;

    private static final long serialVersionUID = 1L;
}
