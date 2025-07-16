package com.acainfo.mvp.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseGroupSummaryDto {
    private Long groupId;
    private String teacherName;
    private String groupType;
    private String groupStatus;
    private java.math.BigDecimal price;
    private int enrolledStudents;
    private int maxCapacity;
    private List<SessionTimeDto> sessions;
}
