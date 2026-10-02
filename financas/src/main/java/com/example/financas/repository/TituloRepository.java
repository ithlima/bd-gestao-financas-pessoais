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

/**
 * Acesso aos títulos — os compromissos financeiros previstos.
 *
 * <p>Além do CRUD herdado de {@link JpaRepository}, este repositório fornece as
 * agregações que alimentam a DRE. A ideia é deixar o banco fazer a soma
 * ({@code SUM ... GROUP BY}), em vez de trazer milhares de títulos para a
 * memória da aplicação apenas para somá-los em Java.
 */
public interface TituloRepository extends JpaRepository<Titulo, Long> {

  // ------------------------------------------------------------------
  // Consultas de apoio ao CRUD
  // ------------------------------------------------------------------

  List<Titulo> findByUsuario(Usuario usuario);

  List<Titulo> findByUsuarioIdUsuario(Long usuarioId);

  /** Quantidade de títulos de um usuário — usada para explicar bloqueios de exclusão. */
  long countByUsuarioIdUsuario(Long usuarioId);

  /** Quantidade de títulos de uma categoria — usada para explicar bloqueios de exclusão. */
  long countByCategoriaIdCategoria(Long categoriaId);

  List<Titulo> findByUsuarioIdUsuarioAndTipo(Long usuarioId, TipoMovimentacao tipo);

  List<Titulo> findByUsuarioIdUsuarioAndSituacao(Long usuarioId, SituacaoTitulo situacao);

  /**
   * Títulos do usuário com vencimento dentro de um período, em ordem de
   * vencimento. Base da DRE prevista.
   */
  List<Titulo> findByUsuarioIdUsuarioAndDataVencimentoBetweenOrderByDataVencimentoAsc(
      Long usuarioId, LocalDate inicio, LocalDate fim);

  /**
   * Títulos pendentes já vencidos (vencimento anterior a uma data).
   *
   * <p>Note que o filtro de situação é {@code PENDENTE}: um título cancelado
   * não é um atraso, e um título já quitado também não. A situação "VENCIDO" em
   * si é derivada em memória (ver {@code Titulo#getSituacaoEfetiva}), porque o
   * banco não guarda esse estado.
   */
  List<Titulo> findByUsuarioIdUsuarioAndSituacaoAndDataVencimentoBeforeOrderByDataVencimentoAsc(
      Long usuarioId, SituacaoTitulo situacao, LocalDate referencia);

  /**
   * Marca um título como PAGO, registrando a data da quitação.
   *
   * <p>É uma atualização direta no banco, e não um {@code save} da entidade. Isso
   * é intencional: permite que o {@code MovimentacaoService} altere a situação do
   * título sem precisar injetar o {@code TituloService}, o que criaria um ciclo
   * de beans. A regra de "quando" quitar continua na camada de serviço; aqui mora
   * apenas o "como" gravar.
   */
  @Modifying
  @Query(
      "UPDATE Titulo t SET t.situacao = com.example.financas.entity.SituacaoTitulo.PAGO, "
          + "t.dataPagamento = :dataPagamento WHERE t.idTitulo = :idTitulo")
  int marcarComoPago(
      @Param("idTitulo") Long idTitulo, @Param("dataPagamento") LocalDate dataPagamento);

  // ------------------------------------------------------------------
  // Agregação para a DRE prevista
  // ------------------------------------------------------------------

  /**
   * Soma dos valores PREVISTOS, agrupada por categoria e tipo, no período de
   * vencimento (regime de competência).
   *
   * <p>É escrito em SQL nativo para deixar explícito o filtro
   * {@code situacao <> 'CANCELADO'}: títulos cancelados ficam de fora, porque o
   * compromisso deixou de existir e não deve aparecer nem como previsão.
   *
   * <p>Atenção ao que <b>não</b> é filtrado: títulos pendentes entram mesmo que
   * estejam vencidos. Eles eram, sim, uma obrigação prevista do período — o
   * atraso é sinalizado à parte, no bloco {@code titulosVencidos} da DRE.
   */
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
}
