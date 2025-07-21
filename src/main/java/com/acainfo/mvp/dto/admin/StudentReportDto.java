package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para reporte de estudiantes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportDto {
    private int totalStudents;
    private Map<String, Integer> byMajor;
    private Map<Integer, Integer> byYear;
    private int activeStudents;
    private int inactiveStudents;
    private double averageEnrollmentsPerStudent;
}