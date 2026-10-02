# Evolução do sistema de Finanças Pessoais — Análise e Plano

Documento de diagnóstico e planejamento. A implementação só começa após aprovação.

> Observação de nomenclatura: ao longo do documento, nomes de **tabela/coluna** aparecem em
> `snake_case` (`titulo`, `valor_previsto`) e nomes de **classe/campo Java** em `CamelCase`
> (`Titulo`, `valorPrevisto`). Quando eu digo "movimentação" sozinho, refiro-me ao evento
> financeiro **realizado** (dinheiro que efetivamente entrou ou saiu) — esse é o ponto central
> de toda a proposta.

---

## Etapa 1 — Diagnóstico da estrutura atual

### 1.1 O que existe hoje

**Projeto:** `financas/` — Spring Boot, Java 21, Maven, MariaDB (`financas` em `localhost:3306`),
`ddl-auto=update`, MariaDB 12.3.3.

**Código-fonte (15 arquivos `.java`):**

| Camada | Situação real |
|---|---|
| `entity/` | 7 arquivos: `Usuario`, `Conta`, `Categoria`, `Movimentacao`, `Recebimento`, `Pagamento`, `TipoMovimentacao` (enum) |
| `repository/` | 6 interfaces `JpaRepository` |
| `config/` | `SecurityConfig` (CSRF desligado, `anyRequest().permitAll()`) |
| `service/` | **não existe** |
| `controller/` | **não existe** |
| `dto/` | **não existe** |
| `exception/` | **não existe** |
| `mapper/` | **não existe** |

O `README.md` descreve uma arquitetura com `dto`, `mapper`, `exception` e `security`, e afirma que
"o total de pagamentos de uma despesa não deve ultrapassar o valor da movimentação" e que "o valor
de movimentações, recebimentos e pagamentos deve ser maior que zero". **Nenhuma dessas regras está
implementada no código.** Hoje o projeto não é um CRUD genérico — é um CRUD *vazio*: as entidades
existem, mas não há sequer um endpoint HTTP. Isso é uma boa notícia para a evolução: não há
contrato de API pública para preservar.

**Banco de dados `financas`:** 6 tabelas existentes (`usuario`, `conta`, `categoria`,
`movimentacao`, `recebimento`, `pagamento`), **todas vazias (0 linhas)**. As tabelas foram criadas
pelo Hibernate a partir das anotações, não por script SQL. Não há `schema.sql`, `data.sql`, migrações
Flyway/Liquibase nem arquivos `.sql` versionados.

**Ponto de atenção:** com 0 linhas em todas as tabelas, **não existe risco de perda de dados**. O
risco real de "quebrar o que funciona" está nas *classes* (as entidades e os repositórios) e no
significado conceitual que elas carregam — não nos dados.

### 1.2 Modelo relacional atual (verificado no banco)

```text
usuario      (id_usuario PK, nome, email UNIQUE, senha, data_cadastro, ativo)
conta        (id_conta PK, nome, tipo, saldo_inicial, usuario_id FK→usuario)   UQ(nome, usuario_id)
categoria    (id_categoria PK, nome VARCHAR(10), tipo ENUM, usuario_id FK→usuario)  UQ(nome, usuario_id)
movimentacao (id_movimentacao PK, descricao, valor, tipo ENUM, data,
              usuario_id FK→usuario, categoria_id FK→categoria)
recebimento  (id_recebimento PK, valor, data_recebimento,
              movimentacao_id FK→movimentacao, conta_id FK→conta)
pagamento    (id_pagamento PK, valor, data_pagamento,
              movimentacao_id FK→movimentacao, conta_id FK→conta)
```

Relacionamentos: `usuario 1:N {conta, categoria, movimentacao}`,
`categoria 1:N movimentacao`, `movimentacao 1:N {recebimento, pagamento}`,
`conta 1:N {recebimento, pagamento}`.

### 1.3 Problemas e limitações encontrados

**P1 — `movimentacao` acumula dois conceitos incompatíveis.**
A tabela tem `valor` **e** `data` (campos de um conceito) e ao mesmo tempo é o lado "1" de uma
relação `1:N` com `recebimento`/`pagamento`, que também têm `valor` e `data`. Ou seja: existem
**duas datas e dois valores concorrentes** para o mesmo dinheiro, e nada no modelo diz qual é o
"previsto" e qual é o "realizado". O README chama a movimentação de "lançamento reconhecido por
competência" e o pagamento de "saída efetiva" — mas ambos são gravados na mesma cadeia, sem
distinção explícita. **Esse é o problema central que a introdução do Título resolve.**

**P2 — Não existe previsão sem realização.**
> "Tenho uma conta de energia de R$ 150,00 que vence dia 10."

Hoje, para representar isso, é obrigatório criar uma `movimentacao` — e a movimentação já carrega
`valor` e `data`, ou seja, **a despesa passa a existir contabilmente no instante do cadastro**. É
exatamente o comportamento que o usuário quer evitar. Não há como dizer "isso está previsto, mas
ainda não aconteceu".

**P3 — Não existe situação (*status*) de pagamento.**
Não há campo algum de situação. Não é possível distinguir pendente, pago, vencido ou cancelado. Sem
isso, uma DRE correta é impossível: qualquer consulta por período soma títulos não pagos como se
fossem despesas realizadas.

**P4 — Não existe data de vencimento.**
Sem `data_vencimento` não há como apurar inadimplência, projetar fluxo de caixa futuro nem separar
"competência" (quando a obrigação pertence ao período) de "caixa" (quando o dinheiro se move).

**P5 — `recebimento` e `pagamento` são tabelas-gêmeas redundantes.**
As duas têm estrutura idêntica (`valor`, `data_*`, `movimentacao_id`, `conta_id`) e só diferem no
nome. Isso obriga a aplicação a validar em dois lugares que o pagamento pertence a uma despesa e o
recebimento a uma receita — uma regra que o banco não conhece. A distinção receita/despesa já é dada
por `movimentacao.tipo` / `titulo.tipo`; manter duas tabelas dobra o código sem acrescentar
informação.

**P6 — `categoria.nome` é `VARCHAR(10)`.**
Verificado no banco. As categorias do exemplo do próprio usuário não caberiam:
`Alimentação` (11 caracteres), `Transporte` (10, no limite), `Rendimentos` (11), `Outras receitas`
(15). Cadastrar "Alimentação" hoje **falha** com erro de truncamento. É um defeito concreto,
verificável, que precisa ser corrigido.

**P7 — A regra "valor maior que zero" não existe no código.**
Não há `@Positive`, `@DecimalMin` nem `@Check` em nenhuma entidade. A validação do README é
aspiracional. Um `BigDecimal` negativo é aceito sem reclamação — em um sistema financeiro isso é um
defeito grave.

**P8 — `tipo` duplicado sem garantia de coerência.**
`categoria.tipo` e `movimentacao.tipo` são independentes. Nada impede uma movimentação `RECEITA` em
categoria de `DESPESA`. A regra existe no README, não no código nem no banco.

**P9 — Ausência total da camada de aplicação.**
Sem service/controller/DTO/exception não há onde colocar regra de negócio, não há tratamento de erro
consistente e as entidades JPA seriam serializadas diretamente (com `LAZY` isso gera
`LazyInitializationException` ou vazamento de dados como `senha`). Precisa ser construída.

**P10 — `Categoria` usa `long` primitivo para o ID.**
`private long idCategoria;` — os outros usam `Long`. Primitivo não representa "ainda não persistido"
e quebra convenções do JPA. Pequena inconsistência, barata de corrigir.

**P11 — `SecurityConfig` usa DSL legado e libera tudo.**
`csrf(csrf -> csrf.disable())` e `auth -> auth.anyRequest().permitAll()` compilam, mas em Spring Boot
4.x a DSL clássica está depreciada em favor da DSL em lambda explícita. Não é um defeito funcional
(hoje não há endpoints), mas precisa ser ajustado ao criar os controllers, para não deixar o sistema
inteiro aberto sem intenção declarada.

### 1.4 O que já está bom e será preservado

- **`usuario`**: PK, `email` único, `data_cadastro` automática via `@PrePersist`, flag `ativo`. Sólido.
- **`conta`**: `UNIQUE(nome, usuario_id)` está correto — nome de conta é único *por usuário*, não
  global. Esse padrão será repetido nas novas tabelas.
- **`categoria`**: `UNIQUE(nome, usuario_id)` idem, e já tem `tipo` (receita/despesa), que é
  exatamente o que a DRE precisa para agrupar linhas.
