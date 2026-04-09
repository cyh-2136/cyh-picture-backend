package com.cyh.cyhpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cyh.cyhpicturebackend.config.RedissonConfig;
import com.cyh.cyhpicturebackend.exception.BusinessException;
import com.cyh.cyhpicturebackend.exception.ErrorCode;
import com.cyh.cyhpicturebackend.exception.ThrowUtils;
import com.cyh.cyhpicturebackend.manager.sharding.DynamicShardingManager;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceAddRequest;
import com.cyh.cyhpicturebackend.model.dto.space.SpaceQueryRequest;
import com.cyh.cyhpicturebackend.model.entity.Picture;
import com.cyh.cyhpicturebackend.model.entity.Space;
import com.cyh.cyhpicturebackend.model.entity.SpaceUser;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.enums.SpaceLevelEnum;
import com.cyh.cyhpicturebackend.model.enums.SpaceRoleEnum;
import com.cyh.cyhpicturebackend.model.enums.SpaceTypeEnum;
import com.cyh.cyhpicturebackend.model.vo.PictureVO;
import com.cyh.cyhpicturebackend.model.vo.SpaceVO;
import com.cyh.cyhpicturebackend.model.vo.UserVO;
import com.cyh.cyhpicturebackend.service.SpaceService;
import com.cyh.cyhpicturebackend.mapper.SpaceMapper;
import com.cyh.cyhpicturebackend.service.SpaceUserService;
import com.cyh.cyhpicturebackend.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author 21369
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2026-03-21 14:28:38
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{


    @Resource
    private UserService userService;

    //编程式事务
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private RedissonClient redissonClient;

    //小项目无需动态分表
    //@Resource
    //@Lazy
    //private DynamicShardingManager dynamicShardingManager;




    /**
     * 创建空间
     * @param spaceAddRequest 空间添加请求参数
     * @param loginUser 登录用户
     * @return 空间id
     */
    @Transactional
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //1.填充参数默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())){
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null){
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        //填充容量和大小
        this.fillSpaceBySpaceLevel(space);

        //2.校验参数
        this.validSpace(space, true);

        //3.校验权限
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间");
        }

        /*4.控制同一个用户只能创建一个私有空间(本地锁)*/
        //        String lock = String.valueOf(userId).intern();
//        synchronized (lock) {
//            Long newSpaceId = transactionTemplate.execute(status -> {
//                //校验用户是否已创建私有空间
//                if (!userService.isAdmin(loginUser)) {
//                    boolean exists = this.lambdaQuery()
//                            .eq(Space::getUserId, userId)
//                            .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
//                            .exists();
//                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
//                }
//                //创建私有空间
//                boolean result = this.save(space);
//                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建私有空间失败");
//
//                // 如果是团队空间，关联新增团队成员记录
//                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
//                    SpaceUser spaceUser = new SpaceUser();
//                    spaceUser.setSpaceId(space.getId());
//                    spaceUser.setUserId(userId);
//                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
//                    result = spaceUserService.save(spaceUser);
//                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
//                }
//                //创建图片空间分表
//                //dynamicShardingManager.createSpacePictureTable(space);
//
//                return space.getId();
//            });
            //return Optional.ofNullable(newSpaceId).orElse(-1L);
        //}
        /*4.控制同一个用户只能创建一个私有空间(分布式锁Redisson)*/
        String lockKey = userId +":"+spaceAddRequest.getSpaceType();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，最多等待 10 秒，锁自动过期时间 30 秒
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    // 校验用户是否已创建私有空间
                    if (!userService.isAdmin(loginUser)) {
                        Space existSpace = spaceMapper.selectByUserIdAndTypeForUpdate(userId, spaceAddRequest.getSpaceType());
                        ThrowUtils.throwIf(existSpace != null, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                        //                        boolean exists = this.lambdaQuery()
//                                .eq(Space::getUserId, userId)
//                                .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
//                                .exists();
                        //ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建一个");
                    }
                    // 创建私有空间
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建私有空间失败");

                    // 如果是团队空间，关联新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        result = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                    }
                    // 创建图片空间分表
                    // dynamicShardingManager.createSpacePictureTable(space);

                    return space.getId();
                });

                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } else {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建空间失败，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 校验空间是否有效
     * @param space 空间
     * @param add 是否新增
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        //新增时校验
        if (add){
            if (StrUtil.isBlank(spaceName)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevelEnum == null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }

        //修改时校验
        if (spaceLevel !=null && spaceLevelEnum == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间等级不存在");
        }

        if (StrUtil.isNotBlank(spaceName) && spaceName.length()>30){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
        }

        // 修改数据时，如果要改空间级别
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }

    }


    /**
     * 获取查询包装器
     * @param spaceQueryRequest 空间查询请求参数
     * @return 查询包装器
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 从多字段中搜索
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("sortField"), sortField);



        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 空间封装
     * @param space 空间实体
     * @param request HttpServletRequest对象
     * @return 空间视图对象
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 分页获取空间封装
     * @param spacePage 空间分页对象
     * @param request HttpServletRequest对象
     * @return 空间视图分页对象
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }


    /**
     * 根据空间级别填充空间数据
     * @param space 空间实体
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

        if (spaceLevelEnum != null){
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }


    /**
     * 校验空间权限
     * @param loginUser 登录用户
     * @param space 空间实体
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员和空间创建者不能查看私有空间");
        }
    }
}




