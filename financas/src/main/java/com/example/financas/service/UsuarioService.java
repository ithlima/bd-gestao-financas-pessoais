package com.example.financas.service;

import com.example.financas.dto.request.UsuarioRequest;
import com.example.financas.dto.response.UsuarioResponse;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoDuplicadoException;
import com.example.financas.exception.RecursoEmUsoException;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.mapper.UsuarioMapper;
import com.example.financas.repository.CategoriaRepository;
import com.example.financas.repository.ContaRepository;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.TituloRepository;
import com.example.financas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negócio de usuário.
 *
 * <p>Esta classe concentra a única regra que existia documentada na modelagem
 * original e que <b>não estava implementada</b>: o e-mail precisa ser único.
 * A restrição {@code UNIQUE} do banco é a última linha de defesa; aqui a
 * validação acontece antes, para devolver uma mensagem útil em vez de um erro
 * de banco.
 *
 * <p>Além dos dados do próprio usuário, este service precisa consultar títulos,
 * movimentações, contas e categorias por um motivo específico: explicar por que
 * um usuário não pode ser excluído, informando <b>quantos</b> registros dependem
 * dele.
 */
@Service
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;
  private final TituloRepository tituloRepository;
  private final MovimentacaoRepository movimentacaoRepository;
  private final ContaRepository contaRepository;
  private final CategoriaRepository categoriaRepository;

  /** Injeção por construtor: dependência obrigatória, testável sem reflexão. */
  public UsuarioService(
      UsuarioRepository usuarioRepository,
      TituloRepository tituloRepository,
      MovimentacaoRepository movimentacaoRepository,
      ContaRepository contaRepository,
      CategoriaRepository categoriaRepository) {
    this.usuarioRepository = usuarioRepository;
    this.tituloRepository = tituloRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.contaRepository = contaRepository;
    this.categoriaRepository = categoriaRepository;
  }

  @Transactional(readOnly = true)
  public List<UsuarioResponse> listar() {
    return usuarioRepository.findAll().stream().map(UsuarioMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public UsuarioResponse buscarPorId(Long id) {
    return UsuarioMapper.toResponse(buscarEntidade(id));
  }

  @Transactional
  public UsuarioResponse criar(UsuarioRequest request) {
    if (usuarioRepository.existsByEmail(request.email())) {
      throw new RecursoDuplicadoException("Já existe um usuário com o e-mail " + request.email());
    }

    Usuario usuario = new Usuario();
    usuario.setNome(request.nome());
    usuario.setEmail(request.email());
    usuario.setSenha(request.senha());
    usuario.setAtivo(request.ativoOuPadrao());

    return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
  }

  @Transactional
  public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
    Usuario usuario = buscarEntidade(id);

    // Se o e-mail mudou, precisa ser único entre os demais usuários.
    if (!usuario.getEmail().equals(request.email())
        && usuarioRepository.existsByEmail(request.email())) {
      throw new RecursoDuplicadoException("Já existe um usuário com o e-mail " + request.email());
    }

    usuario.setNome(request.nome());
    usuario.setEmail(request.email());
    usuario.setSenha(request.senha());
    usuario.setAtivo(request.ativoOuPadrao());

    return UsuarioMapper.toResponse(usuarioRepository.save(usuario));
  }

  /**
   * Remove um usuário, se não houver nada vinculado a ele.
   *
   * <p>A verificação existe para dar uma mensagem útil. O banco recusaria de
   * qualquer forma, pelas chaves estrangeiras — o que é a garantia final —, mas a
   * mensagem dele citaria o SQL executado, sem dizer ao usuário o que fazer.
   * Aqui se informa <b>quantos</b> registros dependem dele.
   */
  @Transactional
  public void remover(Long id) {
    Usuario usuario = buscarEntidade(id);

    long titulos = tituloRepository.countByUsuarioIdUsuario(id);
    if (titulos > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", titulos, "título", "títulos", false);
    }

    long movimentacoes = movimentacaoRepository.countByUsuarioIdUsuario(id);
    if (movimentacoes > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", movimentacoes, "movimentação", "movimentações", true);
    }

    long contas = contaRepository.findByUsuarioIdUsuario(id).size();
    if (contas > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", contas, "conta", "contas", true);
    }

    long categorias = categoriaRepository.findByUsuarioIdUsuario(id).size();
    if (categorias > 0) {
      throw RecursoEmUsoException.porDependencia("usuário", "usuários", categorias, "categoria", "categorias", true);
    }

    usuarioRepository.delete(usuario);
  }

  /**
   * Carrega a entidade ou falha com 404.
   *
   * <p>Método público porque os outros services precisam resolver um usuário a
   * partir do id recebido no request, e reaproveitar esta busca garante que
   * todos produzam a mesma mensagem de erro.
   */
  public Usuario buscarEntidade(Long id) {
    return usuarioRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Usuário", id, false));
  }
}