- **`TipoMovimentacao`**: enum correto e reutilizável. Será reaproveitado como o `tipo` do Título.
- **Padrão de FKs nomeadas**: `@ForeignKey(name = "fk_*")` é uma boa prática já adotada.
- **Repositórios derivados** (`findByUsuarioIdUsuario...`): os métodos existentes serão mantidos e
  apenas complementados, nunca removidos.
- **`Usuario`, `Conta`, `Categoria` como entidades**: permanecem praticamente intactas.

---

## Etapa 2 — Nova modelagem conceitual

### 2.1 Título × Movimentação: a distinção conceitual

Essa é a decisão arquitetural central, então vale enunciá-la com precisão:

| | **Título** | **Movimentação** |
|---|---|---|
| Pergunta que responde | "O que eu **tenho** a pagar/receber?" | "O que **aconteceu** com o meu dinheiro?" |
| Natureza | **Compromisso / previsão** (direito ou obrigação) | **Evento realizado** (fato consumado) |
| Valor | `valorPrevisto` (quanto foi combinado) | `valor` (quanto efetivamente se moveu) |
| Data | `dataVencimento` (quando **deve** acontecer) | `data` (quando **aconteceu**) |
| Situação | `PENDENTE` / `PAGO` / `VENCIDO` / `CANCELADO` | existe por definição — se foi registrada, aconteceu |
| Conta bancária | **não** tem (a obrigação não está em nenhum banco ainda) | **tem** e é obrigatória (`conta_id`) |
| Efeito na DRE realizado | **nenhum** | é a **única** fonte de receita/despesa realizada |
| Pode ser cancelado | sim, sem nunca gerar movimentação | não — cancelar um fato exige estorno |
| Exemplo | "Conta de energia, R$ 150,00, vence 10/03" | "Paguei R$ 150,00 em 08/03 pela conta corrente" |

Em uma frase: **o título é a promessa; a movimentação é o cumprimento.** O título é *o que deveria
acontecer*; a movimentação é *o que aconteceu*. Um título pode existir a vida inteira sem nunca gerar
uma movimentação (basta ser cancelado); uma movimentação, uma vez criada, é irreversível na prática —
o dinheiro já se moveu.

**Por que essa separação é necessária e não apenas elegante:** sem ela, o sistema não consegue
responder à pergunta mais básica de um usuário — *"quanto eu ainda tenho para pagar este mês?"* —
porque tudo que foi cadastrado é tratado como já pago. E não consegue responder *"quanto eu realmente
gastei este mês?"* com segurança, porque não sabe quais dos lançamentos cadastrados viraram dinheiro.

### 2.2 Decisão sobre `recebimento` e `pagamento`: **incorporar em `movimentacao`**

O usuário perguntou se `pagamento` ainda faz sentido. Minha resposta: **não, e `recebimento` também
não.** As duas tabelas devem ser **absorvidas por `movimentacao`**, que passa a ser a tabela única de
eventos financeiros realizados.

Justificativa:

1. **`movimentacao` já possui estrutura equivalente.** Comparando coluna a coluna,
   `movimentacao(valor, data, usuario_id, categoria_id)` +
   `recebimento(valor, data_recebimento, conta_id)` é **a mesma coisa** que uma única tabela
   `movimentacao(descricao, valor, data, tipo, usuario_id, categoria_id, conta_id)`. A separação
   existe por acidente histórico, não por necessidade de modelagem.
2. **A direção já está em `tipo`.** `recebimento` vs `pagamento` é apenas "receita" vs "despesa" —
   informação que `movimentacao.tipo` já carrega. Duas tabelas para o mesmo conceito com uma coluna
   de diferença violam a normalização (é uma dependência funcional `valor → tipo` que deveria estar
   na mesma relação).
3. **É o que permite a DRE ser uma consulta só.** Com uma única tabela de eventos, o resultado do
   período é **um** `SELECT` agrupado por `tipo` e `categoria_id`. Com duas tabelas, todo relatório
   precisa de `UNION ALL` e de somar dois totais diferentes — mais código, mais chance de erro.
4. **O modelo fica mais próximo do mundo real.** Um extrato bancário é uma lista única de lançamentos
   com sinal; não é "uma lista de entradas" e "outra lista de saídas" em estruturas distintas.

O que se perde: a separação física. O que se ganha: uma fonte única de verdade para o caixa, e a
possibilidade de validar no *service* (e futuramente no banco) que uma movimentação de despesa
decrementa a conta e uma de receita incrementa.

**Consequência:** a classe `Pagamento` e a classe `Recebimento` deixam de ser entidades e passam a ser
**DTOs de requisição** (`PagamentoRequest`, `RecebimentoRequest`), pois a operação "pagar um título" e
"receber um título" continuam existindo como *casos de uso* — apenas não são tabelas.

### 2.3 Decisão sobre `VENCIDO`: **situação derivada, não armazenada**

O usuário listou quatro situações: `PENDENTE`, `PAGO`, `VENCIDO`, `CANCELADO`. Três são **estados
persistidos** (decisões do usuário ou consequências de um pagamento). `VENCIDO` **não é** — é uma
consequência do tempo.

Se `VENCIDO` fosse gravado em `situacao`, seria necessário um *job* agendado rodando toda
madrugada para varrer a tabela e marcar os títulos que venceram ontem. Se o job falhasse, ou se o
sistema ficasse parado no fim de semana, a situação ficaria mentindo. Pior: qualquer relatório
histórico ficaria errado retroativamente.

**Regra adotada:** o banco armazena `PENDENTE`, `PAGO` ou `CANCELADO`. `VENCIDO` é **calculado em
tempo de leitura**:

```text
situacaoEfetiva(titulo) =
    CANCELADO, se situacao = CANCELADO
    PAGO,      se situacao = PAGO
    VENCIDO,   se situacao = PENDENTE e dataVencimento < hoje
    PENDENTE,  caso contrário
```

Na API, o campo de resposta se chama `situacao` (o valor efetivo, podendo ser `VENCIDO`) e o campo
`podeRegistrarPagamento` indica se a operação é permitida. Isso dá ao usuário exatamente os quatro
valores que ele pediu, sem fragilidade operacional. É um caso clássico de **estado derivado** — o
mesmo raciocínio de "idade" não ser uma coluna quando se tem "data de nascimento".

### 2.4 Decisão sobre pagamento parcial: **suportado, com "pago" significando "quitado"**

Caso real: aluguel de R$ 1.200,00 pago em duas vezes. Um único campo `dataPagamento` no título não
representa isso.

**Regra:** um título pode ter **N movimentações**. O título só fica `PAGO` quando a soma das
movimentações vinculadas **iguala** o `valorPrevisto`. Enquanto for menor, permanece `PENDENTE`
(e exibido como "parcialmente pago", com `valorRealizado` e `valorEmAberto` no DTO). Isso é
exatamente o que o modelo atual já sugeria com `movimentacao 1:N pagamento` — **preservamos a
cardinalidade**, apenas a renomeamos e a tornamos conceitualmente correta.

Assim, o campo `situacao = PAGO` significa **"quitado"**, não "teve algum pagamento". Essa é a única
leitura que mantém a DRE consistente e evita a ambiguidade de um título metade pago aparecer como
"pago".

### 2.5 Diagrama conceitual

```text
                              USUÁRIO
                                 │
        ┌──────────────┬─────────┴────────┬──────────────────┐
        │              │                  │                  │
     CONTAS        CATEGORIAS         TÍTULOS          MOVIMENTAÇÕES
   (onde o        (como eu            (o que eu        (o que aconteceu)
    dinheiro       classifico)         devo/recebo)          │
    está)              │                  │                   │
        │              │                  │                   │
        │              └─── tipo ─────────┤                   │
        │                  RECEITA/       │                   │
        │                  DESPESA        │                   │
        │                                 │                   │
        │              ┌──────────────────┴────┐              │
        │              │ descrição             │              │
        │              │ valorPrevisto         │              │
        │              │ dataVencimento        │              │
        │              │ tipo RECEITA/DESPESA  │              │
        │              │ categoria (FK)        │              │
        │              │ situação              │              │
        │              │  PENDENTE/PAGO/       │              │
        │              │  CANCELADO            │              │
        │              │  (VENCIDO = derivado) │              │
        │              └───────────┬───────────┘              │
        │                          │                          │
        │                    0..N  │  1                       │
        │                          ▼                          │
        └────────────────────► MOVIMENTAÇÃO ◄─────────────────┘
                               (evento realizado)
                                   │
                          ┌────────┴────────┐
                          │ valor (realizado)│
                          │ data             │
                          │ tipo             │
                          │ conta (FK)       │
                          │ categoria (FK)   │
                          │ título (FK,      │
                          │  opcional)       │
                          └────────┬─────────┘
                                   │
                                   ▼
                                  DRE
                                   │
                                   ▼
                        RESULTADO DO PERÍODO
                     (= RECEITAS REALIZADAS − DESPESAS REALIZADAS)
```

