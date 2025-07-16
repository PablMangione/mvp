package com.acainfo.mvp.dto.teacher;

import com.acainfo.mvp.dto.common.BaseDto;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDetailDto extends BaseDto {
    private String name;
    private String email;
    private int totalGroups;
    private int activeGroups;
}
