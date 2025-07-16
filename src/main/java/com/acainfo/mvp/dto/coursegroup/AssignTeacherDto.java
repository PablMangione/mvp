package com.acainfo.mvp.dto.coursegroup;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeacherDto {

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;
}
