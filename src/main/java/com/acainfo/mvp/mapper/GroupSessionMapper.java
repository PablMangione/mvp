package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.groupsession.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.enums.DayOfWeek;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre GroupSession entity y sus DTOs.
 * Maneja la transformación de sesiones de grupo y su información asociada.
 */
@Component
public class GroupSessionMapper {

    /**
     * Convierte GroupSession entity a GroupSessionDto (información básica).
     */
    public GroupSessionDto toDto(GroupSession groupSession) {
        if (groupSession == null) {
            return null;
        }

        return GroupSessionDto.builder()
                .id(groupSession.getId())
                .courseGroupId(groupSession.getCourseGroup().getId())
                .dayOfWeek(groupSession.getDayOfWeek().toString())
                .startTime(groupSession.getStartTime())
                .endTime(groupSession.getEndTime())
                .classroom(groupSession.getClassroom())
                .build();
    }

    /**
     * Convierte GroupSession a GroupSessionDetailDto con información completa.
     * Incluye datos del grupo, asignatura y profesor.
     */
    public GroupSessionDetailDto toDetailDto(GroupSession groupSession) {
        if (groupSession == null) {
            return null;
        }

        CourseGroup courseGroup = groupSession.getCourseGroup();

        return GroupSessionDetailDto.builder()
                // Información de la sesión
                .id(groupSession.getId())
                .dayOfWeek(groupSession.getDayOfWeek().toString())
                .startTime(groupSession.getStartTime())
                .endTime(groupSession.getEndTime())
                .classroom(groupSession.getClassroom())
                // Información del grupo
                .groupStatus(courseGroup.getStatus().toString())
                .maxCapacity(courseGroup.getMaxCapacity())
                .enrolledStudents(courseGroup.getEnrollments().size())
                // Información de la asignatura
                .subjectName(courseGroup.getSubject().getName())
                // Información del profesor
                .teacherName(courseGroup.getTeacher() != null
                        ? courseGroup.getTeacher().getName()
                        : "Sin asignar")
                .build();
    }

    /**
     * Crea GroupSession desde CreateGroupSessionDto.
     * Requiere CourseGroup ya existente.
     */
    public GroupSession toEntity(CreateGroupSessionDto dto, CourseGroup courseGroup) {
        if (dto == null || courseGroup == null) {
            return null;
        }

        return GroupSession.builder()
                .courseGroup(courseGroup)
                .dayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek()))
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .classroom(dto.getClassroom())
                .build();
    }

    /**
     * Actualiza GroupSession desde UpdateGroupSessionDto.
     * Solo actualiza los campos que vienen con valor.
     */
    public void updateFromDto(GroupSession groupSession, UpdateGroupSessionDto dto) {
        if (groupSession == null || dto == null) {
            return;
        }

        if (dto.getDayOfWeek() != null) {
            groupSession.setDayOfWeek(DayOfWeek.valueOf(dto.getDayOfWeek()));
        }
        if (dto.getStartTime() != null) {
            groupSession.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            groupSession.setEndTime(dto.getEndTime());
        }
        if (dto.getClassroom() != null) {
            groupSession.setClassroom(dto.getClassroom());
        }
    }

    /**
     * Convierte lista de GroupSessions a lista de GroupSessionDtos.
     */
    public List<GroupSessionDto> toDtoList(List<GroupSession> groupSessions) {
        if (groupSessions == null) {
            return new ArrayList<>();
        }

        return groupSessions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte lista de GroupSessions a lista de GroupSessionDetailDtos.
     */
    public List<GroupSessionDetailDto> toDetailDtoList(List<GroupSession> groupSessions) {
        if (groupSessions == null) {
            return new ArrayList<>();
        }

        return groupSessions.stream()
                .map(this::toDetailDto)
                .collect(Collectors.toList());
    }
}