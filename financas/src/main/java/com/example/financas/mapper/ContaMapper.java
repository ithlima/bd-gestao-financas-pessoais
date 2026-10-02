package com.example.financas.mapper;

import com.example.financas.dto.response.ContaResponse;
import com.example.financas.entity.Conta;

import java.math.BigDecimal;

public final class ContaMapper {

  private ContaMapper() {

  }

  public static ContaResponse toResponse(Conta conta, BigDecimal saldoAtual) {
    BigDecimal saldo = saldoAtual != null ? saldoAtual : conta.getSaldoInicial();
    return new ContaResponse(
        conta.getIdConta(),
        conta.getNome(),
        conta.getTipo(),
        conta.getSaldoInicial(),
        saldo,
        conta.getUsuario().getIdUsuario());
  }
}
