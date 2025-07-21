package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas del dashboard administrativo.
 * Estructura exacta esperada por el frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private int totalStudents;
    private int totalTeachers;
    private int totalSubjects;
    private int totalGroups;
    private int activeEnrollments;
    private int pendingRequests;

    private RecentActivity recentActivity;
    private GroupOccupancy groupOccupancy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private int newStudents;
        private int newEnrollments;
        private int cancelledEnrollments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupOccupancy {
        private double average;
        private int full;
        private int nearFull;
    }
}