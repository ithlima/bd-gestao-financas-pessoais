package com.example.financas.controller;

import com.example.financas.config.RespostasDeErro;
import com.example.financas.dto.request.UsuarioRequest;
import com.example.financas.dto.response.UsuarioResponse;
import com.example.financas.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Usuários", description = "Cadastro dos donos dos dados financeiros.")
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @Operation(
      summary = "Lista os usuários",
      description = "Devolve todos os usuários cadastrados. A senha nunca é incluída na resposta.")
  @GetMapping
  public List<UsuarioResponse> listar() {
    return usuarioService.listar();
  }

  @Operation(summary = "Busca um usuário por id", description = "Responde 404 se o id não existir.")
  @GetMapping("/{id}")
  public UsuarioResponse buscarPorId(@PathVariable Long id) {
    return usuarioService.buscarPorId(id);
  }

  @Operation(
      summary = "Cadastra um usuário",
      description =
          """
          Cria um novo usuário. O e-mail deve ser único.

          **Erros:** 400 se um campo for inválido (inclusive e-mail com formato inválido ou
          senha com menos de 6 caracteres); 409 se o e-mail já estiver cadastrado.
          """)
  @PostMapping
  public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request) {
    UsuarioResponse criado = usuarioService.criar(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(criado);
  }

  @Operation(
      summary = "Atualiza um usuário",
      description =
          """
          Altera os dados do usuário. Se o e-mail for alterado, ele também precisa ser único.

          **Erros:** 400 para campo inválido; 404 se o id não existir; 409 se o novo e-mail já
          pertencer a outro usuário.
          """)
  @PutMapping("/{id}")
  public UsuarioResponse atualizar(
      @PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
    return usuarioService.atualizar(id, request);
  }

  @Operation(
      summary = "Remove um usuário",
      description =
          """
          Exclui o usuário **somente se não houver nada vinculado a ele** — títulos,
          movimentações, contas ou categorias.

          **Erros:** 404 se o id não existir; 409 se existirem registros dependentes, com a
          mensagem informando quantos e de que tipo.
          """)
  @RespostasDeErro.Conflito
  @RespostasDeErro.NaoEncontrado
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remover(@PathVariable Long id) {
    usuarioService.remover(id);
    return ResponseEntity.noContent().build();
  }
}
