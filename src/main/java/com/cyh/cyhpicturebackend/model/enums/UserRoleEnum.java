package com.cyh.cyhpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;


/**
 * 用户角色枚举
 * user-用户 admin-管理员
 */
@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举(如果枚举值特别多，可以 Map 缓存所有枚举值来加速查找，而不是遍历列表)
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
