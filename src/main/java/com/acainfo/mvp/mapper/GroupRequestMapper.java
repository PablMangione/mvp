package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.grouprequest.GroupRequestDto;
import com.acainfo.mvp.dto.student.GroupRequestSummaryDto;
import com.acainfo.mvp.model.GroupRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades GroupRequest y sus DTOs.
 */
@Component
public class GroupRequestMapper {

    /**
     * Convierte una entidad GroupRequest a GroupRequestDto.
     *
     * @param request entidad GroupRequest
     * @return GroupRequestDto
     */
    public GroupRequestDto toDto(GroupRequest request) {
        if (request == null) {
            return null;
        }

        GroupRequestDto dto = GroupRequestDto.builder()
                .studentId(request.getStudent().getId())
                .studentName(request.getStudent().getName())
                .subjectId(request.getSubject().getId())
                .subjectName(request.getSubject().getName())
                .requestDate(request.getRequestDate())
                .status(request.getStatus())
                .build();

        dto.setId(request.getId());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una entidad GroupRequest a GroupRequestSummaryDto para vista de estudiante.
     *
     * @param request entidad GroupRequest
     * @return GroupRequestSummaryDto
     */
    public GroupRequestSummaryDto toSummaryDto(GroupRequest request) {
        if (request == null) {
            return null;
        }

        return GroupRequestSummaryDto.builder()
                .requestId(request.getId())
                .subjectId(request.getSubject().getId())
                .subjectName(request.getSubject().getName())
                .requestDate(request.getRequestDate())
                .status(request.getStatus())
                .build();
    }

    /**
     * Convierte una lista de entidades GroupRequest a lista de GroupRequestDto.
     *
     * @param requests lista de entidades
     * @return lista de DTOs
     */
    public List<GroupRequestDto> toDtoList(List<GroupRequest> requests) {
        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de entidades GroupRequest a lista de GroupRequestSummaryDto.
     *
     * @param requests lista de entidades
     * @return lista de DTOs resumen
     */
    public List<GroupRequestSummaryDto> toSummaryDtoList(List<GroupRequest> requests) {
        return requests.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}
