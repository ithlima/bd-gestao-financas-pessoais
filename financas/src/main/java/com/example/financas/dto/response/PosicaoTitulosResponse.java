package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Posição de <b>contas a pagar</b> ou de <b>contas a receber</b> em uma data de
 * referência.
 *
 * <p>É a resposta que alimenta a tela de "o que eu tenho para pagar" e "o que eu
 * tenho para receber". Responde a três perguntas de uma vez:
 *
 * <ol>
 *   <li><b>Quanto eu devo / tenho a receber no total?</b>
 *       ({@code totalEmAberto})</li>
 *   <li><b>Quanto disso já está atrasado?</b>
 *       ({@code totalVencido}, {@code quantidadeVencidos})</li>
 *   <li><b>O que vence nos próximos dias?</b>
 *       ({@code totalAVencerProximosDias} e a lista {@code aVencerProximosDias})</li>
 * </ol>
 *
 * <p><b>O que significa "em aberto".</b> São os títulos com
 * {@code situacao = PENDENTE} e valor ainda não quitado — ou seja, exclui o que já
 * foi pago (nada mais é devido) e o que foi cancelado (nunca foi obrigação). Um
 * título parcialmente pago entra pelo que <b>falta</b>, não pelo valor original.
 *
 * <p><b>Por que as listas vêm separadas.</b> {@code vencidos} e
 * {@code aVencerProximosDias} são recortes de {@code titulos}, já ordenados por
 * urgência. Devolvê-los prontos evita que o cliente precise reimplementar a mesma
 * regra de corte — e evita que cada cliente use uma data de referência diferente.
 */
public record PosicaoTitulosResponse(
    Long usuarioId,
    TipoMovimentacao tipo,
    LocalDate dataReferencia,

    /** Todos os títulos com valor em aberto, ordenados por vencimento. */
    List<TituloEmAbertoResponse> titulos,

    // --- Totais ------------------------------------------------------
    /** Soma do que falta pagar/receber. É o número principal desta resposta. */
    BigDecimal totalEmAberto,
    /** Soma apenas do que já venceu. */
    BigDecimal totalVencido,
    /** Soma do que vence dentro da janela de dias configurada. */
    BigDecimal totalAVencerProximosDias,

    // --- Quantidades -------------------------------------------------
    int quantidadeTitulos,
    int quantidadeVencidos,
    int quantidadeParciais,
    int quantidadeAVencerProximosDias,

    // --- Recortes prontos, ordenados por data de vencimento ----------
    List<TituloEmAbertoResponse> vencidos,
    List<TituloEmAbertoResponse> aVencerProximosDias,

    /**
     * Quantos dias à frente a janela de "próximos dias" considerou.
     *
     * <p>A janela é <b>inclusiva</b>: com {@code janelaDias = 30} e data de
     * referência 15/03, entram os títulos que vencem entre 15/03 e 14/04.
     */
    int janelaDias) {}
