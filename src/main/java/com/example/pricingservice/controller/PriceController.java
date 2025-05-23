package com.example.pricingservice.controller;

import com.example.pricingservice.dto.PriceResponseDTO;
import com.example.pricingservice.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for pricing operations
 */
@RestController
@RequestMapping("/v1/prices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pricing API", description = "API endpoints for retrieving product pricing information")
public class PriceController {

    private static final Logger log = LoggerFactory.getLogger(PriceController.class);
    
    private final PriceService priceService;
    
    /**
     * Get prices for a specific store and article
     *
     * @param storeId the store ID
     * @param articleId the article ID
     * @param page the page number (starting from 1)
     * @param pageSize the page size
     * @return the price response
     */
    @GetMapping("/{storeId}/{articleId}")
    @Operation(summary = "Get prices for a specific store and article",
               description = "Returns a list of prices for the specified store and article IDs with pagination support")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prices found",
                    content = @Content(schema = @Schema(implementation = PriceResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Prices not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PriceResponseDTO> getPrices(
            @PathVariable String storeId,
            @PathVariable String articleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        log.info("Received request for prices with storeId: {}, articleId: {}, page: {}, pageSize: {}",
                storeId, articleId, page, pageSize);
        
        PriceResponseDTO response = priceService.getPrices(storeId, articleId, page, pageSize);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Clear the price cache (admin endpoint)
     *
     * @return success message
     */
    @PostMapping("/admin/clear-cache")
    @Operation(summary = "Clear price cache",
               description = "Administrative endpoint to clear the price cache")
    public ResponseEntity<String> clearCache() {
        log.info("Received request to clear price cache");
        priceService.clearCache();
        return ResponseEntity.ok("Cache cleared successfully");
    }
}