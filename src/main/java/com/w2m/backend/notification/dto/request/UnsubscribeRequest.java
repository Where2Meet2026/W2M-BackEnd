package com.w2m.backend.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UnsubscribeRequest {

    @NotBlank(message = "endpoint는 필수입니다.")
    private String endpoint;
}
