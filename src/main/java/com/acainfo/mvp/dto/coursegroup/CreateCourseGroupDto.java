package com.acainfo.mvp.dto.coursegroup;

import com.acainfo.mvp.dto.groupsession.CreateGroupSessionDto;
import com.acainfo.mvp.model.enums.CourseGroupType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseGroupDto {

    @NotNull(message = "Subject is required")
    private Long subjectId;

    private Long teacherId; // Puede ser null inicialmente

    @NotNull(message = "Type is required")
    private CourseGroupType type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    private List<CreateGroupSessionDto> sessions;
}