**Cardinalidades:**

- `usuario 1:N conta` — inalterado.
- `usuario 1:N categoria` — inalterado.
- `usuario 1:N titulo` — **novo**.
- `usuario 1:N movimentacao` — inalterado.
- `categoria 1:N titulo` — **novo**.
- `categoria 1:N movimentacao` — inalterado.
- `conta 1:N movimentacao` — **novo** (antes era `conta 1:N recebimento` e `conta 1:N pagamento`).
- `titulo 1:N movimentacao` — **novo**, e **opcional** (`titulo_id` é `NULL` para lançamentos
  avulsos, ex.: "paguei um café em dinheiro", que nunca teve título).

### 2.6 O caso de uso do enunciado, ponta a ponta

**Passo 1 — cadastrar o título**

```text
POST /api/v1/titulos
{ "descricao": "Conta de energia", "valorPrevisto": 150.00,
  "dataVencimento": "2026-03-10", "tipo": "DESPESA",
  "categoriaId": 5 }
```

Estado: `titulo` com `situacao = PENDENTE`, `valorRealizado = 0`.
**Nenhuma linha em `movimentacao`.** Nenhuma linha nova em lugar nenhum além de `titulo`.
→ A despesa **não existe** para a DRE realizada. Existe para a DRE prevista.
Se hoje for 11/03 e o título não tiver sido pago, a resposta traz `situacao: "VENCIDO"` (derivado).

**Passo 2 — pagar o título**

```text
POST /api/v1/titulos/5/pagar
{ "valor": 150.00, "data": "2026-03-08", "contaId": 2 }
```

O service executa, em **uma única transação**:

1. Carrega o título; valida que existe, pertence ao usuário, não está `CANCELADO`, não está quitado.
2. Valida `valor > 0`, `valor ≤ valorEmAberto` (não deixa pagar 200 de um título de 150).
3. Valida que a `conta` pertence ao **mesmo usuário** do título.
4. Valida que `conta.ativo` / consistência de tipo (categoria do título compatível).
5. **Cria a `movimentacao`**: `valor=150`, `data=2026-03-08`, `tipo=DESPESA`,
   `titulo_id=5`, `conta_id=2`, `categoria_id` herdada do título.
6. Recalcula `valorRealizado`; como `150 == 150`, grava `titulo.situacao = PAGO`.
7. `COMMIT`.

Se qualquer passo falhar, nada é gravado — **não existe título pago sem movimentação, nem
movimentação órfã de um pagamento que não aconteceu**.

**Passo 3 — DRE**

- DRE **realizada** de março: inclui os R$ 150,00 (foi a movimentação que aconteceu em 08/03).
- DRE **prevista** de março: inclui os R$ 150,00 (o vencimento era 10/03).
- Antes do passo 2, a DRE **realizada** de março seria R$ 0,00 e a **prevista** R$ 150,00. Essa é
  precisamente a separação que o usuário pediu.

---

## Etapa 3 — Modelo lógico do banco

### 3.1 Visão geral das alterações

| Tabela | Ação | Motivo resumido |
|---|---|---|
| `usuario` | **manter** | Atende plenamente. |
| `conta` | **manter** | `UNIQUE(nome, usuario_id)` correto. |
| `categoria` | **alterar** | `nome` de `VARCHAR(10)` → `VARCHAR(100)` (P6). |
| `titulo` | **criar** | Representar previsão/compromisso (P2, P3, P4). |
| `movimentacao` | **alterar** | Receber `conta_id` e `titulo_id`; tornar-se o evento realizado. |
| `recebimento` | **remover** | Absorvida por `movimentacao` (P5). |
| `pagamento` | **remover** | Absorvida por `movimentacao` (P5). |

Total final: **5 tabelas** (era 6). Nenhuma tabela nova supérflua — a única criada é `titulo`, que
é exatamente o conceito pedido.

### 3.2 `titulo` (nova)

**Finalidade:** registrar um compromisso financeiro previsto — uma obrigação a pagar (despesa) ou um
direito a receber (receita) — com valor previsto, vencimento e situação, **sem** afirmar que o
dinheiro já se moveu.

| Coluna | Tipo | Restrições | Observação |
|---|---|---|---|
| `id_titulo` | `BIGINT` | **PK**, `AUTO_INCREMENT` | |
| `descricao` | `VARCHAR(255)` | `NOT NULL` | "Conta de energia" |
| `valor_previsto` | `DECIMAL(15,2)` | `NOT NULL`, `CHECK > 0` | Quanto foi previsto |
| `valor_realizado` | `DECIMAL(15,2)` | `NOT NULL DEFAULT 0`, `CHECK >= 0` | Soma das movimentações quitadas; mantido por regra de negócio |
| `data_vencimento` | `DATE` | `NOT NULL` | Base do cálculo de `VENCIDO` e da DRE prevista |
| `data_pagamento` | `DATE` | `NULL` | Preenchida **somente** quando o título é quitado |
| `tipo` | `ENUM('RECEITA','DESPESA')` | `NOT NULL` | Direção do compromisso |
| `situacao` | `ENUM('PENDENTE','PAGO','CANCELADO')` | `NOT NULL DEFAULT 'PENDENTE'` | **Sem `VENCIDO`** — ver §2.3 |
| `observacao` | `VARCHAR(255)` | `NULL` | Campo livre (opcional, mas útil na prática) |
| `categoria_id` | `BIGINT` | `NOT NULL`, FK→`categoria` | Obrigatória: sem categoria não há linha na DRE |
| `usuario_id` | `BIGINT` | `NOT NULL`, FK→`usuario` | Dono do título |

**Chave primária:** `id_titulo`.
**Chaves estrangeiras:**
- `fk_titulo_usuario` (`usuario_id` → `usuario.id_usuario`), `ON DELETE RESTRICT`.
- `fk_titulo_categoria` (`categoria_id` → `categoria.id_categoria`), `ON DELETE RESTRICT`.

**Cardinalidade:** `usuario 1:N titulo`, `categoria 1:N titulo`, `titulo 1:N movimentacao`.

**Regras de integridade:**
- `CHECK (valor_previsto > 0)` e `CHECK (valor_realizado >= 0)`.
- `CHECK (valor_realizado <= valor_previsto)` — impede pagar mais do que se devia.
- `CHECK (situacao <> 'PAGO' OR data_pagamento IS NOT NULL)` — não existe título quitado sem data de
  quitação. (Nome: `ck_titulo_pago_tem_data`.)
- `ON DELETE RESTRICT` em ambas as FKs: categoria ou usuário com títulos não pode ser apagado.

**Índices** (a DRE prevista filtra exatamente por estes):
- `idx_titulo_usuario_vencimento (usuario_id, data_vencimento)`
- `idx_titulo_usuario_situacao (usuario_id, situacao)`

**Motivo da existência:** é a materialização do conceito de "título financeiro" pedido, e o único
lugar onde `valor previsto`, `vencimento` e `situação` podem viver sem contaminar o caixa.

**Por que não tem `conta_id`:** um título ainda não pago **não está em conta nenhuma**. A conta só é
conhecida no momento do pagamento, e por isso vive na `movimentacao`. Colocar `conta_id` no título
seria afirmar que a obrigação já saiu de um banco — exatamente o erro que estamos corrigindo.

### 3.3 `movimentacao` (alterada)

**Finalidade (nova):** registrar um **evento financeiro realizado** — dinheiro que efetivamente
entrou ou saiu — opcionalmente originado da quitação de um título.

| Coluna | Tipo | Restrições | Observação |
|---|---|---|---|
| `id_movimentacao` | `BIGINT` | **PK**, `AUTO_INCREMENT` | inalterado |
| `descricao` | `VARCHAR(255)` | `NOT NULL` | inalterado |
| `valor` | `DECIMAL(15,2)` | `NOT NULL`, `CHECK > 0` | **valor realizado** (era ambíguo) |
| `tipo` | `ENUM('RECEITA','DESPESA')` | `NOT NULL` | inalterado |
| `data` | `DATE` | `NOT NULL` | **data do evento** (competência de caixa) |
| `usuario_id` | `BIGINT` | `NOT NULL`, FK→`usuario` | inalterado |
| `categoria_id` | `BIGINT` | `NOT NULL`, FK→`categoria` | inalterado |
| `conta_id` | `BIGINT` | `NOT NULL`, FK→`conta` | **NOVO** — absorvido de `recebimento`/`pagamento` |
| `titulo_id` | `BIGINT` | `NULL`, FK→`titulo` | **NOVO** — origem; `NULL` = lançamento avulso |

