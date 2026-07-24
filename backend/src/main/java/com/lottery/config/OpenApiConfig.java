package com.lottery.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lotteryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🎰 抽奖系统 API")
                        .description("商业级抽奖系统后端API文档\n\n" +
                                "## 功能特性\n" +
                                "- 🎯 多种抽奖模式支持\n" +
                                "- 📊 奖品管理与库存控制\n" +
                                "- 👥 用户管理与权限控制\n" +
                                "- 📝 完整的抽奖记录追踪")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Lottery System")
                                .email("support@lottery.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
