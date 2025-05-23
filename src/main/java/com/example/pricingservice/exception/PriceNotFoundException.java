package com.example.pricingservice.exception;

/**
 * Exception thrown when prices are not found for a given request
 */
public class PriceNotFoundException extends RuntimeException {
    
    public PriceNotFoundException(String message) {
        super(message);
    }
    
    public PriceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}