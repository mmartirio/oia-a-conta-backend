package com.oiaaconta.ifood.dto.response;

import com.oiaaconta.ifood.enums.IfoodVinculoStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IfoodVinculoStatusResponse {
    private IfoodVinculoStatus status;
}
