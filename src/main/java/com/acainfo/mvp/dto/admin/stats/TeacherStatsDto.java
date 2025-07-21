package com.acainfo.mvp.dto.admin.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de profesores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStatsDto {
    private int total;
    private int withActiveGroups;
    private double averageGroupsPerTeacher;
}