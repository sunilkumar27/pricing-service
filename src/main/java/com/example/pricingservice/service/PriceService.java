package com.example.pricingservice.service;

import com.example.pricingservice.dto.MetaDTO;
import com.example.pricingservice.dto.PriceDTO;
import com.example.pricingservice.dto.PriceResponseDTO;
import com.example.pricingservice.dto.PropertiesDTO;
import com.example.pricingservice.exception.PriceNotFoundException;
import com.example.pricingservice.model.Article;
import com.example.pricingservice.model.Price;
import com.example.pricingservice.repository.ArticleRepository;
import com.example.pricingservice.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for handling price-related operations
 */
@Service
@Slf4j
public class PriceService {
    
    private final ArticleRepository articleRepository;
    private final PriceRepository priceRepository;
    
    // In-memory cache implementation using ConcurrentHashMap
    private final Map<String, PriceResponseDTO> priceCache = new ConcurrentHashMap<>();

    public PriceService(ArticleRepository articleRepository, PriceRepository priceRepository) {
        this.articleRepository = articleRepository;
        this.priceRepository = priceRepository;
    }

    /**
     * Get prices for a specific store and article with pagination
     * 
     * @param storeId the store ID
     * @param articleId the article ID
     * @param page the page number (starting from 1)
     * @param pageSize the page size
     * @return the price response DTO
     * @throws PriceNotFoundException if prices not found
     */
    public PriceResponseDTO getPrices(String storeId, String articleId, int page, int pageSize) {
        log.debug("Retrieving prices for store: {}, article: {}, page: {}, pageSize: {}", 
                storeId, articleId, page, pageSize);
        
        // For development/testing - disable caching
        clearCache();
        
        // Fetch from database
        Article article = articleRepository.findByStoreIdAndArticleId(storeId, articleId)
                .orElseThrow(() -> new PriceNotFoundException("No prices were found for a given request"));
        
        // Spring Data JPA uses 0-based page indexing
        int pageIndex = Math.max(0, page - 1);
        
        Page<Price> pricePage = priceRepository.findByStoreIdAndArticleId(
                storeId, articleId, PageRequest.of(pageIndex, pageSize));
        
        if (pricePage.isEmpty()) {
            throw new PriceNotFoundException("No prices were found for a given request");
        }
        
        List<Price> prices = new ArrayList<>(pricePage.getContent());
        
        // Step 1: Copy prices to DTOs and process them
        List<PriceDTO> priceDTOs = processPrices(prices);
        
        // Step 2: Build and return the response
        PriceResponseDTO response = buildPriceResponse(article, priceDTOs, page, pageSize);
        
        // Cache for future requests
        String cacheKey = generateCacheKey(storeId, articleId, page, pageSize);
        priceCache.put(cacheKey, response);
        
        return response;
    }
    
    /**
     * Process prices: mark overlapping with different amounts as overlapped,
     * and merge those with same amounts
     */
    private List<PriceDTO> processPrices(List<Price> prices) {
        // Log original prices
        log.debug("Processing {} prices", prices.size());
        for (Price price : prices) {
            log.debug("  Original Price[ID={}, Type={}, Subtype={}, Amount={}, ValidFrom={}, ValidTo={}]",
                price.getId(), price.getType(), price.getSubtype(), 
                price.getAmount(), price.getValidFrom(), price.getValidTo());
        }
        
        // Step 1: Create DTOs from prices, with overlapped flag set to false initially
        List<PriceDTO> dtos = prices.stream()
            .map(price -> PriceDTO.builder()
                .type(price.getType())
                .subtype(price.getSubtype())
                .currency(price.getCurrency())
                .amount(price.getAmount())
                .validFrom(price.getValidFrom())
                .validTo(price.getValidTo())
                .overlapped(false) // Start with overlapped set to false
                .build())
            .collect(Collectors.toList());
        
        // Step 2: Mark overlapping prices with different amounts as "overlapped"
        markOverlappedPrices(dtos);
        
        // Step 3: Merge prices with overlapping date ranges and equal amounts
        List<PriceDTO> mergedPrices = mergePricesWithEqualAmounts(dtos);
        
        // Log final result
        log.debug("Final processed prices (count: {})", mergedPrices.size());
        for (PriceDTO dto : mergedPrices) {
            log.debug("  Processed DTO[Type={}, Subtype={}, Amount={}, ValidFrom={}, ValidTo={}, Overlapped={}]",
                dto.getType(), dto.getSubtype(), dto.getAmount(), 
                dto.getValidFrom(), dto.getValidTo(), dto.isOverlapped());
        }
        
        return mergedPrices;
    }
    
