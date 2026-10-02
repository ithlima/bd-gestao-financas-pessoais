package com.example.financas.dto.request;

import com.example.financas.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada para cadastrar ou atualizar um <b>título</b> — o compromisso
 * financeiro previsto.
 *
 * <p>Repare no que <b>não</b> existe aqui: não há valor pago, data de pagamento
 * nem conta. Cadastrar um título não afirma que o dinheiro se moveu. Para isso
 * existem operações próprias — {@code POST /titulos/{id}/pagar} e
 * {@code POST /titulos/{id}/receber} —, que são as únicas capazes de criar a
 * movimentação correspondente.
 *
 * <p>Exemplo de uso:
 *
 * <pre>
 * POST /api/v1/titulos
 * {
 *   "descricao": "Conta de energia",
 *   "valorPrevisto": 150.00,
 *   "dataVencimento": "2026-03-10",
 *   "tipo": "DESPESA",
 *   "categoriaId": 5,
 *   "usuarioId": 1
 * }
 * </pre>
 */
public record TituloRequest(
    @NotBlank(message = "A descrição do título é obrigatória")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,
    @NotNull(message = "O valor previsto é obrigatório")
        @Positive(message = "O valor previsto deve ser maior que zero")
        BigDecimal valorPrevisto,
    @NotNull(message = "A data de vencimento é obrigatória") LocalDate dataVencimento,
    @NotNull(message = "O tipo do título é obrigatório") TipoMovimentacao tipo,
    @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
        String observacao,
    @NotNull(message = "A categoria é obrigatória") Long categoriaId,
    @NotNull(message = "O usuário é obrigatório") Long usuarioId) {}