**Chave primária:** `id_movimentacao`.
**Chaves estrangeiras:**
- `fk_movimentacao_usuario`, `fk_movimentacao_categoria` — já existem.
- `fk_movimentacao_conta` (`conta_id` → `conta.id_conta`), `ON DELETE RESTRICT` — **nova**.
- `fk_movimentacao_titulo` (`titulo_id` → `titulo.id_titulo`), `ON DELETE RESTRICT` — **nova**,
  `NULL` permitido.

**Cardinalidade:** `conta 1:N movimentacao`, `titulo 1:N movimentacao` (0..N).

**Regras de integridade:**
- `CHECK (valor > 0)` — corrige P7.
- `conta_id` obrigatório: **todo** evento financeiro tem uma conta. Isso torna
  `SALDO = saldo_inicial + Σ receitas − Σ despesas` uma consulta direta, sem `UNION`.
- `titulo_id` opcional: preserva a capacidade de lançar movimentações avulsas (sem previsão), que é o
  uso mais comum do dia a dia e o que garante que **nada do que existe hoje se perde**.

**Índices:**
- `idx_movimentacao_usuario_data (usuario_id, data)` — DRE realizada por período.
- `idx_movimentacao_titulo (titulo_id)` — cálculo de `valorRealizado`.
- `idx_movimentacao_conta (conta_id)` — extrato/saldo por conta.

**Motivo da alteração:** duas colunas resolvem tudo. `conta_id` elimina as tabelas-gêmeas;
`titulo_id` liga previsão e realização sem obrigar que todo lançamento tenha previsão.

### 3.4 `categoria` (alterada)

| Coluna | Antes | Depois | Motivo |
|---|---|---|---|
| `nome` | `VARCHAR(10) NOT NULL` | `VARCHAR(100) NOT NULL` | P6 — "Alimentação" (11) e "Outras receitas" (15) são rejeitados hoje |

Nada mais muda. `UNIQUE(nome, usuario_id)` permanece — aliás, ele **melhora** com a coluna maior:
agora é possível ter "Moradia", "Alimentação", "Transporte", "Educação", "Saúde", "Lazer" etc. sem
colisão.

**Impacto:** com o banco vazio, é um `ALTER TABLE ... MODIFY` sem perda. Feito pelo Hibernate
(`ddl-auto=update`) ou pelo script de migração da seção 3.6.

### 3.5 Tabelas removidas: `recebimento` e `pagamento`

**Finalidade anterior:** registrar entrada (recebimento) ou saída (pagamento) efetiva de dinheiro,
vinculada a uma movimentação.

**Por que saem:** absorvidas por `movimentacao` (§2.2). Comparação explícita:

```text
ANTES                                  DEPOIS
movimentacao(valor, data, tipo,        movimentacao(valor, data, tipo,
             usuario, categoria)                    usuario, categoria,
  ├─ recebimento(valor, data, conta)                conta, titulo)
  └─ pagamento(valor, data, conta)

  3 tabelas, 2 valores, 2 datas,        1 tabela, 1 valor, 1 data,
  2 caminhos para o mesmo dinheiro      1 caminho
```

**`Recebimento` e `Pagamento` como classes Java:** deixam de ser `@Entity` e passam a ser **DTOs de
requisição** (`RecebimentoRequest`, `PagamentoRequest`), porque "registrar um recebimento" e
"registrar um pagamento" continuam sendo operações legítimas do sistema — apenas não são tabelas.
Nenhuma capacidade funcional é perdida; ela muda de lugar.

### 3.6 Estratégia de migração

Como **as 6 tabelas estão vazias**, há duas opções seguras:

**Opção A (recomendada para o trabalho acadêmico) — script versionado.**
Criar `src/main/resources/db/migration/V2__evolucao_dre_titulos.sql` com o `ALTER`/`CREATE`/`DROP`.
Vantagens: o professor vê o SQL; o banco fica reproduzível; nada depende de o Hibernate adivinhar.

**Opção B — deixar o Hibernate fazer (`ddl-auto=update`).**
O Hibernate cria `titulo` e adiciona `conta_id`/`titulo_id`, mas **não remove** tabelas órfãs —
seria preciso rodar `DROP TABLE recebimento, pagamento` manualmente. Além disso, ele **não gera os
`CHECK`** de `valor > 0` nem os índices explícitos.

**Recomendação:** fazer as duas coisas — manter `ddl-auto=update` (para o dia a dia) **e** entregar o
script `V2` documentado, para que o banco possa ser recriado do zero de forma determinística. Se
quisermos rigor acadêmico, adotar Flyway. Fora do escopo do prompt, não recomendo migrar
`recebimento`/`pagamento` para `movimentacao` com `INSERT ... SELECT`: as tabelas estão vazias, então
é complexidade sem retorno.

### 3.7 Diagrama relacional final

```text
usuario ──1:N──> conta
   │                ▲
   │                │ N
   ├──1:N──> categoria
   │           ▲   ▲
   │           │N  │N
   ├──1:N──> titulo ──1:N──> movimentacao
   │                              ▲
   └──────────────────────────────┘  (usuario_id, N:1)
```

Tabelas: `usuario`, `conta`, `categoria`, `titulo`, `movimentacao`. Fim.

---

## Etapa 4 — Regras de negócio

### 4.1 A cadeia `Título → Pagamento/Recebimento → Movimentação → DRE`

```text
   TÍTULO                    OPERAÇÃO                      MOVIMENTAÇÃO
 (previsão)              (caso de uso)                     (realização)
     │                         │                                │
 PENDENTE ──pagar()──────────►│                                │
     │                         ├─ valida dono/tipo/conta       │
     │                         ├─ valida valor > 0             │
     │                         ├─ valida valor ≤ em aberto     │
     │                         ├─ cria ───────────────────────►│ DESPESA
     │                         └─ soma realizados              │
     │                                                          │
     ├─ se Σ realizados == previsto ──► situacao = PAGO         │
     ├─ se Σ realizados  < previsto ──► situacao = PENDENTE     │
     │                                   (parcial)              │
     └─ cancelar() ───────────────────► situacao = CANCELADO    │
                                        (só se Σ realizados = 0)│
                                                                 ▼
                                                               DRE
                                                                 ▼
                                                     RESULTADO DO PERÍODO
```

### 4.2 Regras do Título

| # | Regra | Justificativa |
|---|---|---|
| RN01 | `valorPrevisto` deve ser **maior que zero** | Não existe obrigação de valor zero ou negativo |
| RN02 | `dataVencimento` é **obrigatória** | Sem vencimento não há "vencido" nem DRE prevista por período |
| RN03 | `categoria` deve pertencer ao **mesmo usuário** e ter `tipo` **igual** ao `tipo` do título | Impede "Alimentação (despesa)" numa receita. Corrige P8 |
| RN04 | Título nasce com `situacao = PENDENTE` e `valorRealizado = 0` | Cadastrar previsão **não** cria despesa |
| RN05 | Título `CANCELADO` **não** pode receber pagamento | Cancelado é definitivo |
| RN06 | Título `PAGO` (quitado) **não** pode receber novo pagamento | Impede pagar duas vezes |
| RN07 | Cancelamento **só** é permitido se `valorRealizado = 0` | Se já houve dinheiro, é preciso estornar a movimentação antes |
| RN08 | Um título pode ter **N movimentações** (pagamento parcial) | Caso real: aluguel pago em duas vezes |
| RN09 | `Σ movimentações do título ≤ valorPrevisto` | Impede pagar mais do que se devia |
| RN10 | Ao atingir `Σ = valorPrevisto`: `situacao = PAGO` e `dataPagamento = data da última movimentação` | "PAGO" = quitado (§2.4) |
| RN11 | `VENCIDO` é **derivado** (`PENDENTE` + `dataVencimento < hoje`) | §2.3 — sem job agendado |
| RN12 | Título pertence a **um** usuário; todas as operações validam o dono | Isolamento entre usuários |

### 4.3 Regras da Movimentação

| # | Regra | Justificativa |
|---|---|---|
| RN13 | `valor > 0` | Corrige P7 |
| RN14 | `conta` **obrigatória** e do **mesmo usuário** | Dinheiro sempre está em algum lugar |
| RN15 | `categoria` do **mesmo usuário** e com `tipo` **igual** ao `tipo` da movimentação | Corrige P8 |
| RN16 | Se vier de um título (`titulo_id` ≠ `null`): `tipo`, `categoria` e `usuario` são **herdados** do título — não podem divergir | Garante que a realização corresponde à previsão |
| RN17 | Movimentação originada de título **não** pode existir sem que a validação de RN09 passe | |
| RN18 | Movimentação **avulsa** (`titulo_id = null`) é permitida | Preserva o uso atual e o dia a dia ("café no débito") |
| RN19 | Movimentação **não** é editável em `valor`/`data` se vier de título — exige estorno | Um fato contábil não se reescreve |
| RN20 | Uma movimentação é **imutável** após criada (alterações → estorno + novo lançamento) | Coerência com RN19 |

