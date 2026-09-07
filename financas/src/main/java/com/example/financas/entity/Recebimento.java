package com.example.financas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recebimento {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idRecebimento;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal valor;

  @Column(nullable = false)
  private LocalDate dataRecebimento;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "movimentacao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recebimento_movimentacao"))
  private Movimentacao movimentacao;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recebimento_conta"))
  private Conta conta;
}