package com.example.financas.service;

import com.example.financas.dto.request.ContaRequest;
import com.example.financas.dto.response.ContaResponse;
import com.example.financas.entity.Conta;
import com.example.financas.entity.Usuario;
import com.example.financas.exception.RecursoDuplicadoException;
import com.example.financas.exception.RecursoEmUsoException;
import com.example.financas.exception.RecursoNaoEncontradoException;
import com.example.financas.exception.RegraNegocioException;
import com.example.financas.mapper.ContaMapper;
import com.example.financas.repository.ContaRepository;
import com.example.financas.repository.MovimentacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContaService {

  private final ContaRepository contaRepository;
  private final MovimentacaoRepository movimentacaoRepository;
  private final UsuarioService usuarioService;

  public ContaService(
      ContaRepository contaRepository,
      MovimentacaoRepository movimentacaoRepository,
      UsuarioService usuarioService) {
    this.contaRepository = contaRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.usuarioService = usuarioService;
  }

  @Transactional(readOnly = true)
  public List<ContaResponse> listar(Long usuarioId) {
    Map<Long, BigDecimal> saldos = calcularSaldos(usuarioId);

    return contaRepository.findByUsuarioIdUsuario(usuarioId).stream()
        .map(conta -> ContaMapper.toResponse(conta, saldos.get(conta.getIdConta())))
        .toList();
  }

  @Transactional(readOnly = true)
  public ContaResponse buscarPorId(Long id) {
    Conta conta = buscarEntidade(id);
    BigDecimal saldo = buscarSaldoDaConta(conta);
    return ContaMapper.toResponse(conta, saldo);
  }

  @Transactional
  public ContaResponse criar(ContaRequest request) {
    if (contaRepository.existsByNomeAndUsuarioIdUsuario(request.nome(), request.usuarioId())) {
      throw new RecursoDuplicadoException(
          "Já existe a conta '" + request.nome() + "' para este usuário");
    }

    Usuario usuario = usuarioService.buscarEntidade(request.usuarioId());

    Conta conta = new Conta();
    conta.setNome(request.nome());
    conta.setTipo(request.tipo());
    conta.setSaldoInicial(request.saldoInicialOuZero());
    conta.setUsuario(usuario);

    Conta salva = contaRepository.save(conta);
    return ContaMapper.toResponse(salva, salva.getSaldoInicial());
  }

  @Transactional
  public ContaResponse atualizar(Long id, ContaRequest request) {
    Conta conta = buscarEntidade(id);

    if (!conta.getNome().equals(request.nome())
        && contaRepository.existsByNomeAndUsuarioIdUsuario(request.nome(), request.usuarioId())) {
      throw new RecursoDuplicadoException(
          "Já existe a conta '" + request.nome() + "' para este usuário");
    }

    conta.setNome(request.nome());
    conta.setTipo(request.tipo());
    conta.setSaldoInicial(request.saldoInicialOuZero());

    Conta salva = contaRepository.save(conta);
    return ContaMapper.toResponse(salva, buscarSaldoDaConta(salva));
  }

  @Transactional
  public void remover(Long id) {
    Conta conta = buscarEntidade(id);

    long movimentacoes = movimentacaoRepository.countByContaIdConta(id);
    if (movimentacoes > 0) {
      throw RecursoEmUsoException.porDependencia("conta", "contas", movimentacoes, "movimentação", "movimentações", true);
    }

    contaRepository.delete(conta);
  }

  public Conta buscarEntidade(Long id) {
    return contaRepository
        .findById(id)
        .orElseThrow(() -> RecursoNaoEncontradoException.porId("Conta", id, true));
  }

  public Conta validarContaDoUsuario(Long contaId, Long usuarioId) {
    Conta conta = buscarEntidade(contaId);

    if (!conta.getUsuario().getIdUsuario().equals(usuarioId)) {
      throw new RegraNegocioException("A conta informada pertence a outro usuário");
    }

    return conta;
  }

  private Map<Long, BigDecimal> calcularSaldos(Long usuarioId) {
    Map<Long, BigDecimal> saldos = new HashMap<>();

    for (Object[] linha : movimentacaoRepository.calcularSaldoPorConta(usuarioId)) {
      Long idConta = ((Number) linha[0]).longValue();
      BigDecimal saldo = (BigDecimal) linha[1];
      saldos.put(idConta, saldo);
    }

    return saldos;
  }

  private BigDecimal buscarSaldoDaConta(Conta conta) {
    return calcularSaldos(conta.getUsuario().getIdUsuario())
        .getOrDefault(conta.getIdConta(), conta.getSaldoInicial());
  }
}