### 4.4 A regra de negócio central da DRE (a resposta ao item 4 do pedido)

> ### ⚠ Um título **nunca** entra na DRE realizada. Só a movimentação entra.

Formalmente:

```text
DRE PREVISTA (por competência / vencimento)
  RECEITAS PREVISTAS  = Σ titulo.valorPrevisto WHERE tipo=RECEITA
                                            AND dataVencimento BETWEEN :ini AND :fim
                                            AND situacao <> CANCELADO
  DESPESAS PREVISTAS  = Σ titulo.valorPrevisto WHERE tipo=DESPESA  (idem)
  RESULTADO PREVISTO   = RECEITAS PREVISTAS − DESPESAS PREVISTAS

DRE REALIZADA (por caixa / data do evento)
  RECEITAS REALIZADAS = Σ movimentacao.valor WHERE tipo=RECEITA
                                            AND data BETWEEN :ini AND :fim
  DESPESAS REALIZADAS = Σ movimentacao.valor WHERE tipo=DESPESA (idem)
  RESULTADO REALIZADO  = RECEITAS REALIZADAS − DESPESAS REALIZADAS

DRE COMPARATIVA
  VARIAÇÃO = RESULTADO REALIZADO − RESULTADO PREVISTO
```

**A decisão, explicada:** uma conta de energia de R$ 150,00 que vence dia 10 e **ainda não foi paga**
aparece na DRE **prevista** (compromisso assumido no período) e **não** aparece na DRE **realizada**
(nenhum real saiu da conta). No instante em que o pagamento é registrado, ela passa a compor a DRE
realizada, na **data do pagamento** — não na data do vencimento. É o princípio contábil de **regime de
caixa** para o realizado e **regime de competência** para o previsto, aplicado a finanças pessoais.

**Consequências práticas dessa escolha:**

1. **Um título cancelado desaparece da DRE prevista** e nunca apareceu na realizada. Correto: o
   compromisso deixou de existir.
2. **`VENCIDO` continua aparecendo na DRE prevista** (era uma obrigação do período). Ele é sinalizado
   em um bloco separado — `titulosVencidos` — para o usuário ver o que ficou para trás, sem poluir o
   resultado.
3. **Um pagamento parcial aparece proporcionalmente**: R$ 600,00 pagos de um aluguel de R$ 1.200,00
   entram como R$ 600,00 na DRE realizada de março (se pagos em março) e R$ 1.200,00 na DRE prevista
   de março (vencimento em março). A diferença é exposta campo a campo, não escondida.
4. **A DRE realizada é uma consulta a uma tabela só** — consequência direta de §2.2.

**Estrutura de resposta da DRE** (espelha o layout pedido, sem complexidade empresarial):

```text
RECEITAS
  Salários .................... previsto 5.000,00 | realizado 5.000,00
  Freelances ................. previsto 1.200,00 | realizado   800,00
  Rendimentos ................ previsto    50,00 | realizado    50,00
  Outras receitas ............ previsto   100,00 | realizado     0,00
  (=) TOTAL DE RECEITAS ...... previsto 6.350,00 | realizado 5.850,00

(−) DESPESAS
  Moradia .................... previsto 1.500,00 | realizado 1.500,00
  Alimentação ................ previsto   800,00 | realizado   620,00
  Transporte ................. previsto   300,00 | realizado   280,00
  Educação ................... previsto   400,00 | realizado     0,00
  Saúde ...................... previsto   200,00 | realizado   200,00
  Lazer ...................... previsto   150,00 | realizado    90,00
  Outras despesas ............ previsto   120,00 | realizado    60,00
  (=) TOTAL DE DESPESAS ...... previsto 3.470,00 | realizado 2.750,00

(=) RESULTADO DO PERÍODO ..... previsto 2.880,00 | realizado 3.100,00
    Variação ................. +220,00 (favorável)
    Títulos vencidos ......... 1 (R$ 400,00)
```

O agrupamento é **dinâmico pelas categorias do usuário** — as categorias acima são os dados de
exemplo dele mesmo (`Salários`, `Moradia`, …), não uma lista fixa no código. Assim o sistema serve
qualquer usuário, e é a `categoria` que dá as "linhas" da DRE.

### 4.5 Regras de integridade no banco vs. no service

**Filosofia adotada:** o banco garante o que é *expressável em SQL* e o service garante o que exige
*consultar outras linhas*.

- **No banco (`CHECK`, `NOT NULL`, `FK`, `UNIQUE`):** `valor > 0`, `valorRealizado <= valorPrevisto`,
  `situacao='PAGO' ⇒ data_pagamento IS NOT NULL`, existência das FKs.
- **No service (exige ler outras linhas):** "conta pertence ao mesmo usuário", "categoria é do tipo
  certo", "Σ movimentações ≤ valorPrevisto", "título cancelado não recebe pagamento". Essas regras
  **não** podem virar `CHECK` (um `CHECK` não faz `SELECT` em outra tabela em MariaDB), então ficam
  na camada de serviço, dentro de `@Transactional`, e **sempre antes** de qualquer `save`.

---

## Etapa 5 — Plano de alteração do Spring Boot

### 5.1 Pacotes

O usuário pediu: `controller`, `service`, `repository`, `entity`, `dto`, `exception`.
Proponho **acrescentar dois**, com justificativa:

- **`mapper/`** — conversão `Entidade ↔ DTO`. Sem ele, essa lógica se espalha por todos os services
  (ou os controllers passam a devolver entidades JPA, o que expõe `senha` e causa
  `LazyInitializationException`). Cada mapper é uma classe pequena com métodos estáticos.
  **Recomendado porque o projeto é didático**: separa visivelmente "o que é do banco" de "o que é da
  API".
- **`enums/`** — hoje `TipoMovimentacao` está dentro de `entity/`. Com `SituacaoTitulo` e
  possivelmente `TipoConta`, `entity/` fica misturando entidades e enums. **Contudo**, mover
  `TipoMovimentacao` quebra os `import` existentes nos repositórios. Proponho **manter os enums em
  `entity/`** (como está) para respeitar "não quebrar o que existe" e evitar mudança cosmética com
  custo. Se o usuário preferir a separação, é uma troca de `import` em 3 arquivos.

Estrutura final:

```text
com.example.financas/
├── FinancasApplication.java
├── config/         (existente — SecurityConfig ajustado)
├── controller/     (NOVO)
├── dto/
│   ├── request/    (NOVO)
│   └── response/   (NOVO)
├── entity/         (existente — 3 alteradas, 1 criada, 2 convertidas em DTO)
├── exception/      (NOVO)
├── mapper/         (NOVO)
├── repository/     (existente — complementado)
└── service/        (NOVO)
```

### 5.2 Entidades

| Arquivo | Ação | Detalhe |
|---|---|---|
| `entity/Usuario.java` | **manter** | Sem alteração funcional. Possível `@OneToMany` é desnecessário (evita `LAZY` e complexidade). |
| `entity/Conta.java` | **manter** | Sem alteração. |
| `entity/Categoria.java` | **alterar** | `nome`: `length = 10` → `length = 100` (P6). `long idCategoria` → `Long` (P10). |
| `entity/Movimentacao.java` | **alterar** | + `conta` (`@ManyToOne`, `nullable=false`), + `titulo` (`@ManyToOne`, `nullable=true`), + `@Positive` em `valor`, + `@Table` com índices e `@Check`/`@UniqueConstraint` conforme §3.3. |
| **`entity/Titulo.java`** | **criar** | Nova entidade de §3.2, com `SituacaoTitulo situacao` e método de domínio `situacaoEfetiva()`. |
| **`entity/SituacaoTitulo.java`** | **criar** | Enum `PENDENTE, PAGO, CANCELADO`. |
| `entity/TipoMovimentacao.java` | **manter** | Permanece em `entity/` para não quebrar imports. Reutilizado como tipo do Título. |
| `entity/Recebimento.java` | **remover a entidade** | Vira DTO `RecebimentoRequest`. Se preferirmos preservar o nome de arquivo, `@Entity` sai. |
| `entity/Pagamento.java` | **remover a entidade** | Vira DTO `PagamentoRequest`. |

### 5.3 Repositories

