package com.example.financas.service;

import com.example.financas.dto.request.CategoriaRequest;
import com.example.financas.dto.response.CategoriaResponse;
import com.example.financas.entity.Categoria;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoDuplicadoException;
import com.example.financas.exception.RecursoEmUsoException;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.mapper.CategoriaMapper;
import com.example.financas.repository.CategoriaRepository;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.TituloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;
  private final TituloRepository tituloRepository;
  private final MovimentacaoRepository movimentacaoRepository;
  private final UsuarioService usuarioService;

  public CategoriaService(
      CategoriaRepository categoriaRepository,
      TituloRepository tituloRepository,
      MovimentacaoRepository movimentacaoRepository,
      UsuarioService usuarioService) {
    this.categoriaRepository = categoriaRepository;
    this.tituloRepository = tituloRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.usuarioService = usuarioService;
  }

  @Transactional(readOnly = true)
  public List<CategoriaResponse> listar(Long usuarioId, TipoMovimentacao tipo) {
    List<Categoria> categorias =
        tipo == null
            ? categoriaRepository.findByUsuarioIdUsuario(usuarioId)
            : categoriaRepository.findByUsuarioIdUsuario(usuarioId).stream()
                .filter(c -> c.getTipo() == tipo)
                .toList();

    return categorias.stream().map(CategoriaMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public CategoriaResponse buscarPorId(Long id) {
    return CategoriaMapper.toResponse(buscarEntidade(id));
  }

  @Transactional
  public CategoriaResponse criar(CategoriaRequest request) {
    if (categoriaRepository.existsByNomeAndUsuarioIdUsuario(request.nome(), request.usuarioId())) {
      throw new RecursoDuplicadoException(
          "Já existe a categoria '" + request.nome() + "' para este usuário");
    }

    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());

    Categoria categoria = new Categoria();
    categoria.setNome(request.nome());
    categoria.setTipo(request.tipo());
    categoria.setUsuario(usuario);

    return CategoriaMapper.toResponse(categoriaRepository.save(categoria));
  }

  @Transactional
  public CategoriaResponse atualizar(Long id, CategoriaRequest request, Long usuarioId) {
    Categoria categoria = buscarEntidade(id);

    if (!categoria.getNome().equals(request.nome())
        && categoriaRepository.existsByNomeAndUsuarioIdUsuario(
            request.nome(), request.usuarioId())) {
      throw new RecursoDuplicadoException(
          "Já existe a categoria '" + request.nome() + "' para este usuário");
    }

    categoria.setNome(request.nome());
    categoria.setTipo(request.tipo());

    return CategoriaMapper.toResponse(categoriaRepository.save(categoria));
  }

  @Transactional
  public void remover(Long id, Long usuarioId) {
    Categoria categoria = buscarEntidade(id);

    long titulos = tituloRepository.countByCategoriaIdCategoria(id);
    if (titulos > 0) {
      throw RecursoEmUsoException.porDependencia("categoria", "categorias", titulos, "título", "títulos", false);
    }

    long movimentacoes = movimentacaoRepository.countByCategoriaIdCategoria(id);
    if (movimentacoes > 0) {
      throw RecursoEmUsoException.porDependencia("categoria", "categorias", movimentacoes, "movimentação", "movimentações", true);
    }

    categoriaRepository.delete(categoria);
  }

  public Categoria buscarEntidade(Long id) {
    return categoriaRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Categoria", id, true));
  }

  public Categoria validarCategoriaDoLancamento(
      Long categoriaId, Long usuarioId, TipoMovimentacao tipo, String origem) {

    Categoria categoria = buscarEntidade(categoriaId);

    if (!categoria.getUsuario().getIdUsuario().equals(usuarioId)) {
      throw new RegraNegocioException("A categoria informada pertence a outro usuário");
    }

    if (categoria.getTipo() != tipo) {
      throw new RegraNegocioException(
          "A categoria '"
              + categoria.getNome()
              + "' é do tipo "
              + categoria.getTipo()
              + " e não pode ser usada em "
              + origem
              + " de tipo "
              + tipo);
    }

    return categoria;
  }
}
