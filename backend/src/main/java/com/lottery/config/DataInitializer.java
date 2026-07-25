package com.lottery.config;

import com.lottery.entity.Prize;
import com.lottery.entity.User;
import com.lottery.repository.PrizeRepository;
import com.lottery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器 - 初始化默认数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PrizeRepository prizeRepository;

    @Override
    public void run(String... args) {
        initUsers();
        initPrizes();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            // 创建管理员
            User admin = User.builder()
                    .username("admin")
                    .password("admin123")
                    .nickname("系统管理员")
                    .phone("13800000000")
                    .remainingChances(999)
                    .role("ADMIN")
                    .enabled(true)
                    .build();
            userRepository.save(admin);

            // 创建测试用户
            User user = User.builder()
                    .username("user")
                    .password("user123")
                    .nickname("测试用户")
                    .phone("13800000001")
                    .remainingChances(10)
                    .role("USER")
                    .enabled(true)
                    .build();
            userRepository.save(user);

            log.info("初始化用户数据完成: admin, user");
        }
    }

    private void initPrizes() {
        if (prizeRepository.count() == 0) {
            // 特等奖
            Prize prize1 = Prize.builder()
                    .name("iPhone 15 Pro Max")
                    .description("苹果最新旗舰手机，256GB存储")
                    .imageUrl("/images/iphone.png")
                    .probability(5)
                    .stock(1)
                    .prizeLevel(1)
                    .color("#FFD700")
                    .enabled(true)
                    .sortOrder(1)
                    .build();
            prizeRepository.save(prize1);

            // 一等奖
            Prize prize2 = Prize.builder()
                    .name("AirPods Pro 2")
                    .description("苹果无线降噪耳机")
                    .imageUrl("/images/airpods.png")
                    .probability(20)
                    .stock(5)
                    .prizeLevel(2)
                    .color("#C0C0C0")
                    .enabled(true)
                    .sortOrder(2)
                    .build();
            prizeRepository.save(prize2);

            // 二等奖
            Prize prize3 = Prize.builder()
                    .name("小米手环 8")
                    .description("运动健康智能手环")
                    .imageUrl("/images/miband.png")
                    .probability(50)
                    .stock(20)
                    .prizeLevel(3)
                    .color("#CD7F32")
                    .enabled(true)
                    .sortOrder(3)
                    .build();
            prizeRepository.save(prize3);

            // 三等奖
            Prize prize4 = Prize.builder()
                    .name("星巴克礼品卡")
                    .description("价值100元咖啡礼品卡")
                    .imageUrl("/images/starbucks.png")
                    .probability(100)
                    .stock(50)
                    .prizeLevel(4)
                    .color("#00704A")
                    .enabled(true)
                    .sortOrder(4)
                    .build();
            prizeRepository.save(prize4);

            // 参与奖
            Prize prize5 = Prize.builder()
                    .name("10积分")
                    .description("可兑换商城商品")
                    .imageUrl("/images/points.png")
                    .probability(300)
                    .stock(1000)
                    .prizeLevel(5)
                    .color("#87CEEB")
                    .enabled(true)
                    .sortOrder(5)
                    .build();
            prizeRepository.save(prize5);

            // 参与奖2
            Prize prize6 = Prize.builder()
                    .name("5积分")
                    .description("可兑换商城商品")
                    .imageUrl("/images/points.png")
                    .probability(300)
                    .stock(2000)
                    .prizeLevel(5)
                    .color("#98FB98")
                    .enabled(true)
                    .sortOrder(6)
                    .build();
            prizeRepository.save(prize6);

            // 谢谢参与
            Prize prize7 = Prize.builder()
                    .name("谢谢参与")
                    .description("感谢您的参与，下次再来")
                    .imageUrl("/images/thanks.png")
                    .probability(225)
                    .stock(9999)
                    .prizeLevel(5)
                    .color("#DDA0DD")
                    .enabled(true)
                    .sortOrder(7)
                    .build();
            prizeRepository.save(prize7);

            // 再来一次（额外）
            Prize prize8 = Prize.builder()
                    .name("再接再厉")
                    .description("不要灰心，继续努力")
                    .imageUrl("/images/tryagain.png")
                    .probability(0)
                    .stock(9999)
                    .prizeLevel(5)
                    .color("#FFA07A")
                    .enabled(true)
                    .sortOrder(8)
                    .build();
            prizeRepository.save(prize8);

            log.info("初始化奖品数据完成: 8个奖品");
        }
    }
}
