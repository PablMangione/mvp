package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO para procesar solicitudes en lote.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessDto {
    @NotEmpty
    private List<Long> requestIds;
    private String action; // APROBAR, RECHAZAR
    private String comment;
}