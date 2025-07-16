package com.acainfo.mvp.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectFilterDto {
    private String major;
    private Integer courseYear;
    private Boolean onlyWithActiveGroups;
}
