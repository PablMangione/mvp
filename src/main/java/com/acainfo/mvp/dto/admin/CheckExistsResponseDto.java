package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para verificación de existencia.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckExistsResponseDto {
    private boolean exists;
}