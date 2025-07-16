package com.acainfo.mvp.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherScheduleDto {
    private Long teacherId;
    private String teacherName;
    private List<ScheduleSlotDto> schedule;
}
