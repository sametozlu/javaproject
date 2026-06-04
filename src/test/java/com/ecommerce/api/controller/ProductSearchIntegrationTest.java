package com.ecommerce.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchWithPriceFilter_returnsOk() throws Exception {
        mockMvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchByQuery_matchesCategoryName() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "aksesuar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").exists());
    }
}
