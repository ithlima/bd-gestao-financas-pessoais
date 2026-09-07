package com.example.financas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movimentacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movimentacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idMovimentacao;

  @Column(nullable = false, length = 255)
  private String descricao;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacao tipo;

  @Column(nullable = false)
  private LocalDate data;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimentacao_usuario"))
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimentacao_categoria"))
  private Categoria categoria;
}