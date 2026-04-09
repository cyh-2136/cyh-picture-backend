package com.cyh.cyhpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cyh.cyhpicturebackend.model.dto.user.UserQueryRequest;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cyh.cyhpicturebackend.model.vo.LoginUserVO;
import com.cyh.cyhpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 21369
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-03-16 19:15:12
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 登录用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息
     * @param user
     * @return
     */
    LoginUserVO getUserLoginVO(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
     User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出登录
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
     UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     * @param userList
     * @return
     */
     List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将查询信息转化为QueryWrapper
     * @param userQueryRequest
     * @return
     */
     QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    /** 判断当前用户是否为管理员 **/
    boolean isAdmin(User user);


}
