package com.acainfo.mvp.dto.coursegroup;

import com.acainfo.mvp.dto.common.BaseDto;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.CourseGroupType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseGroupDetailDto extends BaseDto {
    private SubjectInfo subject;
    private TeacherInfo teacher;
    private CourseGroupStatus status;
    private CourseGroupType type;
    private BigDecimal price;
    private int enrolledStudents;
    private int maxCapacity;
    private List<GroupSessionDto> sessions;
    private boolean canEnroll;
    private String enrollmentMessage;
}
