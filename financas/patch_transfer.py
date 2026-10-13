import re
import os

base = "src/main/java/com/example/financas/"

# 1. MovimentacaoService
f = os.path.join(base, "service/MovimentacaoService.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

import_str = """import com.example.financas.dto.request.TransferenciaRequest;
import com.example.financas.dto.response.TransferenciaResponse;
import com.example.financas.entity.TipoMovimentacao;"""

content = content.replace("import com.example.financas.entity.TipoMovimentacao;", import_str)

method_str = """
  @Transactional
  public TransferenciaResponse transferir(TransferenciaRequest request, Long usuarioId) {
    if (request.contaOrigemId().equals(request.contaDestinoId())) {
      throw new RegraNegocioException("A conta de origem não pode ser a mesma de destino");
    }

    Conta contaOrigem = contaService.validarContaDoUsuario(request.contaOrigemId(), usuarioId);
    Conta contaDestino = contaService.validarContaDoUsuario(request.contaDestinoId(), usuarioId);
    Usuario usuario = contaOrigem.getUsuario();

    String descricao = request.descricao() != null && !request.descricao().trim().isEmpty() 
        ? request.descricao() 
        : "Transferência";

    Movimentacao saida = new Movimentacao();
    saida.setConta(contaOrigem);
    saida.setUsuario(usuario);
    saida.setTipo(TipoMovimentacao.TRANSFERENCIA);
    saida.setValor(request.valor());
    saida.setData(request.data());
    saida.setDescricao(descricao + " para " + contaDestino.getNome());
    saida.setIsTransferencia(true);
    saida = movimentacaoRepository.save(saida);

    Movimentacao entrada = new Movimentacao();
    entrada.setConta(contaDestino);
    entrada.setUsuario(usuario);
    entrada.setTipo(TipoMovimentacao.TRANSFERENCIA);
    entrada.setValor(request.valor());
    entrada.setData(request.data());
    entrada.setDescricao(descricao + " de " + contaOrigem.getNome());
    entrada.setIsTransferencia(true);
    entrada = movimentacaoRepository.save(entrada);

    return new TransferenciaResponse(
        saida.getIdMovimentacao(),
        entrada.getIdMovimentacao(),
        contaOrigem.getIdConta(),
        contaDestino.getIdConta(),
        request.valor(),
        request.data(),
        descricao
    );
  }
}"""

content = re.sub(r'}\s*$', method_str, content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)

# 2. MovimentacaoController
f = os.path.join(base, "controller/MovimentacaoController.java")
with open(f, 'r', encoding='utf-8') as file:
    content = file.read()

import_str2 = """import com.example.financas.dto.request.TransferenciaRequest;
import com.example.financas.dto.response.TransferenciaResponse;"""
content = content.replace("import com.example.financas.dto.request.MovimentacaoRequest;", "import com.example.financas.dto.request.MovimentacaoRequest;\n" + import_str2)

endpoint_str = """
  @Operation(
      summary = "Transfere dinheiro entre duas contas do usuário"
  )
  @PostMapping("/transferir")
  public ResponseEntity<TransferenciaResponse> transferir(
      @Valid @RequestBody TransferenciaRequest request,
      @AuthenticationPrincipal Usuario usuario) {
    return ResponseEntity.ok(movimentacaoService.transferir(request, usuario.getIdUsuario()));
  }
}"""

content = re.sub(r'}\s*$', endpoint_str, content)

with open(f, 'w', encoding='utf-8') as file:
    file.write(content)