    /**
     * Mark DTOs as overlapped if they have overlapping date ranges and different amounts
     */
    private void markOverlappedPrices(List<PriceDTO> prices) {
        log.debug("Starting overlap detection for {} prices", prices.size());
        
        for (int i = 0; i < prices.size(); i++) {
            PriceDTO price1 = prices.get(i);
            
            for (int j = i + 1; j < prices.size(); j++) {
                PriceDTO price2 = prices.get(j);
                
                // Check if date ranges overlap
                boolean overlaps = doRangesOverlap(
                    price1.getValidFrom(), price1.getValidTo(),
                    price2.getValidFrom(), price2.getValidTo()
                );
                
                if (overlaps) {
                    log.debug("Date ranges overlap: {} to {} and {} to {}",
                        price1.getValidFrom(), price1.getValidTo(),
                        price2.getValidFrom(), price2.getValidTo());
                    
                    // Check if amounts are different
                    boolean differentAmounts = !price1.getAmount().equals(price2.getAmount());
                    
                    if (differentAmounts) {
                        // MARK OVERLAPPED - this is the key requirement
                        price1.setOverlapped(true);
                        price2.setOverlapped(true);
                        
                        log.debug("Marked as overlapped: {} ({}) and {} ({}), different amounts: {} vs {}",
                            price1.getSubtype(), formatDateRange(price1),
                            price2.getSubtype(), formatDateRange(price2),
                            price1.getAmount(), price2.getAmount());
                    } else {
                        log.debug("Not marked as overlapped (will be merged): {} and {}, same amount: {}",
                            price1.getSubtype(), price2.getSubtype(), price1.getAmount());
                    }
                } else {
                    log.debug("No date overlap: {} ({}) and {} ({})",
                        price1.getSubtype(), formatDateRange(price1),
                        price2.getSubtype(), formatDateRange(price2));
                }
            }
        }
    }
    
    /**
     * Format date range for logging
     */
    private String formatDateRange(PriceDTO price) {
        return price.getValidFrom() + " to " + price.getValidTo();
    }
    
