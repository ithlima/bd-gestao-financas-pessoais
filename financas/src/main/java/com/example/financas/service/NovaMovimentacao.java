package com.example.financas.service;

import com.example.financas.entity.Conta;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NovaMovimentacao(Titulo titulo, BigDecimal valor, LocalDate data, Conta conta) {

  public TipoMovimentacao tipo() {
    return titulo.getTipo();
  }
}
