package com.acainfo.mvp.dto.teacher;

import com.acainfo.mvp.dto.common.BaseDto;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

// DTO básico de profesor
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDto extends BaseDto {
    private String name;
    private String email;
}
