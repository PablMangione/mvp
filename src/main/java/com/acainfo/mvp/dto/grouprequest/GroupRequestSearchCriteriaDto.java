// En un nuevo archivo: GroupRequestSearchCriteria.java
package com.acainfo.mvp.dto.grouprequest;

import com.acainfo.mvp.model.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GroupRequestSearchCriteriaDto {
    private RequestStatus status;
    private Long studentId;
    private Long subjectId;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
