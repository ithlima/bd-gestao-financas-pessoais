package com.example.financas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada para <b>quitar</b> um título — pagar uma despesa ou receber
 * uma receita.
 *
 * <p>É o único caminho pelo qual uma movimentação financeira nasce a partir de um
 * título. A operação é atômica: ou a movimentação é criada <i>e</i> o título é
 * atualizado, ou nada acontece.
 *
 * <p>O {@code valor} pode ser menor que o valor previsto — é o que permite
 * <b>pagamento parcial</b> (RN08). Nesse caso o título continua PENDENTE e passa
 * a exibir o quanto ainda falta ({@code valorEmAberto}). Quando a soma dos
 * pagamentos atinge o previsto, o título vira PAGO e recebe a
 * {@code dataPagamento} (RN10).
 *
 * <pre>
 * POST /api/v1/titulos/5/pagar
 * { "valor": 150.00, "data": "2026-03-08", "contaId": 2 }
 * </pre>
 */
public record PagamentoRequest(
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,
    @NotNull(message = "A data do pagamento é obrigatória") LocalDate data,
    @NotNull(message = "A conta é obrigatória") Long contaId) {}
