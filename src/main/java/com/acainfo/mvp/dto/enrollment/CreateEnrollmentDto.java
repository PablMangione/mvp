package com.acainfo.mvp.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnrollmentDto {

    @Null(message = "El id del estudiante se asigna automáticamente")
    private Long studentId;

    @NotNull(message = "El grupo es obligatorio")
    private Long courseGroupId;
}
