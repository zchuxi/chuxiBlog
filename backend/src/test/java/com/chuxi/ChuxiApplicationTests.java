package com.chuxi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 冒烟测试：完整加载 Spring 上下文（JPA 实体建表 + 全部 Bean 装配 + 种子数据导入）。
 * 使用 src/test/resources/application.yml 的 H2 内存库，mvn test 无需 MySQL/OSS。
 */
@SpringBootTest
class ChuxiApplicationTests {

    @Test
    void contextLoads() {
        // 上下文加载失败（Bean 装配错误、实体映射错误等）会直接使本测试失败
    }
}
