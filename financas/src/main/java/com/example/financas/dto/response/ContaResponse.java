package com.example.financas.dto.response;

import java.math.BigDecimal;

/**
 * Representação pública de uma conta financeira.
 *
 * <p>{@code saldoAtual} não é uma coluna do banco: é calculado como
 * {@code saldoInicial + receitas realizadas - despesas realizadas}. Ele aparece
 * aqui porque é a informação que o usuário realmente quer ver, e no novo modelo
 * esse cálculo é uma consulta única (ver
 * {@code MovimentacaoRepository#calcularSaldoPorConta}).
 */
public record ContaResponse(
    Long idConta, String nome, String tipo, BigDecimal saldoInicial, BigDecimal saldoAtual,
    Long usuarioId) {}
