package com.oiaaconta.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ComparativoResponse {
    private ResumoFinanceiroResponse atual;
    private ResumoFinanceiroResponse anterior;
    private BigDecimal variacaoPercentual;
}
