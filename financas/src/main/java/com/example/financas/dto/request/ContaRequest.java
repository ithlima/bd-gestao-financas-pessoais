package com.example.financas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dados de entrada para cadastrar ou atualizar uma conta financeira.
 *
 * <p>{@code saldoInicial} é opcional: quando não informado, assume zero. Isso
 * cobre o caso comum de cadastrar uma conta nova que ainda não tem saldo.
 */
public record ContaRequest(
    @NotBlank(message = "O nome da conta é obrigatório")
        @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres")
        String nome,
    @NotBlank(message = "O tipo da conta é obrigatório")
        @Size(max = 50, message = "O tipo da conta deve ter no máximo 50 caracteres")
        String tipo,
    @PositiveOrZero(message = "O saldo inicial não pode ser negativo")
        BigDecimal saldoInicial,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {

  /** Saldo inicial tolerante a campo não informado. */
  public BigDecimal saldoInicialOuZero() {
    return saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
  }
}
