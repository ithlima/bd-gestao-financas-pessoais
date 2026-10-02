package com.example.financas.controller;

import com.example.financas.config.RespostasDeErro;
import com.example.financas.dto.request.CategoriaRequest;
import com.example.financas.dto.response.CategoriaResponse;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de categorias.
 *
 * <p>O filtro opcional {@code ?tipo=RECEITA|DESPESA} é útil para montar o
 * formulário de cadastro: ao escolher "Despesa", o cliente pede apenas as
 * categorias de despesa e evita que o usuário selecione uma categoria
 * incompatível — antecipando no front-end a validação que o service faz (RN03,
 * RN15).
 */
@Tag(
    name = "Categorias",
    description = "Como o usuário classifica. É a categoria que forma as **linhas da DRE**.")
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  @Operation(
      summary = "Lista as categorias de um usuário",
      description =
          """
          Lista as categorias do usuário, opcionalmente filtradas por tipo.

          O filtro `tipo` evita que o cliente ofereça uma categoria de despesa para um
          lançamento de receita — validação que o service também faz, mas que é melhor
          antecipar na interface.
          """)
  @GetMapping
  public List<CategoriaResponse> listar(
      @RequestParam Long usuarioId, @RequestParam(required = false) TipoMovimentacao tipo) {
    return categoriaService.listar(usuarioId, tipo);
  }

  @Operation(summary = "Busca uma categoria por id", description = "Responde 404 se não existir.")
  @GetMapping("/{id}")
  public CategoriaResponse buscarPorId(@PathVariable Long id) {
    return categoriaService.buscarPorId(id);
  }

  @Operation(
      summary = "Cadastra uma categoria",
      description =
          """
          Cria uma categoria para o usuário. O nome deve ser único **por usuário** — dois
          usuários podem ter cada um a sua "Moradia".

          **Erros:** 400 para campo inválido; 404 se o usuário não existir; 409 se já houver
          uma categoria com o mesmo nome para o mesmo usuário.
          """)
  @PostMapping
  public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(request));
  }

  @Operation(
      summary = "Atualiza uma categoria",
      description =
          """
          Altera nome e tipo. O novo nome precisa continuar único para o usuário.

          **Atenção:** mudar o tipo de uma categoria já usada por lançamentos pode deixá-la
          incoerente com eles. Verifique os lançamentos existentes antes.

          **Erros:** 400, 404 ou 409 nas mesmas condições do cadastro.
          """)
  @PutMapping("/{id}")
  public CategoriaResponse atualizar(
      @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
    return categoriaService.atualizar(id, request);
  }

  @Operation(
      summary = "Remove uma categoria",
      description =
          """
          Exclui a categoria **somente se nenhum título ou movimentação a estiver usando**.

          A categoria é o que forma as linhas da DRE; apagar uma em uso deixaria lançamentos
          sem classificação.

          **Erros:** 404 se o id não existir; 409 se houver títulos ou movimentações
          vinculados, com a contagem na mensagem.
          """)
  @RespostasDeErro.Conflito
  @RespostasDeErro.NaoEncontrado
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@PathVariable Long id) {
    categoriaService.remover(id);
    return ResponseEntity.noContent().build();
  }
}
