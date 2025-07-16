package com.acainfo.mvp.dto.grouprequest;

import com.acainfo.mvp.model.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestStatusDto {

    @NotNull(message = "Status is required")
    private RequestStatus status;

    private String adminComments;
    private Long createdGroupId; // Si se aprueba y se crea un grupo
}