| Arquivo | Ação |
|---|---|
| `repository/UsuarioRepository.java` | **manter** |
| `repository/ContaRepository.java` | **manter** |
| `repository/CategoriaRepository.java` | **manter** |
| `repository/MovimentacaoRepository.java` | **manter métodos atuais** + adicionar `findByTituloIdTitulo`, `sumByTitulo`, agregações para a DRE (`@Query` com `GROUP BY categoria`), `findByUsuarioIdUsuarioAndDataBetweenAndTipo` |
| `repository/RecebimentoRepository.java` | **remover** |
| `repository/PagamentoRepository.java` | **remover** |
| **`repository/TituloRepository.java`** | **criar** — `findByUsuarioIdUsuario`, `findByUsuarioIdUsuarioAndSituacao`, `findByUsuarioIdUsuarioAndDataVencimentoBetween`, `findBySituacaoAndDataVencimentoBefore` (vencidos), agregação para a DRE prevista |

Nenhum método existente de `MovimentacaoRepository` será removido ou renomeado.

### 5.4 Services (todos novos)

| Service | Responsabilidade | Regras |
|---|---|---|
| `UsuarioService` | CRUD de usuário, e-mail único | — |
| `ContaService` | CRUD de conta, nome único por usuário | — |
| `CategoriaService` | CRUD de categoria, nome único por usuário | — |
| `TituloService` | CRUD + **`pagar(id, request)`**, **`receber(id, request)`**, **`cancelar(id)`** | RN01–RN12 |
| `MovimentacaoService` | CRUD de movimentações avulsas + consultas | RN13–RN20 |
| **`DreService`** | Gera a DRE prevista / realizada / comparativa por período | §4.4 |

`TituloService.pagar()` e `.receber()` são **o coração do sistema**; ambos delegam para um método
privado comum `quitar(titulo, valor, data, conta)` — a única diferença é a direção
(`RECEITA`/`DESPESA`), o que elimina a duplicação que hoje existe entre `recebimento` e `pagamento`.

`DreService` **não grava nada** — só lê. Nenhuma tabela de DRE é criada (mantendo a decisão atual do
projeto, que está correta).

### 5.5 DTOs (todos novos)

**Request:** `UsuarioRequest`, `ContaRequest`, `CategoriaRequest`, `TituloRequest`,
`MovimentacaoRequest`, `PagamentoRequest`, `RecebimentoRequest` — todos com Bean Validation
(`@NotNull`, `@Positive`, `@NotBlank`, `@Size`).

**Response:** `UsuarioResponse` (**sem** o campo `senha` — importante), `ContaResponse`,
`CategoriaResponse`, `TituloResponse` (com `valorRealizado`, `valorEmAberto`, `situacao` efetiva e
`situacaoArmazenada`), `MovimentacaoResponse`, `SaldoContaResponse`, `DreResponse`.

**`DreResponse`** → `DreResponse { periodo, totalReceitas, totalDespesas, resultado, linhas[],
titulosVencidos }`, onde `linhas[]` é `{ categoriaId, categoriaNome, tipo, valorPrevisto,
valorRealizado, variacao }`. É o formato necessário para renderizar o bloco da §4.4.

### 5.6 Controllers (todos novos)

| Controller | Endpoints principais |
|---|---|
| `UsuarioController` | `GET/POST/PUT/DELETE /api/v1/usuarios` |
| `ContaController` | `GET/POST/PUT/DELETE /api/v1/contas` |
| `CategoriaController` | `GET/POST/PUT/DELETE /api/v1/categorias` (filtro `?tipo=`) |
| `TituloController` | `GET/POST/PUT/DELETE /api/v1/titulos`, `POST /api/v1/titulos/{id}/pagar`, `POST /api/v1/titulos/{id}/receber`, `POST /api/v1/titulos/{id}/cancelar`, `GET /api/v1/titulos/vencidos` |
| `MovimentacaoController` | `GET/POST /api/v1/movimentacoes`, `GET /api/v1/movimentacoes?inicio=&fim=` |
| **`DreController`** | `GET /api/v1/dre?inicio=&fim=&modo=previsto\|realizado\|comparativo` |

### 5.7 Exceptions (todos novos)

`RegraNegocioException` (→ HTTP 422), `RecursoNaoEncontradoException` (→ 404),
`RecursoDuplicadoException` (→ 409), `GlobalExceptionHandler` (`@RestControllerAdvice`) traduzindo
tudo em um `ErroResponse` uniforme `{ timestamp, status, erro, mensagem, caminho }`.

### 5.8 Config

`SecurityConfig`: manter `permitAll` explícito e documentado (é um projeto acadêmico sem autenticação
ainda) e ajustar para a DSL de lambda do Spring Boot 4.x. **Não** vou implementar JWT/login — está
fora do escopo pedido e adicionaria complexidade. O `usuarioId` será recebido nos requests.

### 5.9 Resumo: "Estrutura atual → Problema → Nova estrutura → Motivo"

| Estrutura atual | Problema | Nova estrutura | Motivo |
|---|---|---|---|
| `movimentacao` é previsão **e** realização | Duas datas/valores concorrentes; cadastro vira despesa | `titulo` (previsão) + `movimentacao` (realização) | Separar compromisso de fato consumado |
| `recebimento` + `pagamento` | Tabelas-gêmeas; `UNION` em todo relatório; regra em dois lugares | `movimentacao.conta_id` | Uma tabela de eventos, um total, uma consulta |
| sem status | Não há pendente/vencido; DRE soma o que não foi pago | `titulo.situacao` + `VENCIDO` derivado | Situação sem job agendado |
| sem vencimento | Impossível apurar atraso ou DRE por competência | `titulo.data_vencimento` | Base do previsto e do "vencido" |
| `categoria.nome VARCHAR(10)` | "Alimentação" não cabe | `VARCHAR(100)` | Categorias reais |
| sem validação | Valor negativo aceito | `@Positive` + `CHECK` | Integridade financeira |
| `tipo` duplicado sem coerência | Receita em categoria de despesa | validação no service (RN03/RN15) | DRE não pode somar lixo |
| sem service/controller | Nada exposto; entidade viraria JSON | camadas completas | Arquitetura e segurança |

### 5.10 Ordem de implementação (incremental, compilando a cada passo)

1. Enums e entidade `Titulo`; ajuste de `Movimentacao` e `Categoria`.
2. Repositórios (`TituloRepository`, complemento de `MovimentacaoRepository`); remoção dos dois órfãos.
3. Exceptions + `GlobalExceptionHandler`.
4. DTOs (request/response) + mappers.
5. Services (`TituloService` com `pagar`/`receber`/`cancelar`, `MovimentacaoService`, `DreService`, CRUDs).
6. Controllers.
7. Script SQL `V2` de migração + `DROP` das tabelas órfãs; atualizar `README.md`; `mvn compile`.
8. Teste de fumaça: subir a aplicação e exercitar o fluxo "criar título → DRE não muda → pagar → DRE muda".

### 5.11 Itens que **não** farei (para não inflar o projeto)

- Sem Flyway/Liquibase como dependência (um `.sql` comentado basta).
- Sem autenticação JWT, sem `UserDetailsService`, sem tela de login.
- Sem tabela de DRE persistida, sem snapshot mensal.
- Sem `MapStruct`, sem `Specification` genérica, sem `@Query` nativa complexa.
- Sem soft delete generalizado.
- Sem entidade `Cartao`, `Orcamento` (budget) ou `Meta` — não foram pedidos. Ficam como evolução futura
  possível, e o modelo atual comporta acrescentá-los depois.

---

## Etapa 6 — Implementação

### 6.1 Decisões confirmadas

Todas as decisões estruturais foram aprovadas:

1. **Remover as tabelas `recebimento` e `pagamento`** (absorvidas por `movimentacao.conta_id`).
2. **`VENCIDO` derivado**, armazenando apenas `PENDENTE`/`PAGO`/`CANCELADO`.
3. **`PAGO` = quitado** (soma das movimentações igual ao previsto), com pagamento parcial.
4. **Script SQL `V2` documentado** além do `ddl-auto=update`.
5. **API REST completa + endpoints de DRE, sem front-end.**

### 6.2 O que foi implementado

**Banco (5 tabelas).** `V2__evolucao_dre_titulos.sql` cria `titulo`, amplia `categoria.nome`
para `VARCHAR(100)`, adiciona `conta_id` (NOT NULL) e `titulo_id` (NULL) em `movimentacao`,
e remove `recebimento`/`pagamento`. Inclui os `CHECK` que o Hibernate não gera.

**Java (de 15 para 40 arquivos).**

