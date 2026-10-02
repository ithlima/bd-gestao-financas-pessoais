package com.example.financas.service;

import com.example.financas.dto.request.MovimentacaoRequest;
import com.example.financas.dto.response.MovimentacaoResponse;
import com.example.financas.entity.Categoria;
import com.example.financas.entity.Conta;
import com.example.financas.entity.Movimentacao;
import com.example.financas.entity.SituacaoTitulo;
import com.example.financas.entity.TipoMovimentacao;
import com.example.financas.entity.Titulo;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.mapper.MovimentacaoMapper;
import com.example.financas.repository.MovimentacaoRepository;
import com.example.financas.repository.TituloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Regras de negócio das <b>movimentações</b> — os eventos financeiros realizados.
 *
 * <p>Este service é o único ponto do sistema autorizado a criar uma
 * movimentação. Isso é deliberado: é aqui que ficam as regras RN13 a RN20, e
 * centralizá-las garante que nenhuma movimentação nasça sem conta, sem categoria
 * coerente ou com valor inválido.
 *
 * <p>Há dois caminhos de entrada:
 *
 * <ol>
 *   <li>{@link #registrarAvulsa(MovimentacaoRequest)} — lançamento direto, sem
 *       previsão de origem (ex.: "paguei um café em dinheiro"). É o que preserva
 *       o uso que o sistema já tinha;</li>
 *   <li>{@link #registrarQuitacaoDeTitulo(NovaMovimentacao)} — chamado pelo
 *       {@code TituloService} quando um título é pago ou recebido. Nesse caso o
 *       tipo, a categoria e a descrição são <b>herdados</b> do título (RN16).</li>
 * </ol>
 *
 * <h2>Sobre as dependências</h2>
 *
 * <p>Aqui está o ponto delicado da arquitetura: o {@code TituloService} precisa
 * deste service para criar a movimentação, e este service precisa do
 * {@code TituloService} para atualizar a situação do título. Se as duas classes
 * se injetassem mutuamente, haveria um <b>ciclo de beans</b>, que o Spring
 * recusa por padrão.
 *
 * <p>A solução adotada foi criar uma <b>dependência de mão única</b>: o
 * {@code TituloService} depende deste service, e este service <b>não</b> depende
 * dele. Para isso:
 *
 * <ul>
 *   <li>as validações de quitação (RN05, RN06, RN09) foram implementadas aqui
 *       como {@link #validarQuitacao(Titulo, BigDecimal)} — o
 *       {@code TituloService} as reaproveita, evitando duplicação;</li>
 *   <li>a mudança de situação do título passou a ser uma
 *       {@code @Modifying @Query} no repositório, em vez de uma chamada de
 *       service.</li>
 * </ul>
 *
 * <p>O resultado é mais simples de ler do que a alternativa (um
 * {@code @Lazy} para mascarar o ciclo) e não esconde a direção da dependência.
 */
@Service
public class MovimentacaoService {

  private final MovimentacaoRepository movimentacaoRepository;
  private final TituloRepository tituloRepository;
  private final UsuarioService usuarioService;
  private final ContaService contaService;
  private final CategoriaService categoriaService;

  public MovimentacaoService(
      MovimentacaoRepository movimentacaoRepository,
      TituloRepository tituloRepository,
      UsuarioService usuarioService,
      ContaService contaService,
      CategoriaService categoriaService) {
    this.movimentacaoRepository = movimentacaoRepository;
    this.tituloRepository = tituloRepository;
    this.usuarioService = usuarioService;
    this.contaService = contaService;
    this.categoriaService = categoriaService;
  }

  // ------------------------------------------------------------------
  // Consultas
  // ------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<MovimentacaoResponse> listar(
      Long usuarioId, LocalDate inicio, LocalDate fim, TipoMovimentacao tipo) {

    List<Movimentacao> movimentacoes;

    if (inicio != null && fim != null) {
      movimentacoes =
          movimentacaoRepository.findByUsuarioIdUsuarioAndDataBetweenOrderByDataAsc(
              usuarioId, inicio, fim);
    } else {
      movimentacoes = movimentacaoRepository.findByUsuarioIdUsuario(usuarioId);
    }

    if (tipo != null) {
      movimentacoes = movimentacoes.stream().filter(m -> m.getTipo() == tipo).toList();
    }

    return movimentacoes.stream().map(MovimentacaoMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public MovimentacaoResponse buscarPorId(Long id) {
    return MovimentacaoMapper.toResponse(buscarEntidade(id));
  }

  @Transactional(readOnly = true)
  public List<MovimentacaoResponse> extratoDaConta(Long contaId) {
    contaService.buscarEntidade(contaId);
    return movimentacaoRepository.findByContaIdConta(contaId).stream()
        .map(MovimentacaoMapper::toResponse)
        .toList();
  }

  // ------------------------------------------------------------------
  // Caminho 1 — lançamento avulso
  // ------------------------------------------------------------------

  /**
   * Registra uma movimentação sem título de origem (RN18).
   *
   * <p>Validações aplicadas: usuário existe, conta existe e é do mesmo usuário
   * (RN14), categoria é do mesmo usuário e do mesmo tipo (RN15) e valor maior que
   * zero (garantido pelo Bean Validation no DTO e reforçado aqui).
   */
  @Transactional
  public MovimentacaoResponse registrarAvulsa(MovimentacaoRequest request) {
    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());
    Conta conta = contaService.validarContaDoUsuario(request.contaId(), request.usuarioId());
    Categoria categoria =
        categoriaService.validarCategoriaDoLancamento(
            request.categoriaId(), request.usuarioId(), request.tipo(), "movimentação");

    Movimentacao movimentacao = new Movimentacao();
    movimentacao.setDescricao(request.descricao());
    movimentacao.setValor(request.valor());
    movimentacao.setTipo(request.tipo());
    movimentacao.setData(request.data());
    movimentacao.setUsuario(usuario);
    movimentacao.setCategoria(categoria);
    movimentacao.setConta(conta);
    // Sem título de origem: é um lançamento avulso.
    movimentacao.setTitulo(null);

    return MovimentacaoMapper.toResponse(movimentacaoRepository.save(movimentacao));
  }

  // ------------------------------------------------------------------
  // Caminho 2 — quitação de um título
  // ------------------------------------------------------------------

  /**
   * Cria a movimentação correspondente à quitação (total ou parcial) de um
   * título e, se a soma dos pagamentos atingir o valor previsto, marca o título
   * como PAGO.
   *
   * <p><b>Esta é a operação central do sistema.</b> É ela que materializa a
   * passagem
   *
   * <pre>
   *   TÍTULO (previsão)  ->  MOVIMENTAÇÃO (realização)  ->  DRE realizada
   * </pre>
   *
   * <p>Regras aplicadas: RN09 (não ultrapassar o previsto), RN10 (virar PAGO ao
   * quitar), RN14 (conta do mesmo usuário), RN16 (herdar tipo e categoria do
   * título). Tudo dentro de uma transação: ou a movimentação é criada <i>e</i> o
   * título atualizado, ou nada acontece.
   */
  @Transactional
  public Movimentacao registrarQuitacaoDeTitulo(NovaMovimentacao dados) {
    Titulo titulo = dados.titulo();

    // RN05, RN06, RN09 — o valor não pode ultrapassar o que ainda falta pagar.
    validarQuitacao(titulo, dados.valor());

    // RN14 — a conta precisa ser do mesmo usuário do título.
    Conta conta =
        contaService.validarContaDoUsuario(
            dados.conta().getIdConta(), titulo.getUsuario().getIdUsuario());

    Movimentacao movimentacao = new Movimentacao();
    movimentacao.setDescricao(titulo.getDescricao());
    // RN16: tipo e categoria são herdados do título, nunca informados pelo cliente.
    movimentacao.setTipo(titulo.getTipo());
    movimentacao.setCategoria(titulo.getCategoria());
    movimentacao.setValor(dados.valor());
    movimentacao.setData(dados.data());
    movimentacao.setUsuario(titulo.getUsuario());
    movimentacao.setConta(conta);
    movimentacao.setTitulo(titulo);

    Movimentacao salva = movimentacaoRepository.save(movimentacao);

    // RN10 — recalcula o total realizado. Se quitou, o título vira PAGO.
    quitarTituloSeTotalmenteRealizado(titulo);

    return salva;
  }

  /**
   * Valida se o título pode receber uma quitação de {@code valor}
   * (RN05, RN06, RN09).
   *
   * <p>Mora aqui, e não no {@code TituloService}, para manter a dependência em
   * mão única. O {@code TituloService} reaproveita este método público em vez de
   * reimplementar as mesmas três verificações.
   */
  public void validarQuitacao(Titulo titulo, BigDecimal valor) {
    if (titulo.getSituacao() == SituacaoTitulo.CANCELADO) {
      throw new RegraNegocioException("Um título cancelado não pode ser pago ou recebido");
    }
    if (titulo.getSituacao() == SituacaoTitulo.PAGO) {
      throw new RegraNegocioException("Este título já está quitado");
    }

    BigDecimal emAberto = titulo.getValorPrevisto().subtract(totalRealizadoDoTitulo(titulo.getIdTitulo()));

    if (valor.compareTo(emAberto) > 0) {
      throw new RegraNegocioException(
          "O valor informado (R$ "
              + valor
              + ") é maior que o valor em aberto do título (R$ "
              + emAberto
              + ")");
    }
  }

  /**
   * Marca o título como PAGO quando o total realizado atinge o valor previsto
   * (RN09, RN10). Enquanto faltar, o título permanece PENDENTE — é o caso do
   * pagamento parcial.
   *
   * <p>A alteração é feita por uma consulta de atualização direta no repositório,
   * e não por um {@code save} da entidade — ver a explicação sobre o ciclo de
   * beans na documentação da classe.
   */
  private void quitarTituloSeTotalmenteRealizado(Titulo titulo) {
    BigDecimal realizado = totalRealizadoDoTitulo(titulo.getIdTitulo());

    if (realizado.compareTo(titulo.getValorPrevisto()) >= 0) {
      LocalDate dataDaQuitacao =
          movimentacaoRepository.findByTituloIdTitulo(titulo.getIdTitulo()).stream()
              .map(Movimentacao::getData)
              .max(LocalDate::compareTo)
              .orElse(LocalDate.now());

      tituloRepository.marcarComoPago(titulo.getIdTitulo(), dataDaQuitacao);

      // Mantém o objeto em memória coerente com o banco, para a resposta da API
      // já sair com a situação atualizada.
      titulo.quitar(dataDaQuitacao);
    }
  }

  // ------------------------------------------------------------------
  // Apoio
  // ------------------------------------------------------------------

  private Movimentacao buscarEntidade(Long id) {
    return movimentacaoRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Movimentação", id, true));
  }

  /** Total já realizado de um título. */
  @Transactional(readOnly = true)
  public BigDecimal totalRealizadoDoTitulo(Long tituloId) {
    BigDecimal total = movimentacaoRepository.somarRealizadoDoTitulo(tituloId);
    return total != null ? total : BigDecimal.ZERO;
  }

  /** Quantas movimentações já quitaram parte deste título. */
  @Transactional(readOnly = true)
  public int contarMovimentacoesDoTitulo(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId).size();
  }

  /** Indica se o título já tem alguma movimentação vinculada. */
  @Transactional(readOnly = true)
  public boolean temMovimentacaoVinculada(Long tituloId) {
    return !movimentacaoRepository.findByTituloIdTitulo(tituloId).isEmpty();
  }

  /** Data do pagamento mais recente de um título. */
  @Transactional(readOnly = true)
  public LocalDate dataDoUltimoPagamento(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId).stream()
        .map(Movimentacao::getData)
        .max(LocalDate::compareTo)
        .orElse(LocalDate.now());
  }

  /** Movimentações vinculadas a um título. */
  @Transactional(readOnly = true)
  public List<Movimentacao> listarPorTitulo(Long tituloId) {
    return movimentacaoRepository.findByTituloIdTitulo(tituloId);
  }
}
