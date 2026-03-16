package com.ledger.config;

import com.ledger.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class GlobalExceptionHandlerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void nonexistent_endpoint_does_not_return_stacktrace() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent-endpoint-xyz"))
                .andExpect(status().isNotFound());
    }
}
