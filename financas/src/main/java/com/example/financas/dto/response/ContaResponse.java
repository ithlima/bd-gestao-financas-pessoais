package com.example.financas.dto.response;

import java.math.BigDecimal;

public record ContaResponse(
    Long idConta, String nome, String tipo, BigDecimal saldoInicial, BigDecimal saldoAtual,
    Long usuarioId) {}