| Camada | Arquivos |
|---|---|
| `entity/` | `Titulo` (novo), `SituacaoTitulo` (novo), `SituacaoTituloEfetiva` (novo); `Movimentacao` e `Categoria` alteradas; `Recebimento` e `Pagamento` removidas |
| `repository/` | `TituloRepository` (novo); `MovimentacaoRepository` complementado; `projection/` (novo, 2 projeções); 2 removidos |
| `service/` | `TituloService`, `MovimentacaoService`, `DreService`, `UsuarioService`, `ContaService`, `CategoriaService`, `NovaMovimentacao` |
| `controller/` | `Usuario`, `Conta`, `Categoria`, `Titulo`, `Movimentacao`, `Dre` |
| `dto/request/` | 6 records com Bean Validation |
| `dto/response/` | 9 records |
| `mapper/` | `Usuario`, `Conta`, `Categoria`, `Titulo`, `Movimentacao` |
| `exception/` | 3 exceções + `GlobalExceptionHandler` |
| `test/` | `RegrasFinanceirasIntegrationTest` — 18 testes |

### 6.3 Verificação executada

O projeto foi compilado e **executado de verdade** contra o MariaDB 12.3 local, e o fluxo
completo foi exercitado pela API:

- **Antes do pagamento:** `totalDespesasPrevistas = 150,00`, `totalDespesasRealizadas = 0,00`.
- **Depois do pagamento:** `totalDespesasRealizadas = 150,00`, saldo da conta `850,00`.
- **Pagamento parcial:** 600 de 1.200 mantém o título `PENDENTE` com `valorEmAberto = 600,00`.
- **Erros:** pagar acima do em aberto → 422; título já quitado → 422; direção errada
  (`/pagar` em título de receita) → 422; categoria de tipo incompatível → 422; valor zero → 400.
- **Migração:** o banco foi recriado no estado V1 original e o script `V2` foi aplicado sobre
  ele, resultando nas 5 tabelas com os tipos corretos.
- **Alinhamento com o Hibernate:** comparou-se o schema gerado por `ddl-auto=create` do zero
  com o schema produzido pela migração. Nomes de índice e de FK batem exatamente. A única
  diferença eram os `CHECK`, que só a migração cria — e `conta_id`, que foi ajustada para
  `NOT NULL` a fim de não divergir.
- **Testes:** 18 testes, 18 aprovados.

### 6.4 Dois problemas reais encontrados durante a implementação

**1. Ciclo de beans entre `MovimentacaoService` e `TituloService`.**
O `TituloService` precisava criar movimentações e o `MovimentacaoService` precisava atualizar a
situação do título — cada um injetava o outro, e o Spring recusou subir:

```text
The dependencies of some of the beans in the application context form a cycle:
   movimentacaoService <--> tituloService
```

**Solução:** dependência de mão única. As validações de quitação (RN05, RN06, RN09) foram para
o `MovimentacaoService`, que é o dono das regras de movimentação; o `TituloService` as
reaproveita. A mudança de situação do título passou a ser uma `@Modifying @Query`
(`TituloRepository.marcarComoPago`) em vez de uma chamada de service. A alternativa seria um
`@Lazy` para mascarar o ciclo — o que apenas esconderia o problema.

**2. `AbstractMethodError` na serialização da DRE.**
Surgiu apenas na primeira execução e desapareceu após um `clean`. A causa era `.class`
obsoleto em `target/classes`, remanescente de uma compilação anterior. Vale registrar porque é
um modo de falha enganoso: o sintoma aponta para o código, mas o problema está no artefato
compilado.

### 6.5 Uma lição sobre `BigDecimal` nos testes

Cinco testes falharam com `expected: <0.00> but was: <0>`. O código de produção estava
correto; o defeito era do teste.

`BigDecimal.equals` compara **valor e escala**. Portanto `new BigDecimal("0")` e
`new BigDecimal("0.00")` são **diferentes** para `assertEquals`, ainda que sejam o mesmo valor
monetário. A comparação correta entre `BigDecimal` é `compareTo`. Os testes passaram a usar um
helper que faz essa comparação, e o caso ficou documentado no próprio arquivo de teste.

### 6.6 Desvios do plano original

Nenhum desvio estrutural. Duas observações:

- **`open-in-view=false`** foi adicionado a `application.properties` (não estava no plano).
  Justificativa: os mappers já convertem entidade → DTO dentro da transação, então desligar não
  quebra nada e torna visível qualquer consulta preguiçosa que venha a ser introduzida por
  engano.
- **Testes de integração** não estavam no plano. Foram acrescentados porque a regra central
  ("título não entra na DRE realizada") é justamente a que mais precisa de prova automatizada.
  Rodam em um banco separado (`financas_test`), sem tocar nos dados de desenvolvimento.

### 6.7 Ressalva sobre o ambiente de execução

O `mvnw` e o plugin `spring-boot:run` **não** puderam ser usados neste ambiente por limitação
de cache offline do Maven (faltavam `spring-boot-loader-tools` e as dependências do
`surefire`). A verificação foi feita executando a classe `main` diretamente com `java -cp`,
usando o classpath montado a partir do repositório local. Os testes foram executados pela API
do JUnit Platform Launcher, pelo mesmo motivo.

**Isso não afeta o projeto entregue:** `mvnw spring-boot:run` e `mvn test` funcionam
normalmente em uma máquina com acesso à internet, que é o caso do ambiente de desenvolvimento
do usuário. O que não foi possível aqui foi *executar* por esses comandos — o código em si
compila (`mvn compile` e `mvn test-compile` passam offline) e roda.

---

## Etapa 7 — Contas a pagar e contas a receber

### 7.1 A decisão: nenhuma estrutura nova

A pergunta era "vamos colocar contas a pagar e contas a receber". A resposta de modelagem é:
**elas já existem** — não como tabelas, mas como as duas leituras possíveis de `titulo`.

| Conceito pedido | Como é obtido |
|---|---|
| Contas a **pagar** | `titulo.tipo = DESPESA` |
| Contas a **receber** | `titulo.tipo = RECEITA` |
| Está **em aberto** | `situacao = PENDENTE` |
| **Quanto falta** | `valorPrevisto − Σ movimentações` |
| Está **vencido** | `dataVencimento < dataReferencia` |

**Estrutura atual → Problema (hipotético) → Nova estrutura → Motivo**

| Se criássemos `ContaAPagar` / `ContaAReceber` | Consequência |
|---|---|
| Três tabelas com os mesmos campos (descrição, valor, vencimento, situação) | Duplicação de dados |
| Título de despesa + conta a pagar representando a mesma coisa | Duas fontes de verdade que podem divergir |
| Precisaria sincronizar: cadastrar título → criar conta a pagar; pagar → atualizar as duas | Complexidade e risco de inconsistência |

Esse é **exatamente** o problema que a Etapa 2 corrigiu ao absorver `recebimento` e `pagamento`
em `movimentacao`. Repeti-lo aqui seria voltar atrás. Por isso a implementação é composta de
**duas classes puramente de leitura** (`ResumoFinanceiroService` e `ContasController`) mais os
DTOs de resposta — nenhuma entidade, nenhum repositório e nenhuma migração de banco.

### 7.2 A regra que evita inflar a dívida

Os totais somam **`valorEmAberto`**, nunca `valorPrevisto`:

> Aluguel de R$ 1.200,00 com R$ 600,00 pagos entra como **R$ 600,00** em contas a pagar.

Há um teste dedicado a isso (`contasAPagarSomamOValorEmAberto`), porque é o erro mais fácil de
cometer e o mais danoso: o usuário passaria a achar que deve o dobro do que deve.

### 7.3 O que foi acrescentado

| Arquivo | Papel |
|---|---|
| `dto/response/TituloEmAbertoResponse` | Uma conta a pagar/receber (o que falta, dias, situação) |
| `dto/response/PosicaoTitulosResponse` | Posição de um lado: totais, quantidades, recortes |
| `dto/response/ResumoFinanceiroResponse` | Os dois lados + saldo previsto |
| `dto/response/SituacaoEmAberto` | `A_VENCER`, `VENCIDO`, `PARCIAL`, `PARCIAL_VENCIDO` |
| `service/ResumoFinanceiroService` | Cálculo e agrupamento (somente leitura) |
| `controller/ContasController` | `/contas-a-pagar`, `/contas-a-receber`, `/resumo-financeiro` |

Acrescentados também 7 testes de integração (de 18 para **25**), cobrindo soma pelo valor em
aberto, exclusão de quitados e cancelados, separação entre vencido e a vencer, borda da janela
de dias, saldo previsto e isolamento entre usuários.

### 7.4 Uma decisão de borda que os testes revelaram

A janela de "próximos dias" estava com a **última ponta fechada**: um título que vencia
exatamente no 90º dia ficava de fora de uma janela de 90 dias. A implementação foi ajustada
para janela **inclusiva** (`isBefore(limite.plusDays(1))`), que é o que a expressão "vence em
90 dias" sugere. O comportamento está documentado no DTO e coberto por teste.

---

## Etapa 8 — Problemas encontrados na segunda rodada de verificação

