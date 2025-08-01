package com.acainfo.mvp.dto.groupsession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO para transferencia de información de sesiones de grupo.
 * Usado para operaciones de lectura y listado de sesiones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupSessionDto {
    private Long id;
    private Long courseGroupId;  // Referencia al grupo al que pertenece
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;
}