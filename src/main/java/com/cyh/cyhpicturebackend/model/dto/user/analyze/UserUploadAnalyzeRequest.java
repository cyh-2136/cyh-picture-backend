package com.cyh.cyhpicturebackend.model.dto.user.analyze;

import com.cyh.cyhpicturebackend.model.dto.space.analyze.SpaceAnalyzeRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserUploadAnalyzeRequest extends SpaceAnalyzeRequest {
    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 开始时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 结束时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
}
