package com.example.pricingservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MetaDTO {
    private int page;
    private int size;
}