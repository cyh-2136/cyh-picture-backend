package com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration;

import cn.hutool.core.annotation.Alias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetImageGenerationTaskResponse {

    @Alias("request_id")
    private String requestId;

    private Output output;
    private Usage usage;   // usage 与 output 平级

    @Data
    public static class Output {
        @Alias("task_id")
        private String taskId;

        @Alias("task_status")
        private String taskStatus;

        @Alias("submit_time")
        private String submitTime;

        @Alias("scheduled_time")
        private String scheduledTime;

        @Alias("end_time")
        private String endTime;

        private Boolean finished;
        private List<Choice> choices;   // 关键：替换 results
    }

    @Data
    public static class Choice {
        @Alias("finish_reason")
        private String finishReason;
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private List<Content> content;
    }

    @Data
    public static class Content {
        private String image;
        private String type;
    }

    @Data
    public static class Usage {
        @Alias("image_count")
        private Integer imageCount;

        private String size;
        @Alias("total_tokens")
        private Integer totalTokens;
        @Alias("output_tokens")
        private Integer outputTokens;
        @Alias("input_tokens")
        private Integer inputTokens;
    }
}