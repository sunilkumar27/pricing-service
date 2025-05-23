package com.example.pricingservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PriceDTO {
    private String type;
    private String subtype;
    private String currency;
    private BigDecimal amount;
    private ZonedDateTime validFrom;
    private ZonedDateTime validTo;
    private boolean overlapped;
}