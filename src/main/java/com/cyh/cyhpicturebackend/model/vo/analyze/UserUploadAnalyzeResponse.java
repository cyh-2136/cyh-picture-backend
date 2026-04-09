package com.cyh.cyhpicturebackend.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUploadAnalyzeResponse {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 上传次数
     */
    private Long count;
}
