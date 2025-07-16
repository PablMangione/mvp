package com.acainfo.mvp.dto.coursegroup;

import com.acainfo.mvp.model.enums.CourseGroupStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupStatusDto {

    @NotNull(message = "Status is required")
    private CourseGroupStatus status;

    private String reason; // Para auditoría
}
