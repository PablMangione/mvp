package com.acainfo.mvp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta de verificación de email.
 * Indica si un email ya está registrado en el sistema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckResponseDto {
    private String email;
    private boolean exists;
    private boolean available;
}