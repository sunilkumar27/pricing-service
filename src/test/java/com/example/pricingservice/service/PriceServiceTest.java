package com.example.pricingservice.service;

import com.example.pricingservice.dto.PriceDTO;
import com.example.pricingservice.dto.PriceResponseDTO;
import com.example.pricingservice.exception.PriceNotFoundException;
import com.example.pricingservice.model.Article;
import com.example.pricingservice.model.Price;
import com.example.pricingservice.repository.ArticleRepository;
import com.example.pricingservice.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriceServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private PriceService priceService;

    private Article testArticle;
    private List<Price> testPrices;

    @BeforeEach
    void setUp() {
        // Set up test article
    	testArticle = Article.builder()
                .id(1L)
                .articleId("1000102674")
                .storeId("7001")
                .uom("EA")
                .description("Test Article")
                .brand("Test Brand")
                .model("Test Model")
                .build();

        // Set up test prices
        testPrices = new ArrayList<>();
    }

    @Test
    @DisplayName("Should mark prices as overlapped when they have different amounts and overlapping validity ranges")
    void shouldMarkOverlappingPricesWithDifferentAmounts() {
        // Create prices with overlapping ranges but different amounts
    	Price price1 = Price.builder()
                .id(1L)
                .article(testArticle)
                .type("retail")
                .subtype("discounted")
                .currency("CAD")
                .amount(new BigDecimal("27.0"))
                .validFrom(ZonedDateTime.parse("2023-12-21T23:59:59Z"))
                .validTo(ZonedDateTime.parse("2025-12-31T23:59:58Z"))
                .build();

        Price price2 = Price.builder()
                .id(2L)
                .article(testArticle)
                .type("retail")
                .subtype("discounted")
                .currency("CAD")
                .amount(new BigDecimal("26.5"))
                .validFrom(ZonedDateTime.parse("2023-12-21T23:59:59Z"))
                .validTo(ZonedDateTime.parse("2025-12-25T23:59:58Z"))
                .build();

        testPrices.add(price1);
        testPrices.add(price2);

        // Set up mock behavior
        when(articleRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674")))
                .thenReturn(Optional.of(testArticle));
        when(priceRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testPrices));

        // Call the service method
        PriceResponseDTO response = priceService.getPrices("7001", "1000102674", 1, 10);

        // Verify results
        assertNotNull(response);
        assertEquals(2, response.getPrices().size());
        
        // Both prices should be marked as overlapped
        boolean price1Overlapped = false;
        boolean price2Overlapped = false;
        
        for (PriceDTO priceDTO : response.getPrices()) {
            if (priceDTO.getAmount().compareTo(new BigDecimal("27.0")) == 0) {
                price1Overlapped = priceDTO.isOverlapped();
            }
            if (priceDTO.getAmount().compareTo(new BigDecimal("26.5")) == 0) {
                price2Overlapped = priceDTO.isOverlapped();
            }
        }
        
        assertTrue(price1Overlapped);
        assertTrue(price2Overlapped);
    }

    @Test
    @DisplayName("Should not mark prices as overlapped when they don't have overlapping validity ranges")
    void shouldNotMarkNonOverlappingPrices() {
        // Create prices with non-overlapping ranges
    	 Price price1 = Price.builder()
                 .id(1L)
                 .article(testArticle)
                 .type("retail")
                 .subtype("discounted")
                 .currency("CAD")
                 .amount(new BigDecimal("27.0"))
                 .validFrom(ZonedDateTime.parse("2023-01-01T00:00:00Z"))
                 .validTo(ZonedDateTime.parse("2023-06-30T23:59:59Z"))
                 .build();

         Price price2 = Price.builder()
                 .id(2L)
                 .article(testArticle)
                 .type("retail")
                 .subtype("discounted")
                 .currency("CAD")
                 .amount(new BigDecimal("26.5"))
                 .validFrom(ZonedDateTime.parse("2023-07-01T00:00:00Z"))
                 .validTo(ZonedDateTime.parse("2023-12-31T23:59:59Z"))
                 .build();

        testPrices.add(price1);
        testPrices.add(price2);

        // Set up mock behavior
        when(articleRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674")))
                .thenReturn(Optional.of(testArticle));
        when(priceRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testPrices));

        // Call the service method
        PriceResponseDTO response = priceService.getPrices("7001", "1000102674", 1, 10);

        // Verify results
        assertNotNull(response);
        assertEquals(2, response.getPrices().size());
        
        // Neither price should be marked as overlapped
        for (PriceDTO priceDTO : response.getPrices()) {
            assertFalse(priceDTO.isOverlapped());
        }
    }

    @Test
    @DisplayName("Should merge prices with same amounts and overlapping validity ranges")
    void shouldMergePricesWithSameAmountsAndOverlappingRanges() {
        // Create prices with overlapping ranges and same amounts that should be merged
    	Price price1 = Price.builder()
                .id(1L)
                .article(testArticle)
                .type("retail")
                .subtype("special")
                .currency("CAD")
                .amount(new BigDecimal("25.0"))
                .validFrom(ZonedDateTime.parse("2024-01-01T00:00:00Z"))
                .validTo(ZonedDateTime.parse("2024-01-15T23:59:59Z"))
                .build();

        Price price2 = Price.builder()
                .id(2L)
                .article(testArticle)
                .type("retail")
                .subtype("special")
                .currency("CAD")
                .amount(new BigDecimal("25.0"))
                .validFrom(ZonedDateTime.parse("2024-01-15T00:00:00Z"))
                .validTo(ZonedDateTime.parse("2024-01-31T23:59:59Z"))
                .build();

        testPrices.add(price1);
        testPrices.add(price2);

        // Set up mock behavior
        when(articleRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674")))
                .thenReturn(Optional.of(testArticle));
        when(priceRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(testPrices));

        // Call the service method
        PriceResponseDTO response = priceService.getPrices("7001", "1000102674", 1, 10);

        // Verify results
        assertNotNull(response);
        
        // Should have merged the two prices into one
        assertEquals(1, response.getPrices().size());
        
        PriceDTO mergedPrice = response.getPrices().get(0);
        assertEquals("retail", mergedPrice.getType());
        assertEquals("special", mergedPrice.getSubtype());
        assertEquals("CAD", mergedPrice.getCurrency());
        assertEquals(new BigDecimal("25.0"), mergedPrice.getAmount());
        
        // Merged price should have the widest validity range
        assertEquals(ZonedDateTime.parse("2024-01-01T00:00:00Z"), mergedPrice.getValidFrom());
        assertEquals(ZonedDateTime.parse("2024-01-31T23:59:59Z"), mergedPrice.getValidTo());
        
        // Merged price should not be marked as overlapped
        assertFalse(mergedPrice.isOverlapped());
    }

    @Test
    @DisplayName("Should throw PriceNotFoundException when article not found")
    void shouldThrowExceptionWhenArticleNotFound() {
        // Set up mock behavior for article not found
        when(articleRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674")))
                .thenReturn(Optional.empty());

        // Call the service method and verify exception
        assertThrows(PriceNotFoundException.class, () ->
                priceService.getPrices("7001", "1000102674", 1, 10));
    }

    @Test
    @DisplayName("Should throw PriceNotFoundException when no prices found")
    void shouldThrowExceptionWhenNoPricesFound() {
        // Set up mock behavior for article found but no prices
        when(articleRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674")))
                .thenReturn(Optional.of(testArticle));
        when(priceRepository.findByStoreIdAndArticleId(eq("7001"), eq("1000102674"), any(PageRequest.class)))
                .thenReturn(Page.empty());

        // Call the service method and verify exception
        assertThrows(PriceNotFoundException.class, () ->
                priceService.getPrices("7001", "1000102674", 1, 10));
    }
}