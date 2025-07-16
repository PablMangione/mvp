package com.acainfo.mvp.dto.subject;

import com.acainfo.mvp.dto.common.BaseDto;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

// DTO básico de asignatura
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectDto extends BaseDto {
    private String name;
    private String major;
    private Integer courseYear;
}
