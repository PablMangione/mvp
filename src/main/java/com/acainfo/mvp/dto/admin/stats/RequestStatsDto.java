package com.acainfo.mvp.dto.admin.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para estadísticas de solicitudes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatsDto {
    private int total;
    private int pending;
    private int approved;
    private int rejected;
    private double averageProcessingTime;
    private Map<String, Integer> bySubject;
}