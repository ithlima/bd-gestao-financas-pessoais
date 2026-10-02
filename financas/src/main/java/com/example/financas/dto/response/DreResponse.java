package com.example.financas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DreResponse(
    Long usuarioId,
    String modo,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate inicio,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate fim,

    List<DreLinhaResponse> linhasReceitas,
    BigDecimal totalReceitasPrevistas,
    BigDecimal totalReceitasRealizadas,

    List<DreLinhaResponse> linhasDespesas,
    BigDecimal totalDespesasPrevistas,
    BigDecimal totalDespesasRealizadas,

    BigDecimal resultadoPrevisto,
    BigDecimal resultadoRealizado,
    BigDecimal variacaoResultado,
    boolean variacaoFavoravel,

    List<DreTituloVencidoResponse> titulosVencidos,
    BigDecimal totalVencido) {}
