package com.lottery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 时钟配置
 *
 * <p>提供可注入的 {@link Clock}，统一以业务时区（Asia/Shanghai）计算日期，
 * 避免直接依赖 JVM 默认时区，同时便于测试对时间进行控制。</p>
 */
@Configuration
public class ClockConfig {

    /**
     * 业务时区
     */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
