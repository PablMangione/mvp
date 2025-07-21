package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para alertas del sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemAlertDto {
    private Long id;
    private String type; // WARNING, ERROR, INFO
    private String message;
    private String priority; // LOW, MEDIUM, HIGH
    private String timestamp;
    private boolean resolved;
}
