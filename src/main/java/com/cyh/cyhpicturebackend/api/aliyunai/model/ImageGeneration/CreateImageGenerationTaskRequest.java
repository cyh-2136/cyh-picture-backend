package com.cyh.cyhpicturebackend.api.aliyunai.model.ImageGeneration;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CreateImageGenerationTaskRequest implements Serializable {

    /**
     * 模型名称
     * 示例值：wan2.6-t2i
     */
    private String model = "wan2.6-t2i";

    /**
     * 输入的基本信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    /**
     * 输入的基本信息
     */
    @Data
    public static class Input {

        /**
         * 请求内容数组
         * 当前仅支持单轮对话，即传入一组role、content参数，不支持多轮对话
         */
        private List<Message> messages;

        /**
         * 消息
         */
        @Data
        public static class Message {

            /**
             * 消息的角色
             * 此参数必须设置为 user
             */
            private String role = "user";

            /**
             * 消息内容数组
             */
            private List<Content> content;

            /**
             * 消息内容
             */
            @Data
            public static class Content {

                /**
                 * 正向提示词，用于描述期望生成的图像内容、风格和构图
                 * 支持中英文，长度不超过2100个字符，每个汉字、字母、数字或符号计为一个字符，超过部分会自动截断
                 * 示例值：一间有着精致窗户的花店，漂亮的木质门，摆放着花朵
                 * 注意：仅支持传入一个text，不传或传入多个将报错
                 */
                private String text;
            }
        }
    }

    /**
     * 图像处理参数
     */
    @Data
    public static class Parameters {

        /**
         * 反向提示词，用于描述不希望在图像中出现的内容，对画面进行限制
         * 支持中英文，长度不超过500个字符，超出部分将自动截断
         * 示例值：低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲
         */
        @Alias("negative_prompt")
        private String negativePrompt;

        /**
         * 输出图像的分辨率，格式为 宽*高
         * 默认值为 1280*1280
         * 总像素在 [1280*1280, 1440*1440] 之间且宽高比范围为 [1:4, 4:1]。例如，768*2700符合要求
         * 示例值：1280*1280
         * 常见比例推荐的分辨率
         * 1:1：1280*1280
         * 3:4：1104*1472
         * 4:3：1472*1104
         * 9:16：960*1696
         * 16:9：1696*960
         */
        private String size;

        /**
         * 生成图片的数量
         * 取值范围为1~4张，默认为 4
         * 注意：按张计费，测试建议设为 1
         */
        private Integer n = 1;

        /**
         * 是否开启prompt智能改写
         * 开启后，将使用大模型优化正向提示词，对较短的提示词有明显提升效果，但增加3-4秒耗时
         * true：默认值，开启智能改写
         * false：不开启智能改写
         */
        @Alias("prompt_extend")
        private Boolean promptExtend = true;

        /**
         * 是否添加水印标识，水印位于图片右下角，文案固定为"AI生成"
         * false：默认值，不添加水印
         * true：添加水印
         */
        private Boolean watermark = false;

        /**
         * 随机数种子，取值范围 [0,2147483647]
         * 使用相同的 seed 参数值可使生成内容保持相对稳定。若不提供，算法将自动使用随机数种子
         * 注意：模型生成过程具有概率性，即使使用相同的 seed，也不能保证每次生成结果完全一致
         */
        private Integer seed;
    }
}
