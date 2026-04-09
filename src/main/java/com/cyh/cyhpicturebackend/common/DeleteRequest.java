package com.cyh.cyhpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求(通用，传入id)
 *
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}