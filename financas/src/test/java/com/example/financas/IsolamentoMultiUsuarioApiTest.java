package com.example.financas;

import com.example.financas.config.TokenService;
import com.example.financas.entity.Usuario;
import com.example.financas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/testdata/10-dados-base.sql")
class IsolamentoMultiUsuarioApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TokenService tokenService;
  @Autowired private UsuarioRepository usuarioRepository;

  private String tokenAna;
  private String tokenBruno;

  @BeforeEach
  void setup() {
    Usuario ana = usuarioRepository.findById(1L).orElseThrow();
    Usuario bruno = usuarioRepository.findById(2L).orElseThrow();
    tokenAna = "Bearer " + tokenService.gerarToken(ana);
    tokenBruno = "Bearer " + tokenService.gerarToken(bruno);
  }

  @Test
  @DisplayName("Requisição sem token JWT retorna 401 no formato ErroResponse")
  void requisicaoSemTokenRetorna401Padronizado() throws Exception {
    mockMvc
        .perform(get("/api/v1/contas").param("usuarioId", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.erro").value("Não autenticado"))
        .andExpect(jsonPath("$.mensagem").value("Token de autenticação ausente, inválido ou expirado"));
  }

  @Test
  @DisplayName("Login com dados inválidos retorna 400 Erro de validação")
  void loginComDadosInvalidosRetorna400() throws Exception {
    String corpo = """
        {"email": "email-invalido", "senha": ""}
        """;
    mockMvc
        .perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.erro").value("Erro de validação"));
  }

  @Test
  @DisplayName("Login com credenciais incorretas retorna 401 no formato ErroResponse")
  void loginComCredenciaisIncorretasRetorna401() throws Exception {
    String corpo = """
        {"email": "ana@teste.com", "senha": "senha-errada"}
        """;
    mockMvc
        .perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.erro").value("Não autorizado"))
        .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
  }

  @Test
  @DisplayName("Usuário logado só enxerga a si mesmo na listagem de usuários")
  void usuarioLogadoSoListaASiMesmo() throws Exception {
    mockMvc
        .perform(get("/api/v1/usuarios").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].idUsuario").value(1))
        .andExpect(jsonPath("$[0].nome").value("Ana"));
  }

  @Test
  @DisplayName("Endpoint /me devolve o usuário autenticado")
  void endpointMeDevolveUsuarioLogado() throws Exception {
    mockMvc
        .perform(get("/api/v1/usuarios/me").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idUsuario").value(1))
        .andExpect(jsonPath("$.nome").value("Ana"))
        .andExpect(jsonPath("$.email").value("ana@teste.com"));
  }

  @Test
  @DisplayName("Usuário não consegue buscar outro usuário por id")
  void usuarioNaoConsegueBuscarOutroUsuarioPorId() throws Exception {
    mockMvc
        .perform(get("/api/v1/usuarios/2").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue listar contas de outro usuário")
  void usuarioNaoConsegueListarContasDeOutroUsuario() throws Exception {
    mockMvc
        .perform(get("/api/v1/contas").param("usuarioId", "2").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário"));
  }

  @Test
  @DisplayName("Usuário lista normalmente as suas próprias contas")
  void usuarioListaSuasPropriasContas() throws Exception {
    mockMvc
        .perform(get("/api/v1/contas").param("usuarioId", "1").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].idConta").value(1))
        .andExpect(jsonPath("$[0].nome").value("Conta da Ana"));
  }

  @Test
  @DisplayName("Usuário não consegue ver saldo nem detalhes da conta de outro usuário")
  void usuarioNaoConsegueVerContaDeOutroUsuario() throws Exception {
    mockMvc
        .perform(get("/api/v1/contas/2").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: esta conta pertence a outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue atualizar nem remover conta de outro usuário")
  void usuarioNaoConsegueAtualizarOuRemoverContaDeOutroUsuario() throws Exception {
    String corpo = """
        {"nome": "Conta Hackeada", "tipo": "CORRENTE", "saldoInicial": 9999, "usuarioId": 1}
        """;
    mockMvc
        .perform(put("/api/v1/contas/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo)
            .header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: esta conta pertence a outro usuário"));

    mockMvc
        .perform(delete("/api/v1/contas/2").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: esta conta pertence a outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue ver extrato da conta de outro usuário")
  void usuarioNaoConsegueVerExtratoDeOutroUsuario() throws Exception {
    mockMvc
        .perform(get("/api/v1/contas/2/extrato").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("A conta informada pertence a outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue listar categorias de outro usuário")
  void usuarioNaoConsegueListarCategoriasDeOutroUsuario() throws Exception {
    mockMvc
        .perform(get("/api/v1/categorias").param("usuarioId", "2").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue ver categoria de outro usuário por ID")
  void usuarioNaoConsegueVerCategoriaDeOutroUsuarioPorId() throws Exception {
    mockMvc
        .perform(get("/api/v1/categorias/4").header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: esta categoria pertence a outro usuário"));
  }

  @Test
  @DisplayName("Não é possível alterar o tipo de uma categoria já vinculada a títulos")
  void naoPermiteAlterarTipoDeCategoriaEmUso() throws Exception {
    String novoTitulo = """
        {
          "descricao": "Aluguel",
          "valorPrevisto": 1200.0,
          "dataVencimento": "2026-03-10",
          "tipo": "DESPESA",
          "categoriaId": 1,
          "usuarioId": 1
        }
        """;
    mockMvc
        .perform(post("/api/v1/titulos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(novoTitulo)
            .header("Authorization", tokenAna))
        .andExpect(status().isCreated());

    String alterarCategoria = """
        {"nome": "Moradia", "tipo": "RECEITA", "usuarioId": 1}
        """;
    mockMvc
        .perform(put("/api/v1/categorias/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(alterarCategoria)
            .header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem").value(
            "Não é possível alterar o tipo de uma categoria que já possui títulos ou movimentações vinculados"));
  }

  @Test
  @DisplayName("Usuário não consegue acessar DRE de outro usuário")
  void usuarioNaoConsegueAcessarDreDeOutroUsuario() throws Exception {
    mockMvc
        .perform(get("/api/v1/dre")
            .param("usuarioId", "2")
            .param("inicio", "2026-03-01")
            .param("fim", "2026-03-31")
            .header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário"));
  }

  @Test
  @DisplayName("Usuário não consegue criar título em nome de outro usuário")
  void usuarioNaoConsegueCriarTituloEmNomeDeOutroUsuario() throws Exception {
    String corpo = """
        {
          "descricao": "Tentativa Invasão",
          "valorPrevisto": 100.0,
          "dataVencimento": "2026-03-20",
          "tipo": "DESPESA",
          "categoriaId": 1,
          "usuarioId": 2
        }
        """;

    mockMvc
        .perform(post("/api/v1/titulos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo)
            .header("Authorization", tokenAna))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.erro").value("Regra de negócio violada"))
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: você não tem permissão para acessar ou manipular dados de outro usuário"));
  }

  @Test
  @DisplayName("Transferência entre contas, extrato e estorno funcionam sem erros")
  void transferenciaExtratoEEstornoFuncionamCorretamente() throws Exception {
    String novaConta = """
        {"nome": "Poupança da Ana", "tipo": "POUPANCA", "saldoInicial": 200.0, "usuarioId": 1}
        """;
    mockMvc
        .perform(post("/api/v1/contas")
            .contentType(MediaType.APPLICATION_JSON)
            .content(novaConta)
            .header("Authorization", tokenAna))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idConta").value(3));

    String transferencia = """
        {
          "contaOrigemId": 1,
          "contaDestinoId": 3,
          "valor": 300.0,
          "data": "2026-03-15",
          "descricao": "Reserva"
        }
        """;
    mockMvc
        .perform(post("/api/v1/movimentacoes/transferir")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferencia)
            .header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valor").value(300.0));

    // Extrato da conta 1 deve listar a transferência sem NullPointerException
    mockMvc
        .perform(get("/api/v1/contas/1/extrato").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].categoriaNome").value("Transferência"));

    // Bruno não consegue estornar a movimentação da Ana
    mockMvc
        .perform(post("/api/v1/movimentacoes/1/estornar").header("Authorization", tokenBruno))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.mensagem").value("Acesso negado: esta movimentação pertence a outro usuário"));

    // Ana consegue estornar sua movimentação e ambas as contas voltam aos saldos originais
    mockMvc
        .perform(post("/api/v1/movimentacoes/1/estornar").header("Authorization", tokenAna))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/v1/contas/1").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoAtual").value(1000.0));

    mockMvc
        .perform(get("/api/v1/contas/3").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoAtual").value(200.0));
  }

  @Test
  @DisplayName("Resposta 401 serializa URI com aspas em JSON válido")
  void resposta401SerializaCaracteresEspeciaisComSeguranca() throws Exception {
    String caminhoEspecial = "/api/v1/contas/\"aspas\"";
    mockMvc
        .perform(
            get("/api/v1/contas")
                .with(
                    req -> {
                      req.setRequestURI(caminhoEspecial);
                      return req;
                    }))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.erro").value("Não autenticado"))
        .andExpect(jsonPath("$.caminho").value(caminhoEspecial));
  }

  @Test
  @DisplayName("Atualizar próprio usuário com e-mail em maiúsculas/minúsculas mistas não gera conflito 409")
  void atualizarProprioUsuarioComEmailCaseDiferenteNaoGeraConflito() throws Exception {
    String atualizacao = """
        {
          "nome": "Ana Atualizada",
          "email": "Ana@Teste.com",
          "senha": "novaSenha123",
          "ativo": true
        }
        """;
    mockMvc
        .perform(
            put("/api/v1/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(atualizacao)
                .header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ana@teste.com"));
  }

  @Test
  @DisplayName("Cancelar título define valorEmAberto igual a zero")
  void cancelarTituloZeraValorEmAberto() throws Exception {
    String titulo = """
        {
          "descricao": "Aluguel",
          "valorPrevisto": 500.0,
          "dataVencimento": "2026-04-10",
          "tipo": "DESPESA",
          "categoriaId": 1,
          "usuarioId": 1
        }
        """;
    mockMvc
        .perform(
            post("/api/v1/titulos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(titulo)
                .header("Authorization", tokenAna))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.idTitulo").value(1));

    mockMvc
        .perform(post("/api/v1/titulos/1/cancelar").header("Authorization", tokenAna))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.situacao").value("CANCELADO"))
        .andExpect(jsonPath("$.valorEmAberto").value(0));
  }

  @Test
  @DisplayName("Content-Type não suportado retorna 415 em formato ErroResponse")
  void contentTypeNaoSuportadoRetorna415() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/contas")
                .contentType(MediaType.TEXT_PLAIN)
                .content("texto puro")
                .header("Authorization", tokenAna))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.erro").value("Tipo de mídia não suportado"));
  }
}
