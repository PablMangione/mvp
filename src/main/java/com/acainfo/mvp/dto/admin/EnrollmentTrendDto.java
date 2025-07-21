package com.acainfo.mvp.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para tendencias de inscripciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentTrendDto {
    private List<String> labels;
    private List<Integer> data;
}