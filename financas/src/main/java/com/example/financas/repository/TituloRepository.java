package com.example.financas.repository;

import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.entity.Usuario;
import com.example.financas.repository.projection.DrePrevisaoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TituloRepository extends JpaRepository<Titulo, Long> {

  List<Titulo> findByUsuario(Usuario usuario);

  List<Titulo> findByUsuarioIdUsuario(Long usuarioId);

  long countByUsuarioIdUsuario(Long usuarioId);

  long countByCategoriaIdCategoria(Long categoriaId);

  List<Titulo> findByUsuarioIdUsuarioAndTipo(Long usuarioId, TipoMovimentacao tipo);

  List<Titulo> findByUsuarioIdUsuarioAndSituacao(Long usuarioId, SituacaoTitulo situacao);

  List<Titulo> findByUsuarioIdUsuarioAndDataVencimentoBetweenOrderByDataVencimentoAsc(
      Long usuarioId, LocalDate inicio, LocalDate fim);

  List<Titulo> findByUsuarioIdUsuarioAndSituacaoAndDataVencimentoBeforeOrderByDataVencimentoAsc(
      Long usuarioId, SituacaoTitulo situacao, LocalDate referencia);

  @Modifying
  @Query(
      "UPDATE Titulo t SET t.situacao = com.example.financas.entity.SituacaoTitulo.PAGO, "
          + "t.dataPagamento = :dataPagamento WHERE t.idTitulo = :idTitulo")
  int marcarComoPago(
      @Param("idTitulo") Long idTitulo, @Param("dataPagamento") LocalDate dataPagamento);

  @Query(
      value =
          """
          SELECT t.categoria_id          AS categoriaId,
                 c.nome                  AS categoriaNome,
                 t.tipo                  AS tipo,
                 SUM(t.valor_previsto)   AS totalPrevisto
            FROM titulo t
            JOIN categoria c ON c.id_categoria = t.categoria_id
           WHERE t.usuario_id = :usuarioId
             AND t.situacao <> 'CANCELADO'
             AND t.data_vencimento BETWEEN :inicio AND :fim
           GROUP BY t.categoria_id, c.nome, t.tipo
           ORDER BY c.nome
          """,
      nativeQuery = true)
  List<DrePrevisaoProjection> somarPrevistoPorCategoria(
      @Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT t FROM Titulo t WHERE t.idTitulo = :id")
  java.util.Optional<Titulo> findByIdWithLock(@Param("id") Long id);
}
