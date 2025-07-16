package com.acainfo.mvp.dto.grouprequest;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDto {

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    private String comments; // Comentarios opcionales del estudiante
}
