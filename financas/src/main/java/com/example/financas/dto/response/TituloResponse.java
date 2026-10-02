package com.example.financas.dto.response;

import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.SituacaoTituloEfetiva;
import com.example.financas.entity.TipoMovimentacao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representação pública de um <b>título</b>.
 *
 * <p>Alguns campos merecem explicação, porque são o coração da regra de negócio:
 *
 * <ul>
 *   <li>{@code valorRealizado} — soma das movimentações vinculadas. Não é uma
 *       coluna gravada; vem de {@code SUM(movimentacao.valor)}. Uma fonte única
 *       de verdade, que não pode divergir.</li>
 *   <li>{@code valorEmAberto} — {@code valorPrevisto − valorRealizado}. É o
 *       quanto ainda falta para quitar o título.</li>
 *   <li>{@code situacao} — a situação <b>armazenada</b> (PENDENTE, PAGO ou
 *       CANCELADO). Reflete decisões, não a passagem do tempo.</li>
 *   <li>{@code situacaoEfetiva} — a situação <b>calculada</b>, que acrescenta
 *       VENCIDO quando o título está pendente e o vencimento já passou. É o
 *       valor que o front-end deve exibir.</li>
 *   <li>{@code podeRegistrarPagamento} — indica se a operação de quitação é
 *       permitida agora. {@code false} para título já quitado ou cancelado.</li>
 * </ul>
 *
 * <p>Expor as duas situações (armazenada e efetiva) é proposital: deixa visível
 * para quem estuda o sistema que "vencido" é um estado derivado e não uma coluna
 * do banco.
 */
public record TituloResponse(
    Long idTitulo,
    String descricao,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal valorEmAberto,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataVencimento,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate dataPagamento,
    TipoMovimentacao tipo,
    SituacaoTitulo situacao,
    SituacaoTituloEfetiva situacaoEfetiva,
    boolean podeRegistrarPagamento,
    int quantidadeMovimentacoes,
    String observacao,
    Long categoriaId,
    String categoriaNome,
    Long usuarioId) {}
