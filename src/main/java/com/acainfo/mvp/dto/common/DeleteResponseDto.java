package com.acainfo.mvp.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteResponseDto {
    private Long deletedId;
    private String entityType;
    private boolean success;
    private String message;
}
