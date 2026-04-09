package com.cyh.cyhpicturebackend.common;

import org.springframework.util.DigestUtils;

public class PasswordEncryptionUtils {

    private static final String SALT = "cyh";

    /**
    * 密码加密
    * @param password 原始密码
    * @return 加密后的密码
    */
    public static String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }
}
