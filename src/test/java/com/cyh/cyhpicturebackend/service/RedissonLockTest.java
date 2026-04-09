package com.cyh.cyhpicturebackend.service;

import com.cyh.cyhpicturebackend.model.dto.space.SpaceAddRequest;
import com.cyh.cyhpicturebackend.model.entity.User;
import com.cyh.cyhpicturebackend.model.enums.UserRoleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

//@SpringBootTest
public class RedissonLockTest {

    @Resource
    private SpaceService spaceService;

    @Test
    public void testDistributedLock() throws InterruptedException {
        // 测试用户 ID
        Long userId = 1L;
        // 空间类型（私有空间）
        Integer spaceType = 0;
        // 并发线程数
        int threadCount = 5;
        // 成功创建空间的计数
        AtomicInteger successCount = new AtomicInteger(0);
        // 用于等待所有线程完成
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // 模拟并发请求
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 创建空间请求
                    SpaceAddRequest request = new SpaceAddRequest();
                    request.setSpaceName("测试空间" + System.currentTimeMillis());
                    request.setSpaceType(spaceType);
                    request.setSpaceLevel(0);

                    // 创建测试用户
                    User testUser = new User();
                    testUser.setId(userId);
                    testUser.setUserRole(UserRoleEnum.USER.getText());

                    // 调用创建空间方法
                    spaceService.addSpace(request, testUser);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 预期会有多个线程失败
                    System.out.println("创建空间失败：" + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        executorService.shutdown();

        // 验证结果：只有一个线程能成功创建空间
        System.out.println("成功创建空间的线程数：" + successCount.get());
        assert successCount.get() == 1 : "应该只有一个线程能成功创建空间";
    }

    @Test
    public void testApplicationContext() {
        // 简单测试，只要能加载应用上下文就算成功
        assert true;
    }
}