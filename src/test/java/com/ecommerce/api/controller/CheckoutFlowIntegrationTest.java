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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void featuredAndBestsellers_arePublic() throws Exception {
        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        mockMvc.perform(get("/api/products/bestsellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void couponValidate_worksForWelcome10() throws Exception {
        mockMvc.perform(post("/api/coupons/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WELCOME10\",\"orderAmount\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.discountPercent").value(10));
    }

    @Test
    void checkoutFlow_createsOrder() throws Exception {
        String token = registerAndLogin("checkout@test.com");

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ev","fullName":"Test User","phone":"05551112233",
                                "city":"Istanbul","district":"Kadikoy","addressLine":"Demo Sok. 1",
                                "postalCode":"34710","defaultAddress":true}
                                """))
                .andExpect(status().isCreated());

        MvcResult products = mockMvc.perform(get("/api/products?size=1"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(products.getResponse().getContentAsString());
        long productId = list.get("content").get(0).get("id").asLong();

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isOk());

        MvcResult addresses = mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long addressId = objectMapper.readTree(addresses.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(post("/api/cart/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":" + addressId + ",\"couponCode\":\"WELCOME10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"fullName\":\"Checkout User\"}"))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }
}
