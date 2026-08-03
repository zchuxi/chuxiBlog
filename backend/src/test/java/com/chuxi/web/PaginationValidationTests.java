package com.chuxi.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaginationValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/front/home/articles?pageNo=0&pageSize=10",
            "/api/front/home/articles?pageNo=1&pageSize=51",
            "/api/front/tree-hole/barrages?pageNo=0&pageSize=50",
            "/api/front/tree-hole/barrages?pageNo=1&pageSize=101",
            "/api/front/tree-hole/called-texts?pageNo=1&pageSize=101",
            "/api/music?pageNo=1&pageSize=101"
    })
    void rejectsInvalidPagination(String uri) throws Exception {
        mockMvc.perform(get(uri))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分页参数无效"));
    }
}
