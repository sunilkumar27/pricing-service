package com.example.pricingservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorResponseDTO {
    private String type;
    private String title;
    private int status;
    private String detail;
}