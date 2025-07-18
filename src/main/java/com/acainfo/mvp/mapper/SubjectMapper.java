package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.subject.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre Subject entity y sus DTOs.
 * Maneja la transformación de asignaturas y sus grupos asociados.
 */
@Component
public class SubjectMapper {

    /**
     * Convierte Subject entity a SubjectDto (información básica).
     */
    public SubjectDto toDto(Subject subject) {
        if (subject == null) {
            return null;
        }

        return SubjectDto.builder()
                .name(subject.getName())
                .major(subject.getMajor())
                .courseYear(subject.getCourseYear())
                .build();
    }

    /**
     * Convierte Subject a SubjectWithGroupsDto incluyendo grupos activos.
     * Usado para mostrar asignaturas con opciones de inscripción.
     */
    public SubjectWithGroupsDto toWithGroupsDto(Subject subject) {
        if (subject == null) {
            return null;
        }

        // Filtrar solo grupos activos
        List<CourseGroup> activeGroups = subject.getCourseGroups().stream()
                .filter(group -> CourseGroupStatus.ACTIVE.equals(group.getStatus()))
                .collect(Collectors.toList());

        // Convertir grupos a resumen
        List<CourseGroupSummaryDto> groupSummaries = activeGroups.stream()
                .map(this::toCourseGroupSummary)
                .collect(Collectors.toList());

        return SubjectWithGroupsDto.builder()
                .name(subject.getName())
                .major(subject.getMajor())
                .courseYear(subject.getCourseYear())
                .activeGroups(activeGroups.size())
                .totalGroups(subject.getCourseGroups().size())
                .hasActiveGroups(!activeGroups.isEmpty())
                .availableGroups(groupSummaries)
                .build();
    }

    /**
     * Crea nuevo Subject desde CreateSubjectDto.
     * Usado por admin para crear asignaturas.
     */
    public Subject toEntity(CreateSubjectDto dto) {
        if (dto == null) {
            return null;
        }

        return Subject.builder()
                .name(dto.getName())
                .major(dto.getMajor())
                .courseYear(dto.getCourseYear())
                .build();
    }

    /**
     * Convierte CourseGroup a CourseGroupSummaryDto.
     * Incluye información resumida del grupo para vista de asignatura.
     */
    private CourseGroupSummaryDto toCourseGroupSummary(CourseGroup group) {
        if (group == null) {
            return null;
        }

        // Convertir sesiones del grupo
        List<SessionTimeDto> sessions = group.getGroupSessions().stream()
                .map(this::toSessionTimeDto)
                .collect(Collectors.toList());

        return CourseGroupSummaryDto.builder()
                .groupId(group.getId())
                .teacherName(group.getTeacher() != null
                        ? group.getTeacher().getName()
                        : "Sin asignar")
                .groupType(group.getType().toString())
                .groupStatus(group.getStatus().toString())
                .price(group.getPrice())
                .enrolledStudents(group.getEnrollments().size())
                .maxCapacity(group.getMaxCapacity())
                .sessions(sessions)
                .build();
    }

    /**
     * Convierte GroupSession a SessionTimeDto.
     * Información básica de horario para cada sesión.
     */
    private SessionTimeDto toSessionTimeDto(GroupSession session) {
        if (session == null) {
            return null;
        }

        return SessionTimeDto.builder()
                .dayOfWeek(session.getDayOfWeek().toString())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .build();
    }

    /**
     * Actualiza campos de Subject desde UpdateSubjectDto.
     * Usado por admin para modificar asignaturas.
     */
    public void updateFromDto(Subject subject, UpdateSubjectDto dto) {
        if (subject == null || dto == null) {
            return;
        }

        if (dto.getName() != null) {
            subject.setName(dto.getName());
        }
        if (dto.getMajor() != null) {
            subject.setMajor(dto.getMajor());
        }
        if (dto.getCourseYear() != null) {
            subject.setCourseYear(dto.getCourseYear());
        }
    }

    /**
     * Convierte lista de Subjects a lista de SubjectDtos.
     * Útil para endpoints que devuelven múltiples asignaturas.
     */
    public List<SubjectDto> toDtoList(List<Subject> subjects) {
        if (subjects == null) {
            return new ArrayList<>();
        }

        return subjects.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte lista de Subjects a lista de SubjectWithGroupsDtos.
     * Útil para mostrar asignaturas con sus grupos disponibles.
     */
    public List<SubjectWithGroupsDto> toWithGroupsDtoList(List<Subject> subjects) {
        if (subjects == null) {
            return new ArrayList<>();
        }

        return subjects.stream()
                .map(this::toWithGroupsDto)
                .collect(Collectors.toList());
    }
}
