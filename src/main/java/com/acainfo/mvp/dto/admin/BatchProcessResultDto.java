package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para resultado de procesamiento en lote.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessResultDto {
    private int processed;
    private List<String> errors;
}