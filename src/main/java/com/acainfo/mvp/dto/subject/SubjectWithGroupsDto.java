package com.acainfo.mvp.dto.subject;

import com.acainfo.mvp.dto.common.BaseDto;
import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectWithGroupsDto extends BaseDto {
    private String name;
    private String major;
    private Integer courseYear;
    private int activeGroups;
    private int totalGroups;
    private boolean hasActiveGroups;
    private List<CourseGroupSummaryDto> availableGroups;
}
