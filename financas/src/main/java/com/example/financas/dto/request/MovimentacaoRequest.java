package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada para registrar uma <b>movimentação avulsa</b> — um evento
 * financeiro que aconteceu sem ter sido previsto por um título.
 *
 * <p>Exemplo típico: "paguei um café em dinheiro". Não havia compromisso
 * cadastrado antes, então não faz sentido exigir um título. Este é o caso de uso
 * que garante que <b>nada do funcionamento anterior se perde</b>: o lançamento
 * direto de receitas e despesas continua existindo.
 *
 * <p>Duas diferenças em relação ao modelo antigo, ambas obrigatórias:
 *
 * <ul>
 *   <li>{@code contaId} agora é <b>exigido</b>: todo evento financeiro acontece
 *       em alguma conta. Antes isso vivia nas tabelas recebimento/pagamento.</li>
 *   <li>{@code valor} precisa ser <b>maior que zero</b>: o sinal vem de
 *       {@code tipo}, nunca de um valor negativo.</li>
 * </ul>
 */
public record MovimentacaoRequest(
    @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,
    @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal valor,
    @NotNull(message = "O tipo é obrigatório") TipoMovimentacao tipo,
    @NotNull(message = "A data é obrigatória") LocalDate data,
    @NotNull(message = "A categoria é obrigatória") Long categoriaId,
    @NotNull(message = "A conta é obrigatória") Long contaId,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
