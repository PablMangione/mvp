package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.grouprequest.*;
import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.RequestStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre GroupRequest entity y sus DTOs.
 * Maneja la transformación de solicitudes de creación de grupos.
 */
@Component
public class GroupRequestMapper {

    /**
     * Convierte GroupRequest entity a GroupRequestDto (vista completa).
     * Incluye información del estudiante y asignatura.
     */
    public GroupRequestDto toDto(GroupRequest groupRequest) {
        if (groupRequest == null) {
            return null;
        }

        return GroupRequestDto.builder()
                .studentId(groupRequest.getStudent().getId())
                .studentName(groupRequest.getStudent().getName())
                .subjectId(groupRequest.getSubject().getId())
                .subjectName(groupRequest.getSubject().getName())
                .createdAt(groupRequest.getCreatedAt())
                .status(groupRequest.getStatus())
                .build();
    }

    /**
     * Crea GroupRequest desde CreateGroupRequestDto.
     * Requiere Student y Subject ya existentes.
     */
    public GroupRequest toEntity(CreateGroupRequestDto dto, Student student, Subject subject) {
        if (dto == null || student == null || subject == null) {
            return null;
        }

        return GroupRequest.builder()
                .student(student)
                .subject(subject)
                .status(RequestStatus.PENDING) // Estado inicial siempre pendiente
                .build();
    }

    /**
     * Crea GroupRequestResponseDto para confirmar solicitud creada.
     * Incluye información adicional como el total de solicitudes.
     */
    public GroupRequestResponseDto toResponseDto(GroupRequest groupRequest,
                                                 boolean success,
                                                 String message,
                                                 int totalRequests) {
        if (groupRequest == null && !success) {
            // Caso de error sin solicitud creada
            return GroupRequestResponseDto.builder()
                    .requestId(null)
                    .success(false)
                    .message(message)
                    .totalRequests(0)
                    .build();
        }

        return GroupRequestResponseDto.builder()
                .requestId(groupRequest != null ? groupRequest.getId() : null)
                .success(success)
                .message(message)
                .totalRequests(totalRequests)
                .build();
    }

    /**
     * Crea respuesta de error para solicitud fallida.
     */
    public GroupRequestResponseDto toErrorResponse(String errorMessage) {
        return GroupRequestResponseDto.builder()
                .requestId(null)
                .success(false)
                .message(errorMessage)
                .totalRequests(0)
                .build();
    }

    /**
     * Actualiza el estado de una solicitud.
     * Usado por admin para aprobar/rechazar.
     */
    public void updateStatus(GroupRequest groupRequest, UpdateRequestStatusDto dto) {
        if (groupRequest == null || dto == null) {
            return;
        }

        groupRequest.setStatus(dto.getStatus());
        // El adminComments y createdGroupId se manejarían en el servicio
        // ya que requieren lógica adicional o entidades relacionadas
    }

    /**
     * Convierte lista de GroupRequests a lista de GroupRequestDtos.
     * Útil para vistas administrativas.
     */
    public List<GroupRequestDto> toDtoList(List<GroupRequest> groupRequests) {
        if (groupRequests == null) {
            return new ArrayList<>();
        }

        return groupRequests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Filtra solicitudes por estado y convierte a DTOs.
     * Útil para mostrar solo pendientes, aprobadas, etc.
     */
    public List<GroupRequestDto> filterByStatusAndConvert(List<GroupRequest> groupRequests,
                                                          RequestStatus status) {
        if (groupRequests == null || status == null) {
            return new ArrayList<>();
        }

        return groupRequests.stream()
                .filter(request -> status.equals(request.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Genera mensaje descriptivo del estado de la solicitud.
     */
    public String getStatusMessage(RequestStatus status) {
        if (status == null) {
            return "Estado desconocido";
        }

        switch (status) {
            case PENDING:
                return "Solicitud pendiente de revisión";
            case APPROVED:
                return "Solicitud aprobada";
            case REJECTED:
                return "Solicitud rechazada";
            default:
                return status.toString();
        }
    }

    /**
     * Genera mensaje para el estudiante sobre su solicitud.
     */
    public String generateStudentMessage(GroupRequest request) {
        if (request == null) {
            return "Solicitud no encontrada";
        }

        String baseMessage = String.format("Tu solicitud para %s está %s",
                request.getSubject().getName(),
                getStatusMessage(request.getStatus()).toLowerCase());

        if (request.getStatus() == RequestStatus.PENDING) {
            return baseMessage + ". Te notificaremos cuando haya novedades.";
        } else if (request.getStatus() == RequestStatus.APPROVED) {
            return baseMessage + ". Pronto se abrirá un nuevo grupo.";
        } else {
            return baseMessage + ". Puedes intentar más tarde si hay más interesados.";
        }
    }

    /**
     * Verifica si una solicitud puede ser modificada.
     * Solo las solicitudes pendientes pueden cambiar de estado.
     */
    public boolean canBeModified(GroupRequest request) {
        return request != null && RequestStatus.PENDING.equals(request.getStatus());
    }
}