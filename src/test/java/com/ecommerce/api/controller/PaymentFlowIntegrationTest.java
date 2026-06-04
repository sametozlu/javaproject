package com.ecommerce.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void payment_success_whenSimulateFailureFalse() throws Exception {
        String token = registerAndLogin("pay-success@test.com");
        long addressId = createAddress(token);
        long productId = firstProductId();
        addToCart(token, productId);
        long orderId = checkout(token, addressId);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"pay-test-ok-%d","simulateFailure":false}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void payment_fails_whenSimulateFailureTrue() throws Exception {
        String token = registerAndLogin("pay-fail@test.com");
        long addressId = createAddress(token);
        long productId = firstProductId();
        addToCart(token, productId);
        long orderId = checkout(token, addressId);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"pay-test-fail-%d","simulateFailure":true}
                                """.formatted(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Pay User\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private long createAddress(String token) throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ev","fullName":"Pay User","phone":"05551112233",
                                "city":"Istanbul","district":"Kadikoy","addressLine":"Demo 1",
                                "postalCode":"34710","defaultAddress":true}
                                """))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    private long firstProductId() throws Exception {
        MvcResult products = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/products?size=1"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(products.getResponse().getContentAsString()).get("content").get(0).get("id").asLong();
    }

    private void addToCart(String token, long productId) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());
    }

    private long checkout(String token, long addressId) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/cart/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode order = objectMapper.readTree(res.getResponse().getContentAsString());
        return order.get("id").asLong();
    }
}
