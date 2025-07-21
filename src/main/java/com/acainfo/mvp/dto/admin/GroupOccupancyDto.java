package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para ocupación de grupos individuales.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupOccupancyDto {
    private Long groupId;
    private String groupName;
    private int capacity;
    private int enrolled;
    private double percentage;
}