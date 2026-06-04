package com.ecommerce.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartOrderFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void registerUser() throws Exception {
        String email = "cartuser" + System.nanoTime() + "@test.com";
        String body = """
                {"email":"%s","password":"password123","fullName":"Cart User"}
                """.formatted(email);

        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        token = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void addressCartCheckoutFlow() throws Exception {
        String addressBody = """
                {"title":"Ev","fullName":"Cart User","phone":"05551234567","city":"İstanbul","district":"Kadıköy","addressLine":"Test Sok. No:1","postalCode":"34710","defaultAddress":true}
                """;

        MvcResult addrRes = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressBody))
                .andExpect(status().isCreated())
                .andReturn();

        long addressId = objectMapper.readTree(addrRes.getResponse().getContentAsString()).get("id").asLong();

        MvcResult products = mockMvc.perform(get("/api/products?size=1"))
                .andExpect(status().isOk())
                .andReturn();

        long productId = objectMapper.readTree(products.getResponse().getContentAsString())
                .get("content").get(0).get("id").asLong();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());

        String checkout = "{\"couponCode\":null,\"addressId\":" + addressId + "}";

        mockMvc.perform(post("/api/cart/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.shippingAddress.city").value("İstanbul"));
    }
}
