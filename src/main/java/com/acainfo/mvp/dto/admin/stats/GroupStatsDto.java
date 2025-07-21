package com.acainfo.mvp.dto.admin.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para estadísticas de grupos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupStatsDto {
    private int total;
    private Map<String, Integer> byStatus;
    private double averageOccupancy;
    private int withWaitingList;
}