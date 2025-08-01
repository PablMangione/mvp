package com.acainfo.mvp.dto.groupsession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO con información detallada de una sesión de grupo.
 * Incluye información contextual del grupo, asignatura y profesor.
 * Usado para vistas detalladas y reportes administrativos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSessionDetailDto {
    // Información de la sesión
    private Long id;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;

    private String groupStatus;
    private int maxCapacity;
    private int enrolledStudents;

    // Información de la asignatura
    private String subjectName;

    // Información del profesor
    private String teacherName;
}