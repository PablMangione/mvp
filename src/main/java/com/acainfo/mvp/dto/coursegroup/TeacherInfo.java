package com.acainfo.mvp.dto.coursegroup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfo {
    private Long id;
    private String name;
    private String email;
}
