package com.acainfo.mvp.dto.student;

import com.acainfo.mvp.dto.common.BaseDto;
import lombok.*;

// DTO básico de estudiante (sin información sensible)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDto extends BaseDto {
    private String name;
    private String email;
    private String major;
}
