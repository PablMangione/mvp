package com.acainfo.mvp.dto.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO estándar para respuestas de error en la API.
 * Proporciona una estructura consistente para todos los errores.
 * Sigue el formato de error de Spring Boot pero personalizable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseDto {

    /**
     * Timestamp del error.
     * Formato ISO-8601: yyyy-MM-dd'T'HH:mm:ss
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Código de estado HTTP.
     * Ej: 400, 401, 404, 500
     */
    private Integer status;

    /**
     * Descripción textual del estado HTTP.
     * Ej: "Bad Request", "Not Found", "Internal Server Error"
     */
    private String error;

    /**
     * Mensaje descriptivo del error.
     * Debe ser comprensible para el usuario final.
     * Ej: "El email ya está registrado en el sistema"
     */
    private String message;

    /**
     * Detalles adicionales del error (opcional).
     * Útil para errores de validación con múltiples campos.
     * Ej: "campo1: debe ser mayor a 0, campo2: es requerido"
     */
    private String details;

    /**
     * Path de la petición que generó el error.
     * Ej: "/api/students/profile"
     */
    private String path;

    /**
     * Código de error interno (opcional).
     * Útil para clasificación interna de errores.
     * Ej: "STUDENT_NOT_FOUND", "INVALID_PASSWORD"
     */
    private String code;

    /**
     * ID de traza para debugging (opcional).
     * Útil para correlacionar logs en sistemas distribuidos.
     */
    private String traceId;
}