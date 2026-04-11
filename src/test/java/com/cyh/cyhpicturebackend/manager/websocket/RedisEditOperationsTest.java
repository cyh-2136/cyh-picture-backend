package com.cyh.cyhpicturebackend.manager.websocket;

import com.cyh.cyhpicturebackend.config.RedisConfig;
import com.cyh.cyhpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Redis编辑操作记录测试
 */
public class RedisEditOperationsTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserService userService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PictureEditHandler pictureEditHandler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        pictureEditHandler = new PictureEditHandler();
        // 使用反射设置依赖
        try {
            java.lang.reflect.Field redisTemplateField = PictureEditHandler.class.getDeclaredField("redisTemplate");
            redisTemplateField.setAccessible(true);
            redisTemplateField.set(pictureEditHandler, redisTemplate);

            java.lang.reflect.Field userServiceField = PictureEditHandler.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(pictureEditHandler, userService);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSaveEditOperationToRedis() {
        // 测试存储编辑操作记录
        Long pictureId = 123456L;
        String editAction = PictureEditActionEnum.CROP.getValue();
        Long userId = 789012L;

        // 模拟Redis操作
        doNothing().when(redisTemplate).expire(anyString(), anyLong(), any());

        // 调用方法
        try {
            java.lang.reflect.Method saveMethod = PictureEditHandler.class.getDeclaredMethod("saveEditOperationToRedis", Long.class, String.class, Long.class);
            saveMethod.setAccessible(true);
            saveMethod.invoke(pictureEditHandler, pictureId, editAction, userId);
        } catch (Exception e) {
            e.printStackTrace();
            fail("保存操作记录失败");
        }

        // 验证Redis操作
        verify(redisTemplate, atLeastOnce()).opsForList();
        verify(redisTemplate, atLeastOnce()).expire(anyString(), anyLong(), any());
    }

    @Test
    public void testGetEditOperationsFromRedis() {
        // 测试读取编辑操作记录
        Long pictureId = 123456L;

        // 模拟Redis操作
        when(redisTemplate.opsForList()).thenReturn(mock(org.springframework.data.redis.core.ListOperations.class));

        // 调用方法
        try {
            java.lang.reflect.Method getMethod = PictureEditHandler.class.getDeclaredMethod("getEditOperationsFromRedis", Long.class);
            getMethod.setAccessible(true);
            List<Object> result = (List<Object>) getMethod.invoke(pictureEditHandler, pictureId);
            assertNotNull(result);
        } catch (Exception e) {
            e.printStackTrace();
            fail("读取操作记录失败");
        }

        // 验证Redis操作
        verify(redisTemplate, atLeastOnce()).opsForList();
    }

    @Test
    public void testCleanupRedisRecords() {
        // 测试清理Redis记录
        Long pictureId = 123456L;

        // 模拟Redis操作
        doNothing().when(redisTemplate).delete(anyString());

        // 调用方法
        try {
            java.lang.reflect.Method cleanupMethod = PictureEditHandler.class.getDeclaredMethod("cleanupRedisRecords", Long.class);
            cleanupMethod.setAccessible(true);
            cleanupMethod.invoke(pictureEditHandler, pictureId);
        } catch (Exception e) {
            e.printStackTrace();
            fail("清理Redis记录失败");
        }

        // 验证Redis操作
        verify(redisTemplate, times(2)).delete(anyString());
    }
}
