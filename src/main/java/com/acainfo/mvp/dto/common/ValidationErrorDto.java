package com.acainfo.mvp.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar errores de validación individuales.
 * Se usa dentro de ErrorResponseDto para detallar campos con errores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorDto {
    private String field;
    private Object rejectedValue;
    private String message;
}
