package com.example.financas.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resultado completo de uma apuração de DRE para finanças pessoais.
 *
 * <p><b>Convenção de sinais (importante).</b> As listas
 * {@code linhasReceitas} e {@code linhasDespesas} guardam <b>magnitudes
 * positivas</b> — o sinal é dado pelo fato de a linha estar em um bloco ou no
 * outro. Já os campos {@code variacao*} são <b>assinados</b>, com o sinal
 * normalizado de modo que <b>valor positivo = favorável ao usuário</b>:
 *
 * <ul>
 *   <li>receita recebida acima do previsto → variação positiva;</li>
 *   <li>despesa gasta abaixo do previsto → variação positiva;</li>
 *   <li>despesa gasta acima do previsto → variação negativa.</li>
 * </ul>
 *
 * Assim o usuário não precisa interpretar "negativo é bom" linha a linha.
 *
 * <p><b>Estrutura, espelhando uma DRE pessoal:</b>
 *
 * <pre>
 *   RECEITAS ....................... linhasReceitas + totais
 *   (-) DESPESAS ................... linhasDespesas + totais
 *   (=) RESULTADO DO PERÍODO ....... resultadoPrevisto / resultadoRealizado
 *       Títulos vencidos ........... titulosVencidos (não alteram o resultado)
 * </pre>
 */
public record DreResponse(
    Long usuarioId,
    String modo,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate inicio,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate fim,

    // --- Receitas, por categoria -------------------------------------
    List<DreLinhaResponse> linhasReceitas,
    BigDecimal totalReceitasPrevistas,
    BigDecimal totalReceitasRealizadas,

    // --- Despesas, por categoria -------------------------------------
    List<DreLinhaResponse> linhasDespesas,
    BigDecimal totalDespesasPrevistas,
    BigDecimal totalDespesasRealizadas,

    // --- Resultado do período ----------------------------------------
    BigDecimal resultadoPrevisto,
    BigDecimal resultadoRealizado,
    BigDecimal variacaoResultado,
    boolean variacaoFavoravel,

    // --- Sinalizações -------------------------------------------------
    List<DreTituloVencidoResponse> titulosVencidos,
    BigDecimal totalVencido) {}
