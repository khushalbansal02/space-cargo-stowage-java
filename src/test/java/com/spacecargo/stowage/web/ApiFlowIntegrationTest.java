package com.spacecargo.stowage.web;

import com.spacecargo.stowage.repository.ContainerRepository;
import com.spacecargo.stowage.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the primary REST flow end-to-end over HTTP:
 * create container → register item → place → search → retrieve.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ItemRepository items;
    @Autowired ContainerRepository containers;

    @BeforeEach
    void clean() {
        items.deleteAll();
        containers.deleteAll();
    }

    @Test
    void placeSearchAndRetrieveAnItemOverHttp() throws Exception {
        mvc.perform(post("/api/v1/containers").contentType(MediaType.APPLICATION_JSON).content("""
                {"containerId":"C1","zone":"Lab","width":10,"depth":10,"height":10}"""))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content("""
                {"itemId":"I1","name":"Sample","width":2,"depth":2,"height":2,"priority":90}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        mvc.perform(post("/api/v1/placements").contentType(MediaType.APPLICATION_JSON).content("""
                {"itemIds":["I1"],"userId":"tester"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placed").value(1))
                .andExpect(jsonPath("$.results[0].containerId").value("C1"));

        mvc.perform(get("/api/v1/search").param("itemId", "I1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.retrievalSteps[0].action").value("RETRIEVE"));

        mvc.perform(post("/api/v1/items/I1/retrieve").contentType(MediaType.APPLICATION_JSON).content("""
                {"userId":"crew"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETRIEVED"));
    }

    @Test
    void validationFailureReturnsProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content("""
                {"itemId":"","name":"Bad","width":-1,"depth":2,"height":2,"priority":200}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void unknownItemReturns404() throws Exception {
        mvc.perform(get("/api/v1/items/DOES_NOT_EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
