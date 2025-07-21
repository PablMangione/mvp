package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para configuración de reportes personalizados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomReportDto {
    private String reportType;
    private Map<String, Object> filters;
    private String format; // PDF, EXCEL, CSV
}