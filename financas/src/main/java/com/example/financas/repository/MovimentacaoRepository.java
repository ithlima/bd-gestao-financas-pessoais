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

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

  List<Movimentacao> findByUsuario(Usuario usuario);

  List<Movimentacao> findByUsuarioIdUsuario(Long usuarioId);

  long countByUsuarioIdUsuario(Long usuarioId);

  long countByCategoriaIdCategoria(Long categoriaId);

  long countByContaIdConta(Long contaId);

  List<Movimentacao> findByUsuarioIdUsuarioAndTipo(Long usuarioId, TipoMovimentacao tipo);

  List<Movimentacao> findByUsuarioIdUsuarioAndDataBetween(
      Long usuarioId, LocalDate dataInicio, LocalDate dataFim);

  List<Movimentacao> findByUsuarioIdUsuarioAndCategoriaIdCategoria(
      Long usuarioId, Long categoriaId);

  List<Movimentacao> findByTituloIdTitulo(Long tituloId);

  List<Movimentacao> findByContaIdConta(Long contaId);

  List<Movimentacao> findByUsuarioIdUsuarioAndDataBetweenOrderByDataAsc(
      Long usuarioId, LocalDate dataInicio, LocalDate dataFim);

  List<Movimentacao> findByUsuarioIdUsuarioAndTituloIsNull(Long usuarioId);

  @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimentacao m WHERE m.titulo.idTitulo = :tituloId")
  BigDecimal somarRealizadoDoTitulo(@Param("tituloId") Long tituloId);

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
             AND m.is_transferencia = false
           GROUP BY m.categoria_id, c.nome, m.tipo
           ORDER BY c.nome
          """,
      nativeQuery = true)
  List<DreRealizadoProjection> somarRealizadoPorCategoria(
      @Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

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
