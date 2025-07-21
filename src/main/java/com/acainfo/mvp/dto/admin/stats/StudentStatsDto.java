package com.acainfo.mvp.dto.admin.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para estadísticas de estudiantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatsDto {
    private int total;
    private Map<String, Integer> byMajor;
    private int activeEnrollments;
}