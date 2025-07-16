package com.acainfo.mvp.dto.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlotDto {
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroom;
    private String subjectName;
    private Long courseGroupId;
    private String groupType;
    private int enrolledStudents;
}
