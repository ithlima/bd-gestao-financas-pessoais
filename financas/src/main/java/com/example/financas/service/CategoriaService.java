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

/**
 * Regras de negócio de categoria.
 *
 * <p>A categoria é a unidade de classificação que forma as <b>linhas da DRE</b>.
 * Por isso a regra principal aqui é o nome único por usuário: duas categorias
 * chamadas "Moradia" para o mesmo usuário produziriam duas linhas idênticas no
 * relatório, impossíveis de distinguir.
 */
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
  public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
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

  /**
   * Remove uma categoria, se nenhum título ou movimentação a estiver usando.
   *
   * <p>Apagar uma categoria em uso deixaria lançamentos sem classificação — e a
   * categoria é justamente o que forma as linhas da DRE.
   */
  @Transactional
  public void remover(Long id) {
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

  /**
   * Carrega a categoria e valida as duas regras que a DRE depende
   * (RN03 para títulos e RN15 para movimentações):
   *
   * <ol>
   *   <li>a categoria deve pertencer ao <b>mesmo usuário</b> do lançamento —
   *       impede que um usuário classifique algo com a categoria de outro;</li>
   *   <li>o <b>tipo</b> da categoria deve ser igual ao tipo do lançamento —
   *       impede classificar uma despesa como "Salários", o que faria a DRE
   *       somar valores no bloco errado.</li>
   * </ol>
   *
   * <p>A mensagem de erro evita a construção "em uma <i>origem</i>", porque
   * "título" é masculino e "movimentação" é feminino: um artigo fixo erraria em
   * um dos dois casos ("em uma título" / "em um movimentação"). Usando "de tipo
   * X em Y", nenhum artigo é necessário.
   *
   * @param categoriaId categoria informada no request
   * @param usuarioId dono do título ou da movimentação
   * @param tipo tipo do título ou da movimentação
   * @param origem nome do lançamento, no singular e sem artigo ("título",
   *     "movimentação")
   */
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
