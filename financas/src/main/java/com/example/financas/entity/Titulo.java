package com.example.financas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TÍTULO — o compromisso financeiro previsto.
 *
 * <p><b>Conceito.</b> O título responde à pergunta
 * <i>"o que eu tenho a pagar ou a receber?"</i>. Ele representa uma
 * <b>promessa</b>: uma obrigação (despesa) ou um direito (receita) que ainda
 * pode não ter se concretizado.
 *
 * <p><b>Diferença para Movimentacao.</b> A {@link Movimentacao} responde à
 * pergunta <i>"o que aconteceu com o meu dinheiro?"</i> e representa um
 * <b>fato consumado</b>. Um título pode existir para sempre sem nunca gerar uma
 * movimentação (basta ser cancelado); uma movimentação, uma vez criada, é um
 * evento que já ocorreu.
 *
 * <pre>
 *   "Tenho uma conta de energia de R$ 150,00 que vence dia 10"
 *        -> TITULO    (valorPrevisto = 150, dataVencimento = 10, PENDENTE)
 *        -> NENHUMA movimentacao, NENHUMA despesa na DRE realizada
 *
 *   "Paguei a conta de energia de R$ 150,00 no dia 08 pela conta bancaria"
 *        -> TITULO    (situacao = PAGO, dataPagamento = 08)
 *        -> MOVIMENTACAO (valor = 150, data = 08, conta = conta bancaria)
 *        -> agora sim entra na DRE realizada
 * </pre>
 *
 * <p><b>Por que o título NÃO tem {@code conta}.</b> Um título ainda não pago
 * não está em banco nenhum. A conta só é conhecida no momento do pagamento e,
 * por isso, ela vive na {@link Movimentacao}. Colocar uma conta no título seria
 * afirmar que a obrigação já saiu de uma conta — exatamente o erro que a
 * separação entre previsão e realização corrige.
 *
 * <p><b>Por que o título NÃO guarda o valor realizado.</b> O valor já realizado
 * é sempre a soma das movimentações vinculadas. Guardá-lo em uma coluna criaria
 * duas fontes para o mesmo número, que podem divergir. Ele é calculado sob
 * demanda (ver {@code TituloService#valorRealizado}).
 */
@Entity
@Table(
    name = "titulo",
    indexes = {
      @Index(name = "idx_titulo_usuario_vencimento", columnList = "usuario_id, data_vencimento"),
      @Index(name = "idx_titulo_usuario_situacao", columnList = "usuario_id, situacao")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Titulo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idTitulo;

  /** Descrição legível, ex.: "Conta de energia". */
  @NotBlank(message = "A descrição do título é obrigatória")
  @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
  @Column(nullable = false, length = 255)
  private String descricao;

  /**
   * Quanto foi <b>previsto</b> pagar ou receber.
   *
   * <p>Regra RN01: deve ser maior que zero.
   */
  @NotNull(message = "O valor previsto é obrigatório")
  @Positive(message = "O valor previsto deve ser maior que zero")
  @Column(name = "valor_previsto", nullable = false, precision = 15, scale = 2)
  private BigDecimal valorPrevisto;

  /**
   * Quando o compromisso <b>deve</b> ser cumprido.
   *
   * <p>É a base de duas coisas: do cálculo de "vencido" (RN11) e da DRE
   * prevista por competência.
   */
  @NotNull(message = "A data de vencimento é obrigatória")
  @Column(name = "data_vencimento", nullable = false)
  private LocalDate dataVencimento;

  /**
   * Quando o compromisso foi <b>efetivamente</b> quitado.
   *
   * <p>Fica {@code null} enquanto o título não estiver completamente pago ou
   * recebido. É preenchida quando a soma das movimentações atinge o valor
   * previsto (RN10).
   */
  @Column(name = "data_pagamento")
  private LocalDate dataPagamento;

  /** RECEITA (a receber) ou DESPESA (a pagar). */
  @NotNull(message = "O tipo do título é obrigatório")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  /**
   * Situação armazenada. Nunca recebe VENCIDO — ver {@link SituacaoTitulo}.
   */
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SituacaoTitulo situacao = SituacaoTitulo.PENDENTE;

  /** Campo livre para anotações do usuário. */
  @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
  @Column(length = 255)
  private String observacao;

  /**
   * Classificação do título. É a categoria que define a <b>linha</b> da DRE
   * (Salários, Moradia, Alimentação, ...). Por isso é obrigatória.
   */
  @NotNull(message = "A categoria é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "categoria_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_titulo_categoria"))
  private Categoria categoria;

  /** Dono do título. Todas as operações validam este vínculo (RN12). */
  @NotNull(message = "O usuário é obrigatório")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "usuario_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_titulo_usuario"))
  private Usuario usuario;

  @PrePersist
  protected void prePersist() {
    if (situacao == null) {
      situacao = SituacaoTitulo.PENDENTE;
    }
  }

  // ------------------------------------------------------------------
  // Comportamento de domínio
  // ------------------------------------------------------------------

  /**
   * Situação efetiva, já considerando a passagem do tempo (RN11).
   *
   * <pre>
   *   CANCELADO, se situacao = CANCELADO
   *   PAGO,      se situacao = PAGO
   *   VENCIDO,   se situacao = PENDENTE e dataVencimento &lt; hoje
   *   PENDENTE,  caso contrário
   * </pre>
   */
  public SituacaoTituloEfetiva getSituacaoEfetiva() {
    return getSituacaoEfetiva(LocalDate.now());
  }

  /**
   * Mesma regra acima, mas com a data de referência injetada.
   *
   * <p>Existe para que a regra possa ser testada de forma determinística, sem
   * depender do relógio da máquina.
   */
  public SituacaoTituloEfetiva getSituacaoEfetiva(LocalDate referencia) {
    if (situacao == SituacaoTitulo.CANCELADO) {
      return SituacaoTituloEfetiva.CANCELADO;
    }
    if (situacao == SituacaoTitulo.PAGO) {
      return SituacaoTituloEfetiva.PAGO;
    }
    if (dataVencimento != null && dataVencimento.isBefore(referencia)) {
      return SituacaoTituloEfetiva.VENCIDO;
    }
    return SituacaoTituloEfetiva.PENDENTE;
  }

  /** Um título cancelado ou já quitado não aceita novos pagamentos (RN05, RN06). */
  public boolean isAbertoParaQuitacao() {
    return situacao == SituacaoTitulo.PENDENTE;
  }

  /**
   * Marca o título como quitado, registrando a data do último pagamento.
   * Chamado pelo service quando a soma das movimentações atinge o previsto.
   */
  public void quitar(LocalDate dataDoPagamento) {
    this.situacao = SituacaoTitulo.PAGO;
    this.dataPagamento = dataDoPagamento;
  }
}
