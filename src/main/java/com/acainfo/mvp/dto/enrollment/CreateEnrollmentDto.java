package com.acainfo.mvp.dto.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnrollmentDto {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Course Group ID is required")
    private Long courseGroupId;
}
