package com.chuxi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 冒烟测试：完整加载 Spring 上下文（JPA 实体建表 + 全部 Bean 装配 + 种子数据导入）。
 * 使用 src/test/resources/application.yml 的 H2 内存库，mvn test 无需 MySQL/OSS。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChuxiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // 上下文加载失败（Bean 装配错误、实体映射错误等）会直接使本测试失败
    }

    @Test
    void healthEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
