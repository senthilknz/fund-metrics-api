package com.fundmetrics.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflight_allowedLocalOrigin_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/v1/funds")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void preflight_allowedProdOrigin_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/v1/funds")
                        .header("Origin", "https://your-microfrontend.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin",
                        "https://your-microfrontend.example.com"));
    }

    @Test
    void preflight_disallowedOrigin_doesNotReturnAllowOriginHeader() throws Exception {
        mockMvc.perform(options("/api/v1/funds")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void preflight_getMethod_isAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/funds")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void preflight_postMethod_isAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/funds/activate")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void preflight_chooserEndpoint_isAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/funds/chooser")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
