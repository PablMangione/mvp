package com.acainfo.mvp.dto.grouprequest;

import com.acainfo.mvp.dto.common.BaseDto;
import com.acainfo.mvp.model.enums.RequestStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupRequestDto extends BaseDto {
    private Long studentId;
    private String studentName;
    private Long subjectId;
    private String subjectName;
    private LocalDateTime requestDate;
    private RequestStatus status;
}
