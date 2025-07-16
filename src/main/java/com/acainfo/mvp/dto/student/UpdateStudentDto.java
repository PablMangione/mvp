package com.acainfo.mvp.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO para actualizar estudiante (admin)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentDto {
    private String name;
    private String email;
    private String major;
}
