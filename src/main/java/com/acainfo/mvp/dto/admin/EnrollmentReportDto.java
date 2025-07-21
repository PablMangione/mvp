package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para reporte de inscripciones.
 * Coincide con la estructura esperada por el frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentReportDto {
    private String period;
    private int totalEnrollments;
    private Map<String, Integer> bySubject;
    private List<GroupEnrollment> byGroup;
    private List<TrendData> trends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupEnrollment {
        private Long groupId;
        private String groupName;
        private int enrollments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private String date;
        private int count;
    }
}