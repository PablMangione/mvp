package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para programar reportes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleReportDto {
    private String reportType;
    private String frequency; // DAILY, WEEKLY, MONTHLY
    private List<String> recipients;
    private Map<String, Object> filters;
}