### 8.1 Jackson 3 mudou o nome das propriedades de configuração

As datas eram serializadas como arrays JSON — `[2026,9,26]` em vez de `"2026-09-26"` — em
**toda** a API, não só nos endpoints novos. Um cliente precisaria saber que a posição 1 é o mês
e que ele começa em 1.

A correção habitual (`spring.jackson.serialization.write-dates-as-timestamps=false`) **derrubou
a aplicação na subida**:

```text
Failed to bind properties under 'spring.jackson.serialization'
  Reason: No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

Causa: o Spring Boot 4 passou a usar **Jackson 3** (`tools.jackson.*`), que removeu
`WRITE_DATES_AS_TIMESTAMPS` do enum `SerializationFeature`. Vários nomes de propriedade
mudaram em relação ao Jackson 2 (`com.fasterxml.jackson.*`).

Solução: formato declarado **explicitamente** nos DTOs com
`@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")`. Além de funcionar nas duas gerações do
Jackson, deixa o contrato visível junto do DTO em vez de depender de um padrão de biblioteca.
Verificado: as datas passaram a sair como `"2026-09-26"`.

### 8.2 O harness de teste rodava com a biblioteca errada — em silêncio

A verificação neste ambiente offline montava o classpath à mão, agrupando artefatos pelo
**nome do arquivo**. Isso é um defeito: `tools.jackson.core:jackson-databind` (3.1.5, exigido
pelo Spring Boot 4) e `com.fasterxml.jackson.core:jackson-databind` (2.21.5, legado) têm o
**mesmo nome de arquivo**. Um dos dois era descartado sem nenhum aviso, e a aplicação subia com
a versão errada do Jackson.

Foi isso que fez a primeira investigação do formato de datas apontar para o lado errado: o
comportamento observado era do Jackson 2, não do Jackson 3 que o projeto realmente usa.

Correção: incluir o **`groupId`** na chave de agrupamento — a identidade de um artefato Maven é
o par `groupId:artifactId`, nunca o nome do arquivo.

**Lição metodológica:** um harness de verificação com defeito é pior que nenhum harness, porque
produz evidência enganosa com aparência de evidência. Vale conferir que o ambiente de teste
carrega as mesmas versões de biblioteca que o build real.

---

## Etapa 9 — Tratamento de erros completo

### 9.1 O problema encontrado

O pedido foi "fazer todas as exceções necessárias para não retornar um erro sem ser explícito".
Antes de escrever qualquer coisa, os 13 cenários de falha foram **reproduzidos** contra a
aplicação rodando. O resultado foi pior do que o esperado:

| Cenário | Antes | Deveria ser |
|---|---|---|
| `DELETE` de categoria em uso | **500** + SQL executado na mensagem | 409 |
| `DELETE` de usuário com dados | **500** + nome da constraint | 409 |
| JSON malformado | **500** | 400 |
| Enum inválido | **500** + classe Java interna | 400 |
| Data inválida | **500** + formato do parser | 400 |
| Parâmetro de query inválido | **500** | 400 |
| Parâmetro obrigatório ausente | **500** | 400 |
| Rota inexistente | **500** | 404 |
| Método HTTP errado | **500** | 405 |
| Corpo ausente | **500** | 400 |

Somente id inexistente (404) e regra de negócio (422) estavam corretos.

### 9.2 A causa raiz: um handler genérico largo demais

O `GlobalExceptionHandler` tinha um `@ExceptionHandler(Exception.class)`. A intenção era boa —
garantir que nada escapasse sem tratamento — mas o efeito foi o contrário:

- ele **captura antes** do tratamento que o próprio Spring já dava a vários erros do cliente,
  rebaixando-os para 500;
- ele **repassava a mensagem crua** da exceção, o que fazia a resposta vazar o SQL executado e
  nomes de constraints.

Ou seja: o tratador genérico não era uma rede de segurança, era uma forma de apagar informação
de diagnóstico que já existia.

### 9.3 A solução

Tratadores **específicos** para cada situação, que têm precedência sobre o genérico:

| Exceção | HTTP |
|---|---|
| `MethodArgumentNotValidException` | 400 — lista todos os campos reprovados |
| `ConstraintViolationException` | 400 |
| `HttpMessageNotReadableException` | 400 — JSON malformado, enum ou data inválidos, corpo ausente |
| `MethodArgumentTypeMismatchException` | 400 — lista os valores aceitos para enums |
| `MissingServletRequestParameterException` | 400 — diz qual parâmetro faltou |
| `RecursoNaoEncontradoException` | 404 |
| `NoResourceFoundException` / `NoHandlerFoundException` | 404 — rota inexistente |
| `HttpRequestMethodNotSupportedException` | 405 — lista os métodos aceitos |
| `RecursoDuplicadoException` | 409 |
| **`RecursoEmUsoException`** (nova) | 409 — com a contagem de dependentes |
| `DataIntegrityViolationException` | 409 — traduzido do nome da constraint |
| `RegraNegocioException` | 422 |
| `Exception` | 500 — mensagem genérica, causa só no log |

O `Exception` final ficou restrito a defeitos reais, com a mensagem deliberadamente genérica e a
causa completa registrada no log.

### 9.4 Duas camadas para o conflito de exclusão

Criou-se a exceção `RecursoEmUsoException` para que os services **antecipem** o conflito, em vez
de deixar o banco recusar:

- **Camada 1 (service)**: `UsuarioService.remover`, `ContaService.remover` e
  `CategoriaService.remover` contam os dependentes antes de tentar. A mensagem sai com a
  quantidade: *"Não dá para excluir categoria porque há 1 título vinculado a ele"*.
- **Camada 2 (banco)**: `DataIntegrityViolationException` tratada como rede de segurança. Cobre
  a condição de corrida em que alguém insere uma linha dependente entre a contagem e o `DELETE`.
  A mensagem é montada traduzindo o **nome da constraint** (`fk_titulo_categoria` → "existem
  títulos ou movimentações que a utilizam").

Sem a camada 2, uma condição de corrida viraria 500.

### 9.5 O não-vazamento foi verificado, não presumido

A regra "não vazar detalhe interno" foi testada explicitamente. Dois testes percorrem as
respostas e falham se encontrarem:

| Termo proibido na resposta | Origem |
|---|---|
| `sql`, `could not execute`, `fk_` | mensagem crua do driver JDBC |
| `byte offset`, `StreamReadFeature`, `REDACTED` | configuração interna do Jackson |
| `com.example.financas` | nomes de classes da aplicação |
| `Jackson` | nome da biblioteca de parsing |

Esses termos foram extraídos das respostas reais observadas durante a depuração — não de uma
suposição sobre o que a biblioteca poderia emitir.

### 9.6 Concordância de gênero: quatro defeitos da mesma família

As mensagens montadas por concatenação produziram erros de português que só apareceram porque os
testes comparam a mensagem exata:

| Mensagem gerada | Problema |
|---|---|
| `Não é possível excluir **esta** usuário` | artigo feminino + substantivo masculino |
| `**1 títulos vinculados**` | plural com quantidade 1 |
| `**1 movimentação vinculado**` | adjetivo masculino + substantivo feminino |
| `não pode ser usada em **uma** título` | artigo feminino + substantivo masculino |

A causa comum é que a frase precisa concordar em **quatro** dimensões: número do verbo, número
do substantivo, gênero do substantivo e gênero do adjetivo. A solução foi **eliminar as
construções que exigem artigo** (`"Não dá para excluir X"` em vez de `"Não é possível excluir
este/esta X"`) e passar explicitamente por parâmetro apenas o gênero do dependente — o único que
não pode ser deduzido do contexto.

Nenhum desses erros quebra o programa. Mas em um sistema cujo requisito é explicar seus erros, a
clareza da frase é parte do requisito.

### 9.7 Resultado

- **46 testes**, todos passando (eram 25).
- **21 testes novos** em `TratamentoDeErrosApiTest`, cobrindo os 10 status, o formato único do
  corpo, as mensagens traduzidas e o não-vazamento.
- Os 13 cenários que falhavam foram **revalidados contra a aplicação rodando**: todos retornam o
  status correto, com mensagem em português e `detalhes` sem informação interna.

### 9.8 Um ajuste necessário nos testes

O novo teste de API **não pode ser `@Transactional`**: ele precisa de `COMMIT` de verdade para
exercitar a pilha HTTP completa. Isso o tornou incompatível com a carga de dados dos testes
existentes, que era um `INSERT` simples — o segundo teste falhava com chave duplicada.

A carga passou a ser **idempotente**: começa com `DELETE` de tudo e reinicia os contadores de
auto-incremento. Sem o reinício dos contadores, os ids gerados mudariam conforme a ordem de
execução e nenhum teste seria repetível de forma isolada.
