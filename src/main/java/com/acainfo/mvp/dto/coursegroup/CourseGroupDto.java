package com.acainfo.mvp.dto.coursegroup;

import com.acainfo.mvp.dto.common.BaseDto;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import lombok.*;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGroupDto extends BaseDto {
    private Long subjectId;
    private String subjectName;
    private Long teacherId;
    private String teacherName;
    private CourseGroupStatus status;
    private CourseGroupType type;
    private BigDecimal price;
    private int enrolledStudents;
}