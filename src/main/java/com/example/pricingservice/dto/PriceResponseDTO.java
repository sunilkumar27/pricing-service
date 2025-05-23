package com.example.pricingservice.dto;

import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PriceResponseDTO {
    private ZonedDateTime generated_date;
    private String article;
    private String store;
    private MetaDTO meta;
    private PropertiesDTO properties;
    private List<PriceDTO> prices;
}