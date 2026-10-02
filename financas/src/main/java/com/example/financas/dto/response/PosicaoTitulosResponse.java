package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PosicaoTitulosResponse(
    Long usuarioId,
    TipoMovimentacao tipo,
    LocalDate dataReferencia,

    List<TituloEmAbertoResponse> titulos,

    BigDecimal totalEmAberto,

    BigDecimal totalVencido,

    BigDecimal totalAVencerProximosDias,

    int quantidadeTitulos,
    int quantidadeVencidos,
    int quantidadeParciais,
    int quantidadeAVencerProximosDias,

    List<TituloEmAbertoResponse> vencidos,
    List<TituloEmAbertoResponse> aVencerProximosDias,

    int janelaDias) {}
