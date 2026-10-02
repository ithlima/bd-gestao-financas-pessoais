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

  @NotBlank(message = "A descrição do título é obrigatória")
  @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
  @Column(nullable = false, length = 255)
  private String descricao;

  @NotNull(message = "O valor previsto é obrigatório")
  @Positive(message = "O valor previsto deve ser maior que zero")
  @Column(name = "valor_previsto", nullable = false, precision = 15, scale = 2)
  private BigDecimal valorPrevisto;

  @NotNull(message = "A data de vencimento é obrigatória")
  @Column(name = "data_vencimento", nullable = false)
  private LocalDate dataVencimento;

  @Column(name = "data_pagamento")
  private LocalDate dataPagamento;

  @NotNull(message = "O tipo do título é obrigatório")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private SituacaoTitulo situacao = SituacaoTitulo.PENDENTE;

  @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
  @Column(length = 255)
  private String observacao;

  @NotNull(message = "A categoria é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "categoria_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_titulo_categoria"))
  private Categoria categoria;

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

  public SituacaoTituloEfetiva getSituacaoEfetiva() {
    return getSituacaoEfetiva(LocalDate.now());
  }

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

  public boolean isAbertoParaQuitacao() {
    return situacao == SituacaoTitulo.PENDENTE;
  }

  public void quitar(LocalDate dataDoPagamento) {
    this.situacao = SituacaoTitulo.PAGO;
    this.dataPagamento = dataDoPagamento;
  }
}
