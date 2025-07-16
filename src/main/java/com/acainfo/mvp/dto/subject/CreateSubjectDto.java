package com.acainfo.mvp.dto.subject;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubjectDto {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Major is required")
    @Size(max = 100, message = "Major must not exceed 100 characters")
    private String major;

    @NotNull(message = "Course year is required")
    @Min(value = 1, message = "Course year must be at least 1")
    @Max(value = 6, message = "Course year must not exceed 6")
    private Integer courseYear;
}
