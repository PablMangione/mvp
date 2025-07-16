package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.subject.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades Subject y sus DTOs.
 */
@Component
public class SubjectMapper {

    /**
     * Convierte una entidad Subject a SubjectDto básico.
     *
     * @param subject entidad subject
     * @return SubjectDto
     */
    public SubjectDto toDto(Subject subject) {
        if (subject == null) {
            return null;
        }

        SubjectDto dto = SubjectDto.builder()
                .name(subject.getName())
                .major(subject.getMajor())
                .courseYear(subject.getCourseYear())
                .build();

        dto.setId(subject.getId());
        dto.setCreatedAt(subject.getCreatedAt());
        dto.setUpdatedAt(subject.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una lista de entidades Subject a lista de SubjectDto.
     *
     * @param subjects lista de entidades
     * @return lista de DTOs
     */
    public List<SubjectDto> toDtoList(List<Subject> subjects) {
        return subjects.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Subject a SubjectWithGroupsDto incluyendo grupos.
     *
     * @param subject entidad subject con grupos cargados
     * @return SubjectWithGroupsDto
     */
    public SubjectWithGroupsDto toWithGroupsDto(Subject subject) {
        if (subject == null) {
            return null;
        }

        // Filtrar grupos activos
        List<CourseGroup> activeGroups = subject.getCourseGroups().stream()
                .filter(g -> g.getStatus() == CourseGroupStatus.ACTIVE)
                .toList();

        SubjectWithGroupsDto dto = SubjectWithGroupsDto.builder()
                .name(subject.getName())
                .major(subject.getMajor())
                .courseYear(subject.getCourseYear())
                .activeGroups(activeGroups.size())
                .totalGroups(subject.getCourseGroups().size())
                .hasActiveGroups(!activeGroups.isEmpty())
                .availableGroups(activeGroups.stream()
                        .map(this::toCourseGroupSummary)
                        .collect(Collectors.toList()))
                .build();

        dto.setId(subject.getId());
        dto.setCreatedAt(subject.getCreatedAt());
        dto.setUpdatedAt(subject.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte un CourseGroup a CourseGroupSummaryDto.
     *
     * @param group entidad CourseGroup
     * @return CourseGroupSummaryDto
     */
    public CourseGroupSummaryDto toCourseGroupSummary(CourseGroup group) {
        if (group == null) {
            return null;
        }

        return CourseGroupSummaryDto.builder()
                .groupId(group.getId())
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Por asignar")
                .groupType(group.getType().name())
                .groupStatus(group.getStatus().name())
                .price(group.getPrice())
                .enrolledStudents(group.getEnrollments().size())
                .maxCapacity(group.getMaxCapacity()) // Usando el campo real
                .sessions(group.getGroupSessions().stream()
                        .map(this::toSessionTimeDto)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Convierte una GroupSession a SessionTimeDto.
     *
     * @param session entidad GroupSession
     * @return SessionTimeDto
     */
    public SessionTimeDto toSessionTimeDto(GroupSession session) {
        if (session == null) {
            return null;
        }

        return SessionTimeDto.builder()
                .dayOfWeek(session.getDayOfWeek().name())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .build();
    }

    /**
     * Crea una nueva entidad Subject desde CreateSubjectDto.
     *
     * @param createDto DTO de creación
     * @return nueva entidad Subject
     */
    public Subject toEntity(CreateSubjectDto createDto) {
        if (createDto == null) {
            return null;
        }

        return Subject.builder()
                .name(createDto.getName())
                .major(createDto.getMajor())
                .courseYear(createDto.getCourseYear())
                .build();
    }

    /**
     * Actualiza una entidad Subject con datos de UpdateSubjectDto.
     *
     * @param subject entidad a actualizar
     * @param updateDto datos de actualización
     */
    public void updateEntityFromDto(Subject subject, UpdateSubjectDto updateDto) {
        if (updateDto.getName() != null) {
            subject.setName(updateDto.getName());
        }
        if (updateDto.getMajor() != null) {
            subject.setMajor(updateDto.getMajor());
        }
        if (updateDto.getCourseYear() != null) {
            subject.setCourseYear(updateDto.getCourseYear());
        }
    }
}