package com.spacecargo.stowage.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorrelationIdFilterTest {

    @Autowired MockMvc mvc;

    @Test
    void generatesACorrelationIdWhenNoneIsProvided() throws Exception {
        mvc.perform(get("/api/v1/containers"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER,
                        matchesPattern("[0-9a-fA-F-]{36}")));
    }

    @Test
    void echoesAnInboundCorrelationId() throws Exception {
        mvc.perform(get("/api/v1/containers").header(CorrelationIdFilter.HEADER, "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, "trace-123"));
    }
}
