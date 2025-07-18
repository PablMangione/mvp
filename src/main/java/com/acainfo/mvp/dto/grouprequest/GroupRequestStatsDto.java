package com.acainfo.mvp.dto.grouprequest;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de solicitudes de grupo por asignatura.
 * Útil para que los administradores visualicen la demanda de grupos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestStatsDto {
    private Long subjectId;
    private String subjectName;
    private int totalRequests;
    private int pendingRequests;
    private int approvedRequests;
    private int rejectedRequests;
}
