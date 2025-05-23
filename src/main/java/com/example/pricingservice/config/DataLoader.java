package com.example.pricingservice.config;

import com.example.pricingservice.model.Article;
import com.example.pricingservice.model.Price;
import com.example.pricingservice.repository.ArticleRepository;
import com.example.pricingservice.repository.PriceRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Component to load initial data into the database on application startup
 */
@Component
@Slf4j
public class DataLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);
    
    private final ArticleRepository articleRepository;
    private final PriceRepository priceRepository;

    public DataLoader(ArticleRepository articleRepository, PriceRepository priceRepository) {
        this.articleRepository = articleRepository;
        this.priceRepository = priceRepository;
    }

    /**
     * Populates the database with sample data on application startup
     */
    @PostConstruct
    @Transactional
    public void loadData() {
        logger.info("Loading initial data into the database");
        
        // Create sample articles
        createSampleArticles();
        createOverlapTestArticle();
        
        logger.info("Initial data loading completed");
    }
    
    /**
     * Create sample articles with their prices from the original example
     */
    private void createSampleArticles() {
        // Sample article 1 - Example from the requirements
        Article article1 = new Article();
        article1.setArticleId("1000102674");
        article1.setStoreId("7001");
        article1.setUom("EA");
        article1.setDescription("WH Halifax Passage Lever in Satin Nickel");
        article1.setBrand("Weiser");
        article1.setModel("9GLA1010");
        
        articleRepository.save(article1);
        
        // Create prices for article 1
        List<Price> prices1 = new ArrayList<>();
        
        // Regular price
        Price regularPrice = new Price();
        regularPrice.setArticle(article1);
        regularPrice.setType("retail");
        regularPrice.setSubtype("regular");
        regularPrice.setCurrency("CAD");
        regularPrice.setAmount(new BigDecimal("30.0"));
        regularPrice.setValidFrom(ZonedDateTime.parse("2023-12-31T23:59:59Z"));
        regularPrice.setValidTo(ZonedDateTime.parse("9999-12-31T23:59:59Z"));
        prices1.add(regularPrice);
        
        // First discounted price - this will overlap with second discounted price
        Price discountedPrice1 = new Price();
        discountedPrice1.setArticle(article1);
        discountedPrice1.setType("retail");
        discountedPrice1.setSubtype("discounted");
        discountedPrice1.setCurrency("CAD");
        discountedPrice1.setAmount(new BigDecimal("27.0"));
        discountedPrice1.setValidFrom(ZonedDateTime.parse("2023-12-21T23:59:59Z"));
        discountedPrice1.setValidTo(ZonedDateTime.parse("2025-12-31T23:59:58Z"));
        prices1.add(discountedPrice1);
        
        // Second discounted price - overlapping with first but different amount
        Price discountedPrice2 = new Price();
        discountedPrice2.setArticle(article1);
        discountedPrice2.setType("retail");
        discountedPrice2.setSubtype("discounted");
        discountedPrice2.setCurrency("CAD");
        discountedPrice2.setAmount(new BigDecimal("26.5"));
        discountedPrice2.setValidFrom(ZonedDateTime.parse("2023-12-21T23:59:59Z"));
        discountedPrice2.setValidTo(ZonedDateTime.parse("2025-12-25T23:59:58Z"));
        prices1.add(discountedPrice2);
        
        priceRepository.saveAll(prices1);
        
        // Sample article 2 - for testing "no overlap" scenario
        Article article2 = new Article();
        article2.setArticleId("1000203345");
        article2.setStoreId("7001");
        article2.setUom("PC");
        article2.setDescription("Bathroom Faucet Chrome Finish");
        article2.setBrand("Delta");
        article2.setModel("DF2233");
        
        articleRepository.save(article2);
        
        // Create prices for article 2
        List<Price> prices2 = new ArrayList<>();
        
        // Regular price - will not overlap with discounted
        Price regularPrice2 = new Price();
        regularPrice2.setArticle(article2);
        regularPrice2.setType("retail");
        regularPrice2.setSubtype("regular");
        regularPrice2.setCurrency("CAD");
        regularPrice2.setAmount(new BigDecimal("89.99"));
        regularPrice2.setValidFrom(ZonedDateTime.parse("2023-01-01T00:00:00Z"));
        regularPrice2.setValidTo(ZonedDateTime.parse("2023-10-31T23:59:59Z"));
        prices2.add(regularPrice2);
        
        // Discounted price - will not overlap with regular
        Price discountedPrice3 = new Price();
        discountedPrice3.setArticle(article2);
        discountedPrice3.setType("retail");
        discountedPrice3.setSubtype("discounted");
        discountedPrice3.setCurrency("CAD");
        discountedPrice3.setAmount(new BigDecimal("75.50"));
        discountedPrice3.setValidFrom(ZonedDateTime.parse("2023-11-20T00:00:00Z"));
        discountedPrice3.setValidTo(ZonedDateTime.parse("2024-01-10T23:59:59Z"));
        prices2.add(discountedPrice3);
        
        priceRepository.saveAll(prices2);
        
        // Sample article 3 - No prices to test error case
        Article article3 = new Article();
        article3.setArticleId("9999999999");
        article3.setStoreId("9999");
        article3.setUom("EA");
        article3.setDescription("Test article with no prices");
        article3.setBrand("Test Brand");
        article3.setModel("TEST123");
        
        articleRepository.save(article3);
        
        logger.info("Created 3 sample articles with their prices");
    }
    
    /**
     * Create an article with prices for testing various overlap scenarios
     */
    private void createOverlapTestArticle() {
        // Create a test article
        Article article = new Article();
        article.setArticleId("2000000001");
        article.setStoreId("8001");
        article.setUom("EA");
        article.setDescription("Overlap Test Article");
        article.setBrand("Test Brand");
        article.setModel("OV100");
        
        articleRepository.save(article);
        
        List<Price> prices = new ArrayList<>();
        
        // Case 1: Non-overlapping prices (different time periods)
        Price noOverlap1 = new Price();
        noOverlap1.setArticle(article);
        noOverlap1.setType("retail");
        noOverlap1.setSubtype("non-overlapping-1");
        noOverlap1.setCurrency("CAD");
        noOverlap1.setAmount(new BigDecimal("50.0"));
        noOverlap1.setValidFrom(ZonedDateTime.parse("2023-01-01T00:00:00Z"));
        noOverlap1.setValidTo(ZonedDateTime.parse("2023-06-30T23:59:59Z"));
        prices.add(noOverlap1);
        
        Price noOverlap2 = new Price();
        noOverlap2.setArticle(article);
        noOverlap2.setType("retail");
        noOverlap2.setSubtype("non-overlapping-2");
        noOverlap2.setCurrency("CAD");
        noOverlap2.setAmount(new BigDecimal("55.0"));
        noOverlap2.setValidFrom(ZonedDateTime.parse("2023-07-01T00:00:00Z"));
        noOverlap2.setValidTo(ZonedDateTime.parse("2023-12-31T23:59:59Z"));
        prices.add(noOverlap2);
        
        // Case 2: Overlapping prices with SAME amount (should be merged)
        Price sameAmount1 = new Price();
        sameAmount1.setArticle(article);
        sameAmount1.setType("retail");
        sameAmount1.setSubtype("same-amount");
        sameAmount1.setCurrency("CAD");
        sameAmount1.setAmount(new BigDecimal("60.0"));
        sameAmount1.setValidFrom(ZonedDateTime.parse("2024-01-01T00:00:00Z"));
        sameAmount1.setValidTo(ZonedDateTime.parse("2024-06-30T23:59:59Z"));
        prices.add(sameAmount1);
        
        Price sameAmount2 = new Price();
        sameAmount2.setArticle(article);
        sameAmount2.setType("retail");
        sameAmount2.setSubtype("same-amount");
        sameAmount2.setCurrency("CAD");
        sameAmount2.setAmount(new BigDecimal("60.0"));
        sameAmount2.setValidFrom(ZonedDateTime.parse("2024-06-01T00:00:00Z"));
        sameAmount2.setValidTo(ZonedDateTime.parse("2024-08-31T23:59:59Z"));
        prices.add(sameAmount2);
        
        // Case 3: Overlapping prices with DIFFERENT amounts (should be marked as overlapped)
        Price diffAmount1 = new Price();
        diffAmount1.setArticle(article);
        diffAmount1.setType("retail");
        diffAmount1.setSubtype("different-amount");
        diffAmount1.setCurrency("CAD");
        diffAmount1.setAmount(new BigDecimal("70.0"));
        diffAmount1.setValidFrom(ZonedDateTime.parse("2024-10-01T00:00:00Z"));
        diffAmount1.setValidTo(ZonedDateTime.parse("2025-03-31T23:59:59Z"));
        prices.add(diffAmount1);
        
        Price diffAmount2 = new Price();
        diffAmount2.setArticle(article);
        diffAmount2.setType("retail");
        diffAmount2.setSubtype("different-amount");
        diffAmount2.setCurrency("CAD");
        diffAmount2.setAmount(new BigDecimal("65.0"));
        diffAmount2.setValidFrom(ZonedDateTime.parse("2025-01-01T00:00:00Z"));
        diffAmount2.setValidTo(ZonedDateTime.parse("2025-06-30T23:59:59Z"));
        prices.add(diffAmount2);
        
        // Create and save the prices
        priceRepository.saveAll(prices);
        
        logger.info("Created test article with 6 prices demonstrating different overlap scenarios");
    }
}