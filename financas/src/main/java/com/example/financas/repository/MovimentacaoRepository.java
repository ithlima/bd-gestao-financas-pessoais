package com.example.financas.repository;

import com.example.financas.entity.Movimentacao;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Usuario;
import com.example.financas.repository.projection.DreRealizadoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Acesso às movimentações — os eventos financeiros <b>realizados</b>.
 *
 * <p>Os métodos originais foram integralmente preservados. O que foi
 * acrescentado são consultas para os novos vínculos (conta e título) e as
 * agregações da DRE realizada.
 */
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

  // ------------------------------------------------------------------
  // Métodos originais — preservados
  // ------------------------------------------------------------------

  List<Movimentacao> findByUsuario(Usuario usuario);

  List<Movimentacao> findByUsuarioIdUsuario(Long usuarioId);

  /** Quantidade de movimentações de um usuário — para explicar bloqueios de exclusão. */
  long countByUsuarioIdUsuario(Long usuarioId);

  /** Quantidade de movimentações de uma categoria — para explicar bloqueios de exclusão. */
  long countByCategoriaIdCategoria(Long categoriaId);

  /** Quantidade de movimentações de uma conta — para explicar bloqueios de exclusão. */
  long countByContaIdConta(Long contaId);

  List<Movimentacao> findByUsuarioIdUsuarioAndTipo(Long usuarioId, TipoMovimentacao tipo);

  List<Movimentacao> findByUsuarioIdUsuarioAndDataBetween(
      Long usuarioId, LocalDate dataInicio, LocalDate dataFim);

  List<Movimentacao> findByUsuarioIdUsuarioAndCategoriaIdCategoria(
      Long usuarioId, Long categoriaId);

  // ------------------------------------------------------------------
  // Novos vínculos
  // ------------------------------------------------------------------

  /** Movimentações originadas de um título — base do valor realizado (RN09). */
  List<Movimentacao> findByTituloIdTitulo(Long tituloId);

  /** Extrato de uma conta. */
  List<Movimentacao> findByContaIdConta(Long contaId);

  /** Movimentações realizadas no período, ordenadas por data. */
  List<Movimentacao> findByUsuarioIdUsuarioAndDataBetweenOrderByDataAsc(
      Long usuarioId, LocalDate dataInicio, LocalDate dataFim);

  /** Movimentações avulsas (sem título de origem) — útil para conferência. */
  List<Movimentacao> findByUsuarioIdUsuarioAndTituloIsNull(Long usuarioId);

  // ------------------------------------------------------------------
  // Agregações para a DRE realizada e para o saldo
  // ------------------------------------------------------------------

  /**
   * Soma dos valores REALIZADOS de um título.
   *
   * <p>É este número, e não uma coluna gravada, que define se o título está
   * quitado (RN09, RN10). Assim existe uma única fonte de verdade para "quanto
   * já foi pago" e não há risco de a coluna divergir das movimentações.
   */
  @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m WHERE m.titulo.idTitulo = :tituloId")
  BigDecimal somarRealizadoDoTitulo(@Param("tituloId") Long tituloId);

  /**
   * Soma dos valores realizados, agrupada por categoria e tipo, no período em
   * que o dinheiro efetivamente se moveu (regime de caixa).
   *
   * <p>Esta consulta é a <b>única</b> fonte de receita e despesa da DRE
   * realizada. A tabela {@code titulo} não participa dela — é exatamente essa
   * ausência que impede uma conta não paga de ser contada como despesa.
   */
  @Query(
      value =
          """
          SELECT m.categoria_id        AS categoriaId,
                 c.nome                AS categoriaNome,
                 m.tipo                AS tipo,
                 SUM(m.valor)          AS totalRealizado
            FROM movimentacao m
            JOIN categoria c ON c.id_categoria = m.categoria_id
           WHERE m.usuario_id = :usuarioId
             AND m.data BETWEEN :inicio AND :fim
           GROUP BY m.categoria_id, c.nome, m.tipo
           ORDER BY c.nome
          """,
      nativeQuery = true)
  List<DreRealizadoProjection> somarRealizadoPorCategoria(
      @Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  /**
   * Saldo atual de cada conta do usuário:
   * {@code saldo_inicial + receitas realizadas - despesas realizadas}.
   *
   * <p>Como agora existe uma única tabela de eventos com {@code conta_id}
   * obrigatório, este cálculo é uma consulta só. Antes seria preciso um
   * {@code UNION} entre recebimento e pagamento, com dois totais separados e
   * duas chances de errar.
   *
   * <p>Retorna pares {@code [idConta (Long), saldoAtual (BigDecimal)]}.
   */
  @Query(
      value =
          """
          SELECT c.id_conta,
                 c.saldo_inicial
                   + COALESCE(SUM(CASE WHEN m.tipo = 'RECEITA'
                                       THEN m.valor ELSE -m.valor END), 0) AS saldo_atual
            FROM conta c
            LEFT JOIN movimentacao m ON m.conta_id = c.id_conta
           WHERE c.usuario_id = :usuarioId
           GROUP BY c.id_conta, c.saldo_inicial
           ORDER BY c.id_conta
          """,
      nativeQuery = true)
  List<Object[]> calcularSaldoPorConta(@Param("usuarioId") Long usuarioId);
}
