package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.auth.CurrentUserDto;
import com.acainfo.mvp.dto.auth.LoginResponseDto;
import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.ScheduleSlotDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre Teacher entity y sus DTOs.
 * Maneja la lógica de transformación considerando seguridad con sesiones HTTP.
 */
@Component
public class TeacherMapper {

    private final PasswordEncoder passwordEncoder;

    public TeacherMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Convierte Teacher entity a TeacherDto (información pública).
     * No incluye password ni información sensible.
     */
    public TeacherDto toDto(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        TeacherDto dev = TeacherDto.builder()
                .name(teacher.getName())
                .email(teacher.getEmail())
                .build();
        dev.setId(teacher.getId());
        return dev;
    }

    /**
     * Convierte Teacher a LoginResponseDto tras autenticación exitosa.
     * No incluye token ya que usamos sesiones HTTP.
     */
    public LoginResponseDto toLoginResponse(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        return LoginResponseDto.builder()
                .id(teacher.getId())
                .email(teacher.getEmail())
                .name(teacher.getName())
                .role("TEACHER")
                .authenticated(true)
                .build();
    }

    /**
     * Convierte Teacher a CurrentUserDto para verificación de sesión.
     */
    public CurrentUserDto toCurrentUserDto(Teacher teacher) {
        if (teacher == null) {
            return CurrentUserDto.builder()
                    .authenticated(false)
                    .build();
        }

        return CurrentUserDto.builder()
                .id(teacher.getId())
                .email(teacher.getEmail())
                .name(teacher.getName())
                .role("TEACHER")
                .authenticated(true)
                .build();
    }

    /**
     * Crea nuevo Teacher desde CreateTeacherDto (usado por admin).
     * Hashea el password antes de guardar.
     */
    public Teacher toEntity(CreateTeacherDto dto) {
        if (dto == null) {
            return null;
        }

        return Teacher.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build();
    }

    /**
     * Convierte Teacher con sus sesiones a TeacherScheduleDto.
     * Incluye el horario semanal completo del profesor.
     */
    public TeacherScheduleDto toScheduleDto(Teacher teacher, List<GroupSession> sessions) {
        if (teacher == null) {
            return null;
        }

        List<ScheduleSlotDto> schedule = sessions != null
                ? sessions.stream()
                .map(this::toScheduleSlotDto)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return TeacherScheduleDto.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getName())
                .schedule(schedule)
                .build();
    }

    public PageResponseDto<TeacherDto> toPageResponseDto(Page<TeacherDto> page) {
        return PageResponseDto.<TeacherDto>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    /**
     * Convierte GroupSession a ScheduleSlotDto para el horario del profesor.
     * Incluye información relevante de cada sesión.
     */
    private ScheduleSlotDto toScheduleSlotDto(GroupSession session) {
        if (session == null) {
            return null;
        }

        return ScheduleSlotDto.builder()
                .dayOfWeek(session.getDayOfWeek().toString())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .subjectName(session.getCourseGroup().getSubject().getName())
                .courseGroupId(session.getCourseGroup().getId())
                .groupType(session.getCourseGroup().getType().toString())
                .enrolledStudents(session.getCourseGroup().getEnrollments().size())
                .build();
    }

    public List<TeacherDto> toDtoList(List<Teacher> teachers) {
        if (teachers == null) {
            return new ArrayList<>();
        }

        return teachers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza campos básicos del Teacher (sin password ni email).
     * Usado para actualizaciones parciales por admin.
     */
    public void updateBasicInfo(Teacher teacher, TeacherDto dto) {
        if (teacher == null || dto == null) {
            return;
        }

        if (dto.getName() != null) {
            teacher.setName(dto.getName());
        }
        // Email no se actualiza por seguridad
    }

    /**
     * Valida si un password sin hashear coincide con el hasheado.
     */
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Hashea un nuevo password.
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
