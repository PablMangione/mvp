package com.acainfo.mvp.dto.admin.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para estadísticas de asignaturas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatsDto {
    private int total;
    private Map<String, Integer> byMajor;
    private Map<Integer, Integer> byYear;
    private int withActiveGroups;
}
