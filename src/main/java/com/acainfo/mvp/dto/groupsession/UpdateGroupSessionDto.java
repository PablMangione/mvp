package com.acainfo.mvp.dto.groupsession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * DTO para actualizar sesiones de grupo existentes.
 * Todos los campos son opcionales - solo se actualizan los que se envían.
 * Usado por administradores para modificar horarios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupSessionDto {

    // No se permite cambiar el grupo al que pertenece la sesión

    private String dayOfWeek; // MONDAY, TUESDAY, etc.

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 50, message = "Classroom must not exceed 50 characters")
    private String classroom;
}