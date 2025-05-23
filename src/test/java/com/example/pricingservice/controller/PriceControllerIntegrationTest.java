package com.example.pricingservice.controller;

import com.example.pricingservice.PricingServiceApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PricingServiceApplication.class)
@AutoConfigureMockMvc
public class PriceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return price data for existing article and store")
    void shouldReturnPriceDataForExistingArticleAndStore() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/prices/7001/1000102674")
                .param("page", "1")
                .param("pageSize", "3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.article", is("1000102674")))
                .andExpect(jsonPath("$.store", is("7001")))
                .andExpect(jsonPath("$.meta.page", is(1)))
                .andExpect(jsonPath("$.meta.size", is(3)))
                .andExpect(jsonPath("$.properties.uom", is("EA")))
                .andExpect(jsonPath("$.properties.description", is("WH Halifax Passage Lever in Satin Nickel")))
                .andExpect(jsonPath("$.properties.brand", is("Weiser")))
                .andExpect(jsonPath("$.properties.model", is("9GLA1010")))
                .andExpect(jsonPath("$.prices", hasSize(greaterThanOrEqualTo(1))))
                // Check for overlapped prices
                .andExpect(jsonPath("$.prices[?(@.type=='retail' && @.subtype=='discounted' && @.amount==27.0)].overlapped", contains(true)))
                .andExpect(jsonPath("$.prices[?(@.type=='retail' && @.subtype=='discounted' && @.amount==26.5)].overlapped", contains(true)));
    }

    @Test
    @DisplayName("Should return 404 for non-existing article")
    void shouldReturn404ForNonExistingArticle() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/prices/7001/9999999")
                .param("page", "1")
                .param("pageSize", "3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("Not_Found")))
                .andExpect(jsonPath("$.title", is("Unavailable prices")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", containsString("No prices were found")));
    }

    @Test
    @DisplayName("Should return 404 for article with no prices")
    void shouldReturn404ForArticleWithNoPrices() throws Exception {
        // Article with ID 9999999999 was created in DataLoader without any prices
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/prices/9999/9999999999")
                .param("page", "1")
                .param("pageSize", "3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type", is("Not_Found")))
                .andExpect(jsonPath("$.title", is("Unavailable prices")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("Should successfully clear cache")
    void shouldSuccessfullyClearCache() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/prices/admin/clear-cache")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Cache cleared successfully"));
    }
}