    /**
     * Merge prices with overlapping validity ranges and equal amounts
     */
    private List<PriceDTO> mergePricesWithEqualAmounts(List<PriceDTO> prices) {
        log.debug("Starting merge process for {} prices", prices.size());
        
        // Group prices by type, subtype, currency, and amount
        Map<String, List<PriceDTO>> priceGroups = prices.stream()
                .collect(Collectors.groupingBy(p -> 
                    p.getType() + "_" + p.getSubtype() + "_" + p.getCurrency() + "_" + p.getAmount()));
        
        List<PriceDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<PriceDTO>> entry : priceGroups.entrySet()) {
            String groupKey = entry.getKey();
            List<PriceDTO> group = entry.getValue();
            
            log.debug("Processing group: {} with {} prices", groupKey, group.size());
            
            if (group.size() == 1) {
                // If only one price in the group, add it directly
                result.add(group.get(0));
                log.debug("Added single price to result: {}, overlapped={}", 
                    group.get(0).getSubtype(), group.get(0).isOverlapped());
            } else {
                // Find groups of prices to merge (with connecting overlaps)
                List<List<PriceDTO>> mergeGroups = findConnectedRanges(group);
                
                for (List<PriceDTO> mergeGroup : mergeGroups) {
                    if (mergeGroup.size() == 1) {
                        // Single price, no merging needed
                        result.add(mergeGroup.get(0));
                        log.debug("Added single price from merge group: {}, overlapped={}", 
                            mergeGroup.get(0).getSubtype(), mergeGroup.get(0).isOverlapped());
                    } else {
                        // Multiple prices to merge
                        PriceDTO mergedPrice = mergePrices(mergeGroup);
                        result.add(mergedPrice);
                        log.debug("Merged {} prices into one: {}, range={} to {}, overlapped={}", 
                            mergeGroup.size(), mergedPrice.getSubtype(), 
                            mergedPrice.getValidFrom(), mergedPrice.getValidTo(),
                            mergedPrice.isOverlapped());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Find groups of prices that form connected date ranges (through overlaps)
     */
    private List<List<PriceDTO>> findConnectedRanges(List<PriceDTO> prices) {
        List<List<PriceDTO>> result = new ArrayList<>();
        boolean[] processed = new boolean[prices.size()];
        
        for (int i = 0; i < prices.size(); i++) {
            if (processed[i]) {
                continue;
            }
            
            List<PriceDTO> connectedGroup = new ArrayList<>();
            connectedGroup.add(prices.get(i));
            processed[i] = true;
            
            boolean foundNewConnection;
            do {
                foundNewConnection = false;
                
                for (int j = 0; j < prices.size(); j++) {
                    if (processed[j]) {
                        continue;
                    }
                    
                    // Check if this price overlaps with any in the current group
                    boolean overlapsWithGroup = false;
                    for (PriceDTO groupPrice : connectedGroup) {
                        if (doRangesOverlap(
                                groupPrice.getValidFrom(), groupPrice.getValidTo(),
                                prices.get(j).getValidFrom(), prices.get(j).getValidTo())) {
                            overlapsWithGroup = true;
                            break;
                        }
                    }
                    
                    if (overlapsWithGroup) {
                        connectedGroup.add(prices.get(j));
                        processed[j] = true;
                        foundNewConnection = true;
                    }
                }
            } while (foundNewConnection);
            
            result.add(connectedGroup);
        }
        
        return result;
    }
    
    /**
     * Merge a group of prices into a single price with the widest date range
     */
    private PriceDTO mergePrices(List<PriceDTO> prices) {
        if (prices.isEmpty()) {
            return null;
        }
        
        PriceDTO reference = prices.get(0);
        
        // Find earliest validFrom and latest validTo
        ZonedDateTime earliestValidFrom = reference.getValidFrom();
        ZonedDateTime latestValidTo = reference.getValidTo();
        
        // Check if any price is marked as overlapped
        boolean anyOverlapped = false;
        
        for (PriceDTO price : prices) {
            if (price.getValidFrom().isBefore(earliestValidFrom)) {
                earliestValidFrom = price.getValidFrom();
            }
            
            if (price.getValidTo().isAfter(latestValidTo)) {
                latestValidTo = price.getValidTo();
            }
            
            if (price.isOverlapped()) {
                anyOverlapped = true;
            }
        }
        
        // Create the merged price
        return PriceDTO.builder()
                .type(reference.getType())
                .subtype(reference.getSubtype())
                .currency(reference.getCurrency())
                .amount(reference.getAmount())
                .validFrom(earliestValidFrom)
                .validTo(latestValidTo)
                .overlapped(anyOverlapped)
                .build();
    }
    
    /**
     * Check if two date ranges overlap
     */
    private boolean doRangesOverlap(ZonedDateTime start1, ZonedDateTime end1, 
                                   ZonedDateTime start2, ZonedDateTime end2) {
        // For a true overlap, start1 must be before end2 AND start2 must be before end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    /**
     * Build the complete price response DTO
     */
    private PriceResponseDTO buildPriceResponse(Article article, List<PriceDTO> prices, int page, int pageSize) {
        return PriceResponseDTO.builder()
                .generated_date(ZonedDateTime.now())
                .article(article.getArticleId())
                .store(article.getStoreId())
                .meta(MetaDTO.builder().page(page).size(pageSize).build())
                .properties(PropertiesDTO.builder()
                        .uom(article.getUom())
                        .description(article.getDescription())
                        .brand(article.getBrand())
                        .model(article.getModel())
                        .build())
                .prices(prices)
                .build();
    }
    
    /**
     * Generate a cache key for the price request
     */
    private String generateCacheKey(String storeId, String articleId, int page, int pageSize) {
        return String.format("%s_%s_%d_%d", storeId, articleId, page, pageSize);
    }
    
    /**
     * Clear the price cache
     */
    public void clearCache() {
        priceCache.clear();
        log.debug("Price cache cleared");
    }
}