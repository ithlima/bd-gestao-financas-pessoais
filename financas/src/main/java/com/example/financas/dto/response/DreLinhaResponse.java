package com.example.financas.dto.response;

import com.example.financas.entity.TipoMovimentacao;

import java.math.BigDecimal;

/**
 * Uma <b>linha</b> da DRE — corresponde a uma categoria do usuário.
 *
 * <p>Exemplos: "Salários" no bloco de receitas; "Moradia", "Alimentação" ou
 * "Transporte" no bloco de despesas.
 *
 * <p>Os três valores juntos respondem à pergunta central do relatório:
 *
 * <pre>
 *   valorPrevisto    -> quanto eu esperava (título, por vencimento)
 *   valorRealizado   -> quanto de fato aconteceu (movimentação, por data)
 *   variacao         -> realizado - previsto
 * </pre>
 *
 * <p><b>Como ler a variação.</b> {@code variacao > 0} é sempre favorável ao
 * usuário, porque o sinal é normalizado: em uma linha de receita, receber mais
 * que o previsto dá variação positiva; em uma linha de despesa, gastar menos que
 * o previsto também dá variação positiva. Isso é feito aplicando o sinal do tipo
 * aos dois lados da subtração, para que o usuário não precise interpretar
 * "negativo é bom" linha a linha.
 */
public record DreLinhaResponse(
    Long categoriaId,
    String categoriaNome,
    TipoMovimentacao tipo,
    BigDecimal valorPrevisto,
    BigDecimal valorRealizado,
    BigDecimal variacao) {}
