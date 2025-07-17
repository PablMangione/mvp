package com.acainfo.mvp.dto.student;

import com.acainfo.mvp.model.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequestSummaryDto {
    private Long requestId;
    private Long subjectId;
    private String subjectName;
    private LocalDateTime createdAt;
    private RequestStatus status;
}
