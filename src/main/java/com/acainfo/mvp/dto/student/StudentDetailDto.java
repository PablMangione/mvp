package com.acainfo.mvp.dto.student;

import com.acainfo.mvp.dto.common.BaseDto;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDetailDto extends BaseDto {
    private String name;
    private String email;
    private String major;
    private int activeEnrollments;
    private int pendingPayments;
    private int pendingRequests;
}
