package com.acainfo.mvp.dto.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionTimeDto {
    private String dayOfWeek;
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    private String classroom;
}
