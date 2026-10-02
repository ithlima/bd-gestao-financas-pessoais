package com.example.financas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada para <b>receber</b> um título de receita — registrar que o
 * dinheiro efetivamente entrou.
 *
 * <p>É a operação espelho de {@link PagamentoRequest}. As duas existem como
 * conceitos separados porque, do ponto de vista do usuário, "paguei" e "recebi"
 * são coisas diferentes e merecem endpoints diferentes. Internamente, porém,
 * ambas chamam a <b>mesma</b> rotina de quitação: a única diferença é a direção
 * do dinheiro, que já está no {@code tipo} do título.
 *
 * <p>Essa é uma das simplificações obtidas ao absorver as tabelas
 * {@code recebimento} e {@code pagamento} em {@code movimentacao}: antes existiam
 * duas entidades e dois códigos quase idênticos; agora existe um só.
 */
public record RecebimentoRequest(
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,
    @NotNull(message = "A data do recebimento é obrigatória") LocalDate data,
    @NotNull(message = "A conta é obrigatória") Long contaId) {}
