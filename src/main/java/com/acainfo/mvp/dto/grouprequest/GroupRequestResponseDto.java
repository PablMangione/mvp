package com.acainfo.mvp.dto.grouprequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestResponseDto {
    private Long requestId;
    private boolean success;
    private String message;
    private int totalRequests; // Total de solicitudes para esa asignatura
}