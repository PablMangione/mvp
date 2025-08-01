package com.acainfo.mvp.dto.groupsession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * DTO para crear nuevas sesiones de grupo.
 * Usado por administradores al definir horarios de grupos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupSessionDto {

    @NotNull(message = "Course group ID is required")
    private Long courseGroupId;

    @NotBlank(message = "Day of week is required")
    private String dayOfWeek; // MONDAY, TUESDAY, etc.

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Size(max = 50, message = "Classroom must not exceed 50 characters")
    private String classroom;
}