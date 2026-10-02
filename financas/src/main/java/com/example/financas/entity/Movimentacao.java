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
    name = "movimentacao",
    indexes = {
      @Index(name = "idx_movimentacao_usuario_data", columnList = "usuario_id, data"),
      @Index(name = "idx_movimentacao_conta", columnList = "conta_id"),
      @Index(name = "idx_movimentacao_titulo", columnList = "titulo_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movimentacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idMovimentacao;

  @NotBlank(message = "A descrição da movimentação é obrigatória")
  @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
  @Column(nullable = false, length = 255)
  private String descricao;

  @NotNull(message = "O valor é obrigatório")
  @Positive(message = "O valor da movimentação deve ser maior que zero")
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @NotNull(message = "O tipo da movimentação é obrigatório")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @NotNull(message = "A data da movimentação é obrigatória")
  @Column(nullable = false)
  private LocalDate data;

  @NotNull(message = "O usuário é obrigatório")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "usuario_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_usuario"))
  private Usuario usuario;

  @NotNull(message = "A categoria é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "categoria_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_categoria"))
  private Categoria categoria;

  @NotNull(message = "A conta é obrigatória")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "conta_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_movimentacao_conta"))
  private Conta conta;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "titulo_id", foreignKey = @ForeignKey(name = "fk_movimentacao_titulo"))
  private Titulo titulo;
}
