package com.example.pricingservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PropertiesDTO {
    private String uom;
    private String description;
    private String brand;
    private String model;
}