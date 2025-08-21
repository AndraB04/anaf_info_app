package com.backend.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response model")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "404")
    private int status;
    
    @Schema(description = "Error message", example = "Company not found")
    private String message;
    
    @Schema(description = "Detailed error description", example = "No company found with CUI: 12345678")
    private String details;
    
    @Schema(description = "Timestamp when the error occurred", example = "2024-08-06T10:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request path that caused the error", example = "/api/firma/12345678")
    private String path;
}
