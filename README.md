# Gerenciador de Finanças Pessoais

API REST para gestão de finanças pessoais com **DRE (Demonstração do Resultado) adaptada
para pessoa física**, desenvolvida com Java, Spring Boot e MariaDB.

O sistema separa **previsão** de **realização**: um compromisso cadastrado (título) não é
uma despesa até ser efetivamente pago. É esse desenho que permite responder, sem
ambiguidade, "quanto eu ainda tenho para pagar?" e "quanto eu realmente gastei?".

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [O conceito central: Título × Movimentação](#o-conceito-central-título--movimentação)
- [Modelagem do banco](#modelagem-do-banco)
- [Regras de negócio](#regras-de-negócio)
- [A DRE](#a-dre)
- [Endpoints da API](#endpoints-da-api)
- [Contas a pagar e a receber](#contas-a-pagar-e-a-receber)
- [Exemplo ponta a ponta (requisições reais)](#exemplo-ponta-a-ponta-requisições-reais)
- [Como usar](#como-usar)
- [Documentação interativa (Swagger)](#documentação-interativa-swagger)
- [Testes](#testes)
- [Tratamento de erros](#tratamento-de-erros)
- [Erros encontrados durante o desenvolvimento](#erros-encontrados-durante-o-desenvolvimento)
- [Decisões de projeto](#decisões-de-projeto)

---

## Tecnologias

| Item | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Web MVC | (via `spring-boot-starter-webmvc`) |
| Spring Data JPA | Hibernate 7.4 |
| Spring Security | 7.x |
| Bean Validation | Jakarta Validation 3 |
| MariaDB | 12.3 |
| Maven | 3.9+ |
| JUnit 5 | Jupiter |

---

## Arquitetura

Arquitetura em camadas. Cada camada tem uma responsabilidade única, e as dependências
fluem sempre em uma direção: `controller → service → repository`.

```text
src/main/java/com/example/financas/
├── config/        SecurityConfig
├── controller/    exposição HTTP (fino: não contém regra de negócio)
├── dto/
│   ├── request/   o que o cliente pode enviar
│   └── response/  o que a API devolve
├── entity/        mapeamento JPA + enums + comportamento de domínio
├── exception/     exceções de negócio e tratamento global
├── mapper/        conversão entidade <-> DTO
├── repository/    acesso a dados (Spring Data JPA)
│   └── projection/ projeções agregadas da DRE
└── service/       regras de negócio
```

**Por que `mapper` existe** (e não apenas as seis camadas mínimas): sem ele, a conversão
entidade → DTO se repetiria em todos os services, e a API correria o risco de serializar
entidades JPA diretamente — expondo o campo `senha` e disparando consultas preguiçosas
fora da transação. Cada mapper é uma classe pequena com métodos estáticos.

---

## O conceito central: Título × Movimentação

Esta é a decisão arquitetural que sustenta todo o sistema.

| | **Título** | **Movimentação** |
|---|---|---|
| Pergunta que responde | "O que eu **tenho** a pagar/receber?" | "O que **aconteceu** com o meu dinheiro?" |
| Natureza | **Compromisso / previsão** | **Evento realizado** |
| Valor | `valorPrevisto` | `valor` |
| Data | `dataVencimento` (quando **deve** acontecer) | `data` (quando **aconteceu**) |
| Situação | `PENDENTE` / `PAGO` / `VENCIDO` / `CANCELADO` | existe por definição: se foi registrada, aconteceu |
| Conta bancária | **não tem** — a obrigação ainda não está em banco nenhum | **tem**, e é obrigatória |
| Efeito na DRE realizada | **nenhum** | é a **única** fonte de receita/despesa realizada |
| Pode ser cancelado | sim, sem nunca gerar movimentação | não — cancelar um fato exige estorno |
| Exemplo | "Conta de energia, R$ 150,00, vence 10/03" | "Paguei R$ 150,00 em 08/03 pela conta corrente" |

Em uma frase: **o título é a promessa; a movimentação é o cumprimento.**

A cadeia completa:

```text
TÍTULO PAGO/RECEBIDO  ->  MOVIMENTAÇÃO  ->  DRE  ->  RESULTADO DO PERÍODO
```

Conceitualmente:

```text
USUÁRIO
   │
   ├── CONTAS
   │
   ├── CATEGORIAS
   │
   ├── TÍTULOS ──────────────┐
   │      ├── Receita/Despesa│
   │      ├── Vencimento     │  1:N (0..N)
   │      ├── Valor previsto │
   │      └── Situação       │
   │                         ▼
   └── MOVIMENTAÇÕES ◄───────┘
          ├── Receita/Despesa
          ├── Valor realizado
          ├── Data
          ├── Conta utilizada
          └── Título de origem (opcional)
```

---

## Modelagem do banco

**Cinco tabelas:**

```text
USUARIO
   ├── CONTA
   ├── CATEGORIA
   ├── TITULO
   └── MOVIMENTACAO ──> CONTA, CATEGORIA, TITULO (opcional)
```

### `usuario`

Armazena os usuários. Sem alterações nesta evolução.

| Coluna | Tipo | Restrição |
|---|---|---|
| `id_usuario` | `BIGINT` | **PK**, auto incremento |
| `nome` | `VARCHAR(100)` | NOT NULL |
| `email` | `VARCHAR(150)` | NOT NULL, **UNIQUE** |
| `senha` | `VARCHAR(255)` | NOT NULL |
| `data_cadastro` | `DATETIME(6)` | NOT NULL, preenchida por `@PrePersist` |
| `ativo` | `BIT(1)` | NOT NULL |

### `conta`

Contas financeiras do usuário. Sem alterações.

| Coluna | Tipo | Restrição |
|---|---|---|
| `id_conta` | `BIGINT` | **PK** |
| `nome` | `VARCHAR(100)` | NOT NULL |
| `tipo` | `VARCHAR(50)` | NOT NULL |
| `saldo_inicial` | `DECIMAL(15,2)` | NOT NULL |
| `usuario_id` | `BIGINT` | **FK** → `usuario`, NOT NULL |

**Restrição:** `UNIQUE(nome, usuario_id)` — nome de conta é único por usuário, não global.

### `categoria`

Classifica receitas e despesas. É a categoria que define as **linhas da DRE**.

| Coluna | Tipo | Restrição |
|---|---|---|
| `id_categoria` | `BIGINT` | **PK** |
| `nome` | `VARCHAR(100)` | NOT NULL |
| `tipo` | `ENUM('RECEITA','DESPESA')` | NOT NULL |
| `usuario_id` | `BIGINT` | **FK** → `usuario`, NOT NULL |

**Restrição:** `UNIQUE(nome, usuario_id)`.

> **Correção aplicada:** `nome` era `VARCHAR(10)`. Com esse limite, categorias normais não
> podiam ser cadastradas — `Alimentação` (11 caracteres), `Rendimentos` (11) e
> `Outras receitas` (15) falhavam com erro de truncamento. Agora são 100 caracteres.

### `titulo` — nova

Registra um compromisso financeiro previsto: uma obrigação a pagar ou um direito a receber,
com valor, vencimento e situação, **sem** afirmar que o dinheiro já se moveu.

| Coluna | Tipo | Restrição | Observação |
|---|---|---|---|
| `id_titulo` | `BIGINT` | **PK** | |
| `descricao` | `VARCHAR(255)` | NOT NULL | |
| `valor_previsto` | `DECIMAL(15,2)` | NOT NULL | `CHECK > 0` |
| `data_vencimento` | `DATE` | NOT NULL | base do "vencido" e da DRE prevista |
| `data_pagamento` | `DATE` | NULL | preenchida só quando quitado |
| `tipo` | `ENUM('RECEITA','DESPESA')` | NOT NULL | direção do compromisso |
| `situacao` | `ENUM('PENDENTE','PAGO','CANCELADO')` | NOT NULL, default `PENDENTE` | **sem `VENCIDO`** |
| `observacao` | `VARCHAR(255)` | NULL | campo livre |
| `categoria_id` | `BIGINT` | **FK** → `categoria`, NOT NULL | linha da DRE |
| `usuario_id` | `BIGINT` | **FK** → `usuario`, NOT NULL | dono |

**Cardinalidade:** `usuario 1:N titulo`, `categoria 1:N titulo`, `titulo 1:N movimentacao`.

**Índices:** `(usuario_id, data_vencimento)`, `(usuario_id, situacao)`.

**Regras de integridade:**

- `CHECK (valor_previsto > 0)` — não existe obrigação de valor zero ou negativo.
- `CHECK (situacao <> 'PAGO' OR data_pagamento IS NOT NULL)` — não existe título quitado sem
  data de quitação.
- Ambas as FKs com `ON DELETE RESTRICT`.

**Duas ausências propositais:**

1. **Não tem `conta_id`.** Um título ainda não pago não está em banco nenhum. A conta só é
   conhecida no momento do pagamento, e por isso vive na `movimentacao`. Colocar uma conta
   aqui afirmaria que a obrigação já saiu de uma conta — exatamente o erro que a separação
   corrige.
2. **Não tem `valor_realizado`.** O valor realizado é sempre a soma das movimentações
   vinculadas. Guardá-lo em uma coluna criaria duas fontes para o mesmo número, que podem
   divergir. É calculado com `SUM(movimentacao.valor)`.

### `movimentacao` — alterada

Registra um **evento financeiro realizado**, opcionalmente originado da quitação de um título.

| Coluna | Tipo | Restrição | Observação |
|---|---|---|---|
| `id_movimentacao` | `BIGINT` | **PK** | |
| `descricao` | `VARCHAR(255)` | NOT NULL | |
| `valor` | `DECIMAL(15,2)` | NOT NULL | **valor realizado**, `CHECK > 0` |
| `tipo` | `ENUM('RECEITA','DESPESA')` | NOT NULL | |
| `data` | `DATE` | NOT NULL | **data do evento** |
| `usuario_id` | `BIGINT` | **FK** → `usuario`, NOT NULL | |
| `categoria_id` | `BIGINT` | **FK** → `categoria`, NOT NULL | |
| `conta_id` | `BIGINT` | **FK** → `conta`, **NOT NULL** | **nova** |
| `titulo_id` | `BIGINT` | **FK** → `titulo`, **NULL** | **nova** |

**Cardinalidade:** `conta 1:N movimentacao`, `titulo 1:N movimentacao` (0..N).

**Índices:** `(usuario_id, data)`, `(conta_id)`, `(titulo_id)`.

- `conta_id` **obrigatória**: todo evento financeiro acontece em alguma conta. Absorveu o
  papel das tabelas `recebimento`/`pagamento` e tornou o cálculo de saldo uma consulta única.
- `titulo_id` **opcional**: `NULL` significa lançamento avulso ("paguei um café em dinheiro"),
  que nunca teve previsão. É essa opcionalidade que garante que **nada do uso anterior se
  perde**.

### Tabelas removidas: `recebimento` e `pagamento`

Ambas foram **absorvidas por `movimentacao`**. Motivo: eram tabelas-gêmeas de estrutura
idêntica (`valor`, `data`, `movimentacao_id`, `conta_id`), e a diferença "entrada × saída" já
está em `movimentacao.tipo`.

```text
ANTES (6 tabelas)                        DEPOIS (5 tabelas)

movimentacao(valor, data, tipo,          movimentacao(valor, data, tipo,
             usuario, categoria)                      usuario, categoria,
  ├─ recebimento(valor, data, conta)                  conta, titulo)
  └─ pagamento(valor, data, conta)

2 valores e 2 datas concorrentes         1 valor, 1 data, 1 caminho
para o mesmo dinheiro
```

Os conceitos **não** se perderam: as operações "pagar um título" e "receber um título"
continuam existindo, agora como endpoints e DTOs (`PagamentoRequest`, `RecebimentoRequest`).

---

## Regras de negócio

### Título

| # | Regra |
|---|---|
| RN01 | `valorPrevisto` deve ser maior que zero |
| RN02 | `dataVencimento` é obrigatória |
| RN03 | A categoria deve pertencer ao mesmo usuário **e** ter o mesmo `tipo` do título |
| RN04 | Título nasce `PENDENTE`, com zero realizado — cadastrar previsão **não** cria despesa |
| RN05 | Título `CANCELADO` não pode receber pagamento |
| RN06 | Título `PAGO` (quitado) não pode receber novo pagamento |
| RN07 | Cancelamento só é permitido se nada foi realizado |
| RN08 | Um título pode ter N movimentações (pagamento parcial) |
| RN09 | `Σ movimentações ≤ valorPrevisto` |
| RN10 | Ao atingir `Σ = valorPrevisto`: `situacao = PAGO` e `dataPagamento` = data do último pagamento |
| RN11 | `VENCIDO` é **derivado** (`PENDENTE` + `dataVencimento < hoje`), nunca armazenado |
| RN12 | Todas as operações validam o dono do título |

### Movimentação

| # | Regra |
|---|---|
| RN13 | `valor > 0` — o sinal vem do `tipo`, nunca de valor negativo |
| RN14 | `conta` obrigatória e do mesmo usuário |
| RN15 | Categoria do mesmo usuário e com `tipo` igual ao da movimentação |
| RN16 | Se vier de um título, `tipo`, `categoria` e `descricao` são **herdados** do título |
| RN17 | Não pode ser criada se RN09 falhar |
| RN18 | Lançamento avulso (`titulo_id = null`) é permitido |
| RN19 | Movimentação não é editável em `valor`/`data` se vier de título |
| RN20 | Uma movimentação é imutável após criada — correções exigem estorno |

### Por que "PAGO" significa **quitado**

Caso real: aluguel de R$ 1.200,00 pago em duas vezes. O título só fica `PAGO` quando a soma
das movimentações **iguala** o valor previsto. Enquanto for menor, permanece `PENDENTE` e
expõe `valorRealizado` e `valorEmAberto`. Essa é a única leitura que mantém a DRE consistente
e evita que um título metade pago apareça como "pago".

### Por que "VENCIDO" é derivado e não armazenado

Se `VENCIDO` fosse gravado em `situacao`, seria necessário um job agendado varrendo a tabela
toda madrugada para marcar os títulos que venceram ontem. Se o job falhasse, ou se o sistema
ficasse parado no fim de semana, a situação ficaria mentindo — e relatórios históricos
ficariam errados retroativamente.

A regra é calculada em tempo de leitura:

```text
situacaoEfetiva(titulo) =
    CANCELADO, se situacao = CANCELADO
    PAGO,      se situacao = PAGO
    VENCIDO,   se situacao = PENDENTE e dataVencimento < hoje
    PENDENTE,  caso contrário
```

A API devolve **os quatro valores** que o usuário espera (`situacaoEfetiva`), e o campo
`situacao` mostra o que está gravado no banco. Ter os dois campos torna visível que "vencido"
é um estado derivado, e não uma coluna.

### Banco × service: onde cada regra vive

- **No banco** (`CHECK`, `NOT NULL`, `FK`, `UNIQUE`): o que é expressável em SQL —
  `valor > 0`, `situacao='PAGO' ⇒ data_pagamento IS NOT NULL`, existência das FKs.
- **No service** (dentro de `@Transactional`): o que exige ler outras linhas — "conta é do
  mesmo usuário", "categoria é do tipo certo", "Σ movimentações ≤ valorPrevisto". Essas
  regras **não** podem virar `CHECK`, porque um `CHECK` do MariaDB não faz `SELECT` em outra
  tabela.

---

## A DRE

### A regra central

> **Um título nunca entra na DRE realizada. Só a movimentação entra.**

```text
DRE PREVISTA (regime de competência — por data_vencimento)
  RECEITAS PREVISTAS  = Σ titulo.valorPrevisto  WHERE tipo=RECEITA
  DESPESAS PREVISTAS  = Σ titulo.valorPrevisto  WHERE tipo=DESPESA
  RESULTADO PREVISTO  = RECEITAS PREVISTAS − DESPESAS PREVISTAS

DRE REALIZADA (regime de caixa — por data da movimentação)
  RECEITAS REALIZADAS = Σ movimentacao.valor     WHERE tipo=RECEITA
  DESPESAS REALIZADAS = Σ movimentacao.valor     WHERE tipo=DESPESA
  RESULTADO REALIZADO = RECEITAS REALIZADAS − DESPESAS REALIZADAS

DRE COMPARATIVA
  VARIAÇÃO = RESULTADO REALIZADO − RESULTADO PREVISTO
```

**Na prática:** uma conta de energia de R$ 150,00 que vence dia 10 e ainda não foi paga
aparece na DRE **prevista** (era uma obrigação do período) e **não** aparece na DRE
**realizada** (nenhum real saiu da conta). No instante em que o pagamento é registrado, ela
passa a compor a DRE realizada, na **data do pagamento** — não na data do vencimento.

### Estrutura do relatório

As linhas vêm das **categorias do usuário**, não de uma lista fixa no código:

```text
RECEITAS
  Salários .................... previsto 5.000,00 | realizado 5.000,00
  Freelances ................. previsto 1.200,00 | realizado   800,00
  Rendimentos ................ previsto    50,00 | realizado    50,00
  (=) TOTAL DE RECEITAS ...... previsto 6.250,00 | realizado 5.850,00

(−) DESPESAS
  Moradia .................... previsto 1.500,00 | realizado 1.500,00
  Alimentação ................ previsto   800,00 | realizado   620,00
  Transporte ................. previsto   300,00 | realizado   280,00
  Educação ................... previsto   400,00 | realizado     0,00
  (=) TOTAL DE DESPESAS ...... previsto 3.000,00 | realizado 2.400,00

(=) RESULTADO DO PERÍODO ..... previsto 3.250,00 | realizado 3.450,00
    Variação ................. +200,00 (favorável)
    Títulos vencidos ......... 1 (R$ 400,00)
```

**Convenção de sinais.** Os totais de receita e despesa são **magnitudes positivas**. O campo
`variacao` é **assinado com sinal normalizado**, de modo que **positivo = favorável**:

- receita recebida **acima** do previsto → variação positiva;
- despesa gasta **abaixo** do previsto → variação positiva;
- despesa gasta **acima** do previsto → variação negativa.

Assim o usuário não precisa interpretar "negativo é bom" linha a linha.

### A DRE não é armazenada

Não existe tabela de DRE. Ela é sempre calculada a partir dos fatos, o que elimina qualquer
possibilidade de o relatório divergir dos lançamentos que o originaram. Como a DRE realizada
lê **uma única tabela** de eventos, ela é uma consulta só, sem `UNION`.

---

## Endpoints da API

Base: `/api/v1`. A API está aberta (sem autenticação) — ver [Decisões de projeto](#decisões-de-projeto).

### Usuários

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/usuarios` | Lista usuários |
| `GET` | `/usuarios/{id}` | Busca por id |
| `POST` | `/usuarios` | Cadastra |
| `PUT` | `/usuarios/{id}` | Atualiza |
| `DELETE` | `/usuarios/{id}` | Remove |

### Contas

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/contas?usuarioId=` | Lista com **saldo atual** calculado |
| `GET` | `/contas/{id}` | Busca por id |
| `GET` | `/contas/{id}/extrato` | Movimentações da conta |
| `POST` | `/contas` | Cadastra |
| `PUT` | `/contas/{id}` | Atualiza |
| `DELETE` | `/contas/{id}` | Remove |

O saldo é `saldoInicial + receitas − despesas`, calculado em uma consulta única.

### Categorias

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/categorias?usuarioId=&tipo=` | Lista, com filtro opcional por tipo |
| `GET` | `/categorias/{id}` | Busca por id |
| `POST` | `/categorias` | Cadastra |
| `PUT` | `/categorias/{id}` | Atualiza |
| `DELETE` | `/categorias/{id}` | Remove |

### Títulos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/titulos?usuarioId=&tipo=&situacao=` | Lista com filtros |
| `GET` | `/titulos/{id}` | Busca por id |
| `GET` | `/titulos/vencidos?usuarioId=&referencia=` | Pendentes já vencidos |
| `POST` | `/titulos` | Cadastra a **previsão** — nada acontece no caixa |
| `PUT` | `/titulos/{id}` | Atualiza (só se pendente) |
| `POST` | `/titulos/{id}/pagar` | Registra a **realização** (despesa paga) |
| `POST` | `/titulos/{id}/receber` | Registra a **realização** (receita recebida) |
| `POST` | `/titulos/{id}/cancelar` | Cancela (só se nada foi realizado) |
| `DELETE` | `/titulos/{id}` | Remove (só se não tem movimentações) |

Repare que **não existe** endpoint para "criar a despesa de um título". Essa operação não
existe: despesa é consequência de pagamento, nunca de cadastro.

### Movimentações

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/movimentacoes?usuarioId=&inicio=&fim=&tipo=` | Lista com filtros |
| `GET` | `/movimentacoes/{id}` | Busca por id |
| `POST` | `/movimentacoes` | Registra lançamento **avulso** (sem título) |

Não há `PUT`/`DELETE`: uma movimentação é um fato consumado. Correções exigem estorno e novo
lançamento.

### DRE

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/dre?usuarioId=&inicio=&fim=&modo=` | Gera a DRE do período |

`modo` aceita três valores, e cada um decide **quais valores saem** na resposta:

| `modo` | Fonte | Data filtrada | O que a resposta traz |
|---|---|---|---|
| `previsto` | `titulo` | `vencimento` | só os valores previstos; os realizados vêm `null` |
| `realizado` | `movimentacao` | `data` | só os valores realizados; os previstos vêm `null` |
| `comparativo` | as duas | ambas | os dois lados e a `variacaoResultado` (**padrão**) |

Um valor desconhecido devolve **422** dizendo quais são aceitos, em vez de devolver uma
apuração com o rótulo errado.

Os campos não apurados no modo escolhido vêm como `null`, e não como zero: `null` significa
"não foi apurado neste modo", enquanto zero afirmaria "é zero" — que é outra informação.

As listas `linhasReceitas` e `linhasDespesas` vêm sempre (mesmo que só com os valores de um
lado), porque agrupar por tipo de lançamento não depende do modo.

### Contas a pagar e a receber

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/contas-a-pagar?usuarioId=&dataReferencia=&janelaDias=` | O que ainda falta pagar |
| `GET` | `/contas-a-receber?usuarioId=&dataReferencia=&janelaDias=` | O que ainda falta receber |
| `GET` | `/resumo-financeiro?usuarioId=&dataReferencia=&janelaDias=` | Os dois lados com o saldo previsto |

Detalhes em [Contas a pagar e a receber](#contas-a-pagar-e-a-receber).

---

## Contas a pagar e a receber

Esta funcionalidade responde às duas perguntas mais práticas do dia a dia — *"quanto eu
ainda tenho para pagar?"* e *"quanto eu ainda tenho para receber?"*.

### Ela não introduz nenhum conceito novo

Contas a pagar e a receber **não são entidades novas**. São a mesma entidade `Titulo` vista
de dois ângulos:

| Conceito | Como é derivado |
|---|---|
| Contas a **pagar** | títulos com `tipo = DESPESA` |
| Contas a **receber** | títulos com `tipo = RECEITA` |
| Está **em aberto** | `situacao = PENDENTE` (não quitado nem cancelado) |
| **Quanto falta** | `valorPrevisto − Σ movimentações` |
| Está **vencido** | `dataVencimento < dataReferencia` |

Por isso não há tabela, entidade nem campo novo — apenas consultas e agregações sobre o que
já existia. Criar `ContaAPagar` e `ContaAReceber` como entidades próprias levaria a três
tabelas com os mesmos campos (descrição, valor, vencimento, situação), exatamente o problema
que a evolução anterior corrigiu ao absorver `recebimento` e `pagamento` em `movimentacao`.

### A regra do valor em aberto

Os totais somam **`valorEmAberto`**, nunca o valor previsto:

> Um aluguel de R$ 1.200,00 com R$ 600,00 já pagos entra como **R$ 600,00** nas contas a
> pagar — não como R$ 1.200,00.

Somar o valor previsto inflaria a dívida e faria o usuário achar que deve mais do que
realmente deve.

### O que a resposta traz

```json
{
  "usuarioId": 1,
  "tipo": "DESPESA",
  "dataReferencia": "2026-03-15",
  "titulos": [ ... ],
  "totalEmAberto": 1850.00,
  "totalVencido": 600.00,
  "totalAVencerProximosDias": 950.00,
  "quantidadeTitulos": 4,
  "quantidadeVencidos": 1,
  "quantidadeParciais": 1,
  "quantidadeAVencerProximosDias": 2,
  "vencidos": [ ... ],
  "aVencerProximosDias": [ ... ],
  "janelaDias": 30
}
```

- **`titulos`** — todos os que têm valor em aberto, ordenados por vencimento (os vencidos
  naturalmente ficam no topo);
- **`vencidos`** e **`aVencerProximosDias`** — recortes já prontos, para o cliente não
  reimplementar a mesma regra de corte nem escolher uma data de referência diferente da que
  o servidor usou.

Cada item traz `situacao`, que refina a leitura:

| `situacao` | Significado |
|---|---|
| `A_VENCER` | pendente, dentro do prazo |
| `VENCIDO` | pendente, vencimento ultrapassado |
| `PARCIAL` | já recebeu algum pagamento, ainda no prazo |
| `PARCIAL_VENCIDO` | parcialmente pago e com vencimento ultrapassado |

`diasParaVencimento` é **positivo quando já venceu** (dias de atraso) e negativo quando ainda
falta.

### Parâmetros

- `dataReferencia` (opcional) — a data que define o que está vencido. Permite simular
  cenários ("como fica minha posição no fim do mês?") e torna o comportamento testável sem
  depender do relógio da máquina.
- `janelaDias` (opcional, padrão **30**) — quantos dias à frente considerar em "próximos
  dias". A janela é **inclusiva**: com 30 dias a partir de 15/03, entram os vencimentos até
  14/04.

### O resumo consolidado

`GET /resumo-financeiro` devolve os dois lados e o `saldoPrevisto` (a receber − a pagar).

Ele é **deliberadamente separado do saldo das contas**. O saldo bancário é passado (fatos
consumados) e as contas em aberto são futuro (previsões); somá-los produziria um número
enganoso. Para o dinheiro que já está na conta, use `GET /api/v1/contas?usuarioId=`.

---

## Exemplo ponta a ponta (requisições reais)

A sequência conceitual está em [Como usar](#como-usar). Aqui ela aparece como requisições reais, usando o caso do enunciado: *"Tenho uma conta de energia de R$ 150,00 que vence dia 10."*

**1. Criar usuário, conta e categoria**

```bash
POST /api/v1/usuarios
{ "nome": "Ana", "email": "ana@exemplo.com", "senha": "segredo123" }

POST /api/v1/contas
{ "nome": "Conta Corrente", "tipo": "CORRENTE", "saldoInicial": 1000.00, "usuarioId": 1 }

POST /api/v1/categorias
{ "nome": "Moradia", "tipo": "DESPESA", "usuarioId": 1 }
```

**2. Cadastrar o título**

```bash
POST /api/v1/titulos
{
  "descricao": "Conta de energia",
  "valorPrevisto": 150.00,
  "dataVencimento": "2026-03-10",
  "tipo": "DESPESA",
  "categoriaId": 1,
  "usuarioId": 1
}
```

Resposta: `situacao = PENDENTE`, `valorRealizado = 0`, `valorEmAberto = 150.00`.
**Nenhuma movimentação foi criada e nenhuma despesa existe na DRE realizada.**

```bash
GET /api/v1/dre?usuarioId=1&inicio=2026-03-01&fim=2026-03-31
```

```json
{
  "totalReceitasPrevistas": 0.00,   "totalReceitasRealizadas": 0.00,
  "totalDespesasPrevistas": 150.00, "totalDespesasRealizadas": 0.00,
  "resultadoPrevisto": -150.00,     "resultadoRealizado": 0.00
}
```

Note: a despesa existe como **previsão**, mas o **realizado está zerado**.

**3. Pagar o título**

```bash
POST /api/v1/titulos/1/pagar
{ "valor": 150.00, "data": "2026-03-08", "contaId": 1 }
```

A operação, em uma única transação: valida que o título está pendente e pertence ao usuário,
valida que o valor não excede o em aberto, valida que a conta é do mesmo usuário, **cria a
movimentação** (valor 150, data 08/03, DESPESA, conta 1, título 1), e como a soma atingiu o
previsto, marca o título como `PAGO` com `dataPagamento = 2026-03-08`.

**4. A DRE mudou**

```json
{
  "totalDespesasPrevistas": 150.00, "totalDespesasRealizadas": 150.00,
  "resultadoPrevisto": -150.00,     "resultadoRealizado": -150.00
}
```

O saldo da conta passou de R$ 1.000,00 para R$ 850,00.

**5. Pagamento parcial**

```bash
POST /api/v1/titulos/2/pagar { "valor": 600.00, "data": "2026-03-05", "contaId": 1 }
```

Resposta: `situacao = PENDENTE`, `valorRealizado = 600.00`, `valorEmAberto = 600.00`.
A DRE realizada reflete os R$ 600,00 já pagos — proporcionalmente, sem esconder nada.

---

## Como usar

Esta seção explica **como o sistema funciona por dentro** e como operá-lo no dia a dia, do
banco vazio até a DRE pronta.

### 1. Preparar o banco

```sql
CREATE DATABASE financas CHARACTER SET utf8mb4;
```

### 2. Configurar a conexão

Edite `financas/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/financas
spring.datasource.username=root
spring.datasource.password=SUA_SENHA
```

> Não versione credenciais reais.

### 3. Criar as tabelas

Há dois caminhos, e eles se complementam:

**a) Script de migração (recomendado).** Registra a evolução do modelo de forma explícita e
reproduzível, e cria os `CHECK` que o Hibernate não gera:

```bash
mariadb -u root -p financas < financas/src/main/resources/db/migration/V2__evolucao_dre_titulos.sql
```

**b) Deixar o Hibernate criar.** Com `spring.jpa.hibernate.ddl-auto=update`, subir a aplicação
já cria as tabelas a partir das entidades. É mais simples, mas o Hibernate **não cria os
`CHECK`** de `valor > 0` — nesse caminho, essas regras passam a valer apenas na camada de
serviço.

### 4. Subir a aplicação

```bash
cd financas

# Windows
mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080` e a documentação interativa em
`http://localhost:8080/swagger-ui.html`.

### 5. Popular com dados de exemplo (opcional)

Para não começar com o banco vazio, há um script com um cenário completo — salário, freelance,
aluguel, mercado, transporte e uma consulta médica pendente:

```bash
mariadb -u root -p financas < financas/src/main/resources/db/exemplo-dados.sql
```

Ele cria o usuário `ana@exemplo.com` (senha `segredo123`), duas contas, seis categorias, seis
títulos e quatro movimentações. O resultado esperado, já verificado:

| | Receitas | Despesas | Resultado |
|---|---|---|---|
| **DRE prevista** (por vencimento) | 6.200,00 | 2.850,00 | **+3.350,00** |
| **DRE realizada** (por pagamento) | 5.000,00 | 2.600,00 | **+2.400,00** |

E contas a pagar com **250,00** em aberto (a consulta médica).

> O script apaga os dados existentes antes de inserir. Não use em banco com dados reais.

### 6. O fluxo de uso

O sistema tem **uma ordem de operações**, e entendê-la é entender o sistema:

```text
1. cadastrar usuário
2. cadastrar contas        (onde o dinheiro está)
3. cadastrar categorias    (como você classifica)
        │
        ▼
4. CADASTRAR TÍTULO        ← a previsão. Nada acontece no caixa.
        │
        │  a DRE PREVISTA já mostra este valor
        │  a DRE REALIZADA ainda mostra zero
        ▼
5. PAGAR ou RECEBER        ← a realização. Cria a movimentação.
        │
        ▼
   a DRE REALIZADA passa a mostrar o valor,
   na data do pagamento (não na do vencimento)
```

**Passo 1 — usuário.** É o dono de tudo. Contas, categorias, títulos e movimentações sempre
pertencem a um usuário, e o `usuarioId` é informado nas chamadas.

**Passo 2 — contas.** Representam onde o dinheiro está (conta corrente, poupança, carteira). O
`saldoInicial` é o ponto de partida; o **saldo atual é calculado**, nunca gravado.

**Passo 3 — categorias.** São a classificação, e viram as **linhas do relatório**. Cada uma tem
um tipo, RECEITA ou DESPESA, e o tipo precisa combinar com o lançamento: uma despesa não pode
ser classificada como "Salários". O sistema recusa isso com 422.

**Passo 4 — títulos (a previsão).** Aqui está o conceito central. Um título é um **compromisso**:
"tenho uma conta de energia de R$ 150 que vence dia 10".

Cadastrar um título **não cria despesa**. Nenhum valor sai ou entra de conta alguma, e a DRE
realizada continua zerada. O título aparece apenas na DRE **prevista**, que olha a data de
vencimento.

**Passo 5 — pagar ou receber (a realização).** Quando o dinheiro efetivamente se move, você
registra o pagamento (`POST /titulos/{id}/pagar`) ou o recebimento (`/receber`), informando
**quanto**, **quando** e **de qual conta**.

É este passo que cria a **movimentação** — o evento que de fato aconteceu — e é ele que faz o
valor aparecer na DRE realizada.

**Passo 6 — consultar.** Com os dados lançados, os relatórios ficam disponíveis:

| O que você quer saber | Endpoint |
|---|---|
| Como foi o mês (previsto × realizado) | `GET /api/v1/dre` |
| Quanto ainda tenho a pagar | `GET /api/v1/contas-a-pagar` |
| Quanto ainda tenho a receber | `GET /api/v1/contas-a-receber` |
| Como está minha situação geral | `GET /api/v1/resumo-financeiro` |
| Quanto tenho em cada conta | `GET /api/v1/contas?usuarioId=1` |

### 7. O que o `modo` da DRE faz

O endpoint da DRE aceita três valores, e cada um decide **quais valores saem** na resposta:

| `modo` | Fonte | Data filtrada | O que traz |
|---|---|---|---|
| `previsto` | título | vencimento | só os valores previstos; os realizados vêm `null` |
| `realizado` | movimentação | pagamento | só os valores realizados; os previstos vêm `null` |
| `comparativo` | as duas | ambas | os dois lados + a variação (**padrão**) |

Um `modo` desconhecido devolve **422** dizendo quais são aceitos.

Use `previsto` para planejar ("o que eu esperava deste mês"), `realizado` para conferir o que
aconteceu de fato, e `comparativo` para ver a diferença entre os dois.

### 8. O que os relatórios mostram — e por que confiar neles

A regra que sustenta tudo:

> **Um título nunca entra na DRE realizada. Só a movimentação entra.**

Na prática, com o cenário de exemplo: a consulta médica de R$ 250 aparece na DRE prevista e
**não** na realizada, porque ainda não foi paga. No dia em que for paga, ela passa a compor o
resultado realizado **na data do pagamento**, não na data do vencimento.

Também vale saber:

- **"PAGO" significa quitado**, não "teve algum pagamento". Aluguel de R$ 1.500 pago em duas
  vezes fica PENDENTE até a soma atingir 1.500, e o quanto falta aparece em `valorEmAberto`.
- **"VENCIDO" não é gravado no banco.** É calculado na hora da consulta: um título pendente cujo
  vencimento já passou. Assim não existe rotina agendada para marcar atrasos, e nenhum relatório
  fica errado se o sistema passar um fim de semana desligado.
- **Títulos cancelados saem da previsão** e nunca chegaram a existir na realização.
- **Movimentações não podem ser editadas nem excluídas.** São fatos consumados: o dinheiro já se
  moveu. A API responde 405 explicando os métodos que aceita.

### 9. Erros que você vai encontrar

A API recusa o que viola o domínio, e explica o motivo. Os casos mais comuns:

| Status | Quando | Exemplo de mensagem |
|---|---|---|
| **400** | dado malformado | `Data inválida. Use o formato ISO: AAAA-MM-DD` |
| **404** | id inexistente | `Usuário não encontrado para o id 99` |
| **409** | duplicado ou em uso | `Já existe uma categoria 'Moradia' para este usuário` |
| **422** | regra de negócio | `O valor informado (R$ 200) é maior que o valor em aberto do título (R$ 150.00)` |

O mapa completo está em [Tratamento de erros](#tratamento-de-erros).

---

## Documentação interativa (Swagger)

Com a aplicação no ar, a API pode ser explorada e exercitada pelo navegador:

| Endereço | O que é |
|---|---|
| `http://localhost:8080/swagger-ui.html` | **Swagger UI** — interface para executar as chamadas |
| `http://localhost:8080/v3/api-docs` | **Contrato OpenAPI** em JSON, consumível por Postman e Insomnia |

O contrato é **gerado a partir do código** (controllers, DTOs e anotações de validação). Não
existe um YAML escrito à mão que possa ficar desatualizado em relação ao que o sistema faz.

Os endpoints estão agrupados em seis seções: **Usuários**, **Contas**, **Categorias**,
**Títulos**, **Movimentações** e **Relatórios**. Cada operação documenta o que faz, quando usar
e quais status de erro pode devolver. A ordem de uso está em [Como usar](#como-usar).

### Completude verificada por teste

`DocumentacaoOpenApiTest` garante que a documentação **não fica para trás**:

- os **19 caminhos** que a API expõe estão todos no contrato (e nenhum caminho órfão);
- toda operação tem `summary`, `tag` e resposta de sucesso;
- as 6 tags declaradas correspondem às usadas;
- os schemas de requisição e resposta estão registrados, com os campos que a API devolve;
- o endpoint de pagamento documenta o **pagamento parcial** e o de DRE documenta os **três
  modos** de apuração.

Assim, criar um endpoint novo e esquecer de documentá-lo faz o build falhar — em vez de a falha
aparecer na mão de quem tenta usar a API.

### Versão do springdoc

Usa-se o **springdoc-openapi 3.0.2**, que é a linha compatível com Spring Boot 4 / Spring
Framework 7. A linha 2.x atende Spring Boot 3 e **não** funciona aqui.

---

## Testes

A suíte cobre as regras de negócio centrais, com destaque para a que dá sentido ao modelo:

> **`tituloPendenteNaoEntraNaDreRealizada`** — prova que uma conta ainda não paga não é
> contada como despesa efetivamente realizada.

Também são verificados: pagamento parcial, quitação em duas parcelas, reconhecimento pela data
do pagamento, rejeição de pagamento acima do valor em aberto, título quitado/cancelado, conta e
categoria de outro usuário, situação `VENCIDO` derivada, agrupamento por categoria, sinal da
variação, isolamento entre usuários, e as **contas a pagar e a receber** (soma pelo valor em
aberto, exclusão de quitados e cancelados, separação entre vencido e a vencer, janela
configurável e saldo previsto).

`TratamentoDeErrosApiTest` cobre as **respostas de erro** com `MockMvc`:
os 10 status HTTP possíveis, o formato único do corpo, a tradução das mensagens e — o mais
importante — que **nenhuma resposta vaze detalhe interno** (SQL, nome de constraint, classe
Java, configuração do parser).

```bash
cd financas
mvn test
```

Os testes usam um banco separado (`financas_test`, criado automaticamente) e não afetam os
dados de desenvolvimento.

**65 testes de integração**, todos passando.

---

## Tratamento de erros

**Princípio: nenhum erro sem explicação.** Nenhuma falha chega ao cliente como um "erro
interno" genérico, e nenhuma mensagem vaza detalhe interno (SQL executado, nome de tabela,
classe Java, configuração de biblioteca).

### Mapa completo

| Status | `erro` | Quando ocorre |
|---|---|---|
| **400** | `Erro de validação` | Bean Validation reprovou campos do corpo (todos são listados de uma vez) |
| **400** | `Requisição inválida` | JSON malformado, enum inválido, data inválida, corpo ausente |
| **400** | `Parâmetro inválido` | parâmetro de query/rota com tipo inválido (lista os valores aceitos) |
| **400** | `Parâmetro ausente` | parâmetro obrigatório não informado (diz qual) |
| **404** | `Recurso não encontrado` | id inexistente |
| **404** | `Rota não encontrada` | endpoint inexistente |
| **405** | `Método não permitido` | verbo HTTP não aceito pela rota (lista os aceitos) |
| **409** | `Registro duplicado` | e-mail, nome de conta ou de categoria repetido |
| **409** | `Recurso em uso` | exclusão bloqueada por registros dependentes (com a contagem) |
| **409** | `Conflito de integridade` | violação de FK/UNIQUE detectada pelo banco (rede de segurança) |
| **422** | `Regra de negócio violada` | regra de domínio violada (título quitado, valor acima do em aberto, modo de DRE inválido, etc.) |
| **500** | `Erro interno` | erro realmente inesperado |

Formato único de resposta:

```json
{
  "timestamp": "2026-10-01T19:59:58",
  "status": 400,
  "erro": "Parâmetro inválido",
  "mensagem": "O parâmetro 'tipo' recebeu um valor inválido. Valores aceitos: RECEITA, DESPESA",
  "caminho": "/api/v1/titulos",
  "detalhes": ["parâmetro: tipo, valor: XYZ, esperado: TipoMovimentacao"]
}
```

### Exemplos de mensagens

O objetivo é que a mensagem diga **o que fazer**, não apenas o que falhou:

```text
400  O JSON enviado está malformado. Verifique chaves, aspas e vírgulas
400  Data inválida. Use o formato ISO: AAAA-MM-DD (exemplo: 2026-03-10)
400  Valor inválido para um campo de opções. Valores aceitos: RECEITA, DESPESA
400  O parâmetro obrigatório 'usuarioId' não foi informado
400  O corpo da requisição é obrigatório e não foi enviado
404  Usuário não encontrado para o id 99999
404  Não existe endpoint para GET /api/v1/rota-que-nao-existe
405  O método DELETE não é aceito nesta rota. Métodos aceitos: GET
409  Já existe um usuário com o e-mail ana@exemplo.com
409  Não dá para excluir categoria porque há 1 título vinculado a ele
422  A categoria 'Moradia' é do tipo DESPESA e não pode ser usada em título de tipo RECEITA
422  O valor informado (R$ 9999) é maior que o valor em aberto do título (R$ 150.00)
```

### Duas camadas para o conflito de exclusão

Quando uma exclusão é bloqueada por registros dependentes, há **duas** verificações:

1. **No service, antes de tentar** — produz a mensagem com **contagem**:
   *"Não dá para excluir categoria porque há 1 título vinculado a ele"*.
2. **No banco, como garantia final** — entre a verificação e o `DELETE`, alguém poderia inserir
   uma linha dependente. A chave estrangeira recusa, e o
   `DataIntegrityViolationException` é traduzido a partir do **nome da constraint**:
   *"Não existem títulos ou movimentações que a utilizam…"*.

Sem a segunda camada, uma condição de corrida viraria um erro 500.

### O que deliberadamente **não** é enviado ao cliente

As mensagens cruas do banco e do Jackson contêm informação que não ajuda quem consome a API e
revela estrutura interna. Ambas são **registradas no log do servidor** e substituídas por uma
mensagem traduzida:

| Origem | O que era enviado | O que é enviado agora |
|---|---|---|
| Banco | `could not execute statement [...] SQL [delete from usuario where...] constraint [fk_categoria_usuario]` | `Não dá para excluir usuário porque há 1 conta vinculada a ele` |
| Jackson | `Cannot deserialize value of type com.example.financas.entity.TipoMovimentacao from String "INVALIDO"... byte offset: #73` | `Valor inválido para um campo de opções. Valores aceitos: RECEITA, DESPESA` |

O erro 500 segue a mesma regra: a causa completa vai para o log, e o cliente recebe
*"Ocorreu um erro inesperado ao processar a requisição"*. Se um erro do cliente chegar ao
handler de 500, é sinal de que falta um tratador específico — os testes cobrem esse caminho.

---

## Erros encontrados durante o desenvolvimento

Registrados aqui porque são úteis: quase todos só aparecem em tempo de execução, não na
compilação.

### 1. `BigDecimal` não se compara com `equals`

Cinco testes falharam com `expected: <0.00> but was: <0>`. O código de produção estava
correto; o defeito era do teste.

`BigDecimal.equals` compara **valor e escala**. Portanto `new BigDecimal("0")` e
`new BigDecimal("0.00")` são **diferentes** para `assertEquals`, ainda que sejam o mesmo valor
monetário. A comparação correta entre `BigDecimal` é `compareTo`. Um erro fácil de cometer e
que produz um teste vermelho por um motivo que não tem nada a ver com a regra testada.

### 2. Ciclo de beans entre services

`MovimentacaoService` e `TituloService` se injetavam mutuamente e a aplicação não subia:

```text
The dependencies of some of the beans in the application context form a cycle:
   movimentacaoService <--> tituloService
```

A solução foi uma **dependência de mão única**: as validações de quitação foram para o
`MovimentacaoService` (dono das regras de movimentação) e a mudança de situação do título é
feita por uma `@Modifying @Query` no repositório. A alternativa — um `@Lazy` para mascarar o
ciclo — apenas esconderia o problema.

### 3. Jackson 2 × Jackson 3 e o formato das datas

Duas armadilhas encadeadas, ambas específicas do **Spring Boot 4**:

**(a) O formato padrão de data é ruim para a API.** As datas saíam como arrays JSON —
`"dataVencimento":[2026,9,26]` em vez de `"2026-09-26"`. Um cliente teria de saber que a
posição 1 é o mês e que ele começa em 1, não em 0.

**(b) A solução "óbvia" não existe mais.** A correção habitual seria a propriedade
`spring.jackson.serialization.write-dates-as-timestamps=false`. No Spring Boot 4 isso
**derruba a aplicação na subida**, porque o Jackson 3 removeu `WRITE_DATES_AS_TIMESTAMPS` do
enum `SerializationFeature`:

```text
No enum constant tools.jackson.databind.SerializationFeature.write-dates-as-timestamps
```

O Spring Boot 4 passou a usar **Jackson 3** (`tools.jackson.*`), e vários nomes de propriedade
mudaram em relação ao Jackson 2 (`com.fasterxml.jackson.*`).

A solução adotada foi declarar o formato **explicitamente** nos DTOs, com
`@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")`. Vantagens: não depende do
comportamento padrão de uma versão de biblioteca e deixa o contrato visível junto do DTO.

### 4. Um classpath montado à mão pode esconder a versão errada de uma biblioteca

Durante a verificação neste ambiente offline, o classpath era montado manualmente. O
agrupamento usava o **nome do arquivo** para decidir qual versão de cada artefato manter — e
`tools.jackson.core:jackson-databind` (3.1.5) e `com.fasterxml.jackson.core:jackson-databind`
(2.21.5) têm o mesmo nome de arquivo. Um dos dois era descartado em silêncio, e a aplicação
rodava com a versão errada do Jackson sem emitir nenhum erro.

A correção foi incluir o **`groupId`** na chave de agrupamento. A lição vale além deste
ambiente: quando duas coordenadas Maven compartilham o `artifactId`, a identidade do artefato
é o par `groupId:artifactId`, nunca o nome do arquivo.

### 5. Um handler genérico demais rebaixa todo erro do cliente para 500

Ao escrever o tratamento de erros, a intenção era simples: garantir que nada retornasse erro
genérico. O resultado foi o oposto do pretendido. Um `@ExceptionHandler(Exception.class)` largo
demais **captura antes** do tratamento que o próprio Spring já dava a vários erros do cliente —
e os transforma em 500:

```text
POST /api/v1/titulos  {"descricao": "x",          ->  500  (deveria ser 400)
GET  /api/v1/titulos?tipo=XYZ                     ->  500  (deveria ser 400)
GET  /api/v1/titulos            (sem usuarioId)   ->  500  (deveria ser 400)
GET  /api/v1/rota-inexistente                     ->  500  (deveria ser 404)
DELETE /api/v1/movimentacoes/1                    ->  500  (deveria ser 405)
```

Pior: as mensagens passaram a vazar detalhe interno, porque o handler genérico repassava a
mensagem crua da exceção — incluindo o SQL executado e o nome da constraint.

A correção foi adicionar tratadores **específicos** para cada uma dessas situações. Eles têm
precedência sobre o genérico, que fica reduzido a uma última rede de proteção — com a mensagem
deliberadamente genérica e a causa completa registrada apenas no log.

**A lição:** um `catch (Exception)` amplo não é uma rede de segurança, é uma forma de apagar
informação. Quanto mais amplo o tratador, maior a chance de ele esconder um diagnóstico melhor
que já existia.

### 6. Concordância de gênero em mensagens montadas por concatenação

As mensagens de erro são montadas por concatenação com nomes de recursos — e o português cobrou
o preço. Três defeitos da mesma família apareceram:

| Mensagem gerada | Problema | Correção |
|---|---|---|
| `Não é possível excluir **esta** usuário` | artigo feminino com substantivo masculino | construção sem artigo: `Não dá para excluir usuário` |
| `**1 títulos vinculados**` | plural com quantidade 1 | formas singular e plural por parâmetro |
| `**1 movimentação vinculado**` | adjetivo masculino com substantivo feminino | gênero do dependente por parâmetro |
| `não pode ser usada em **uma** título` | artigo feminino com substantivo masculino | `não pode ser usada em título de tipo X` |

Cada caso exigia um dado diferente (número do verbo, número do substantivo, gênero do
substantivo, gênero do adjetivo). A solução foi **eliminar as construções que exigem artigo**
e passar explicitamente apenas o gênero do dependente, que é o único que não dá para deduzir do
contexto.

Nenhum desses erros quebra o programa — todos apareceram em testes de comparação de mensagem.
Mas em um sistema que se propõe a explicar seus erros, a clareza da frase é parte do requisito.

### 7. O parâmetro `modo` da DRE era apenas um rótulo

Defeito encontrado pelo usuário ao testar no Swagger: *"no DRE, quando troco o modo de previsto
para realizado, aparece nos 2 o resultado"*.

Estava certo. O `modo` era recebido, escrito no campo `modo` da resposta e **não filtrava
nada**: a DRE sempre devolvia `totalDespesasPrevistas` **e** `totalDespesasRealizadas`
preenchidos, em qualquer modo. O parâmetro existia, mas não fazia diferença — e o cliente não
tinha como saber qual número pertencia a qual regime.

**Correção:** o modo passou a decidir **quais valores saem** na resposta.

| Campo | `previsto` | `realizado` | `comparativo` |
|---|---|---|---|
| `totalReceitasPrevistas` / `totalDespesasPrevistas` | preenchido | `null` | preenchido |
| `totalReceitasRealizadas` / `totalDespesasRealizadas` | `null` | preenchido | preenchido |
| `resultadoPrevisto` | preenchido | `null` | preenchido |
| `resultadoRealizado` | `null` | preenchido | preenchido |
| `variacaoResultado` | `null` | `null` | preenchido |
| valores dentro de cada linha | só previsto | só realizado | os dois |

Três detalhes da decisão:

- **`null`, não zero.** `null` diz "não foi apurado neste modo"; zero afirmaria "é zero", que é
  uma informação diferente e enganosa.
- **As listas continuam vindo sempre.** Uma despesa continua sendo despesa em qualquer modo, e
  devolver a lista como `null` obrigaria o cliente a tratar dois casos para percorrer o
  relatório. O que muda são os **valores** dentro de cada linha.
- **O modo passou a ser validado.** Um valor desconhecido gera 422 dizendo quais são aceitos, em
  vez de devolver silenciosamente uma apuração com o rótulo errado.

O cálculo da variação precisou de cuidado: como o modo anula um dos lados, os totais passaram a
ser calculados a partir das linhas **completas** (antes do recorte) e só depois o recorte é
aplicado para montar a resposta. Calcular os totais depois do recorte causava
`NullPointerException` — o que os testes pegaram.

### 8. Acentuação corrompida (mojibake) nos textos da API

Segundo defeito relatado pelo usuário: *"algumas palavras estão bugadas com acentuação"*.

A causa não era o Swagger nem o Spring: era **dupla codificação nos arquivos-fonte**, causada
por scripts PowerShell usados durante o desenvolvimento, que leram e gravaram os arquivos com
codificações incompatíveis. O sintoma é o clássico mojibake — um caractere acentuado vira dois:

```text
"Títulos"  correto: U+0054 U+00ED ...
"TÃtulos"  errado:  U+0054 U+00C3 U+00AD ...
```

Nos bytes, o "í" correto é `C3 AD`; corrompido vira `C3 83 C2 AD`.

**Correção:** os 12 arquivos afetados foram reprocessados com a operação inversa da dupla
codificação — ler o texto como UTF-8, recuperar os bytes originais via Latin-1 e reinterpretar
como UTF-8, aplicado diretamente nos bytes do arquivo. O README e o `ANALISE-E-PLANO.md` também
estavam afetados.

**Lição:** ao escrever arquivos com acento no Windows, use uma API com codificação explícita.
O `Set-Content -Encoding UTF8` do PowerShell 5.1 adiciona BOM, e o console exibe UTF-8 como
Latin-1 — o que faz um arquivo **correto** parecer corrompido e um **corrompido** passar
despercebido. Duas medições minhas seguiram essa pista falsa antes de eu comparar os bytes de
fato.

**Testes que travam a correção:** `contratoTemAcentuacaoCorreta` e `descricoesMantemAcentuacao`
verificam que o contrato contém `Títulos` com U+00ED e que **não** contém a sequência de
mojibake. Se a codificação quebrar de novo, o build falha.

---

## Decisões de projeto

Itens deliberadamente **fora** desta evolução, para manter o foco na modelagem financeira:

| Não implementado | Motivo |
|---|---|
| Autenticação (JWT, login, hash de senha) | Fora do escopo pedido. A API está aberta de forma **explícita e documentada** em `SecurityConfig`; o `usuarioId` é recebido nos requests. A evolução natural é obtê-lo do token. |
| Flyway / Liquibase | Um script SQL comentado cumpre o papel acadêmico sem adicionar dependência. |
| Tabela de DRE ou snapshot mensal | A DRE é derivada dos fatos; armazená-la criaria risco de divergência. |
| MapStruct, Specifications genéricas | Complexidade sem retorno em um projeto didático. |
| Front-end | O escopo é a API REST. |
| Entidades `Cartao`, `Orcamento`, `Meta` | Não pedidas. O modelo atual comporta acrescentá-las depois. |
| Estorno de movimentação (`DELETE`/`PUT`) | Decidido como fora do escopo: uma movimentação é um fato consumado (RN20). A API responde 405 explicando os métodos aceitos. |

### Notas de implementação

- **Ciclo de beans.** `MovimentacaoService` e `TituloService` não se injetam mutuamente: o
  `TituloService` depende do `MovimentacaoService`, e não o contrário. Para isso, as validações
  de quitação ficaram no `MovimentacaoService` (reaproveitadas pelo `TituloService`) e a
  mudança de situação do título é feita por uma consulta `@Modifying` no repositório. A
  alternativa — um `@Lazy` para mascarar o ciclo — esconderia a direção da dependência.
- **`spring.jpa.open-in-view=false`.** Todos os mappers convertem entidade → DTO dentro da
  transação, então nenhuma consulta preguiçosa é disparada na serialização. Desligar torna esse
  erro visível em vez de silencioso.
- **Contas a pagar e a receber são somente leitura.** Não há tabela, entidade nem campo novo;
  são visões sobre os títulos existentes. Ver
  [Contas a pagar e a receber](#contas-a-pagar-e-a-receber).

---

## Status

Funcional e em evolução. Estão implementados e cobertos por testes:

- as **5 tabelas** e o modelo de Título × Movimentação;
- as camadas `controller` / `service` / `repository` / `entity` / `dto` / `mapper` / `exception`;
- o CRUD de usuários, contas e categorias;
- o ciclo completo de títulos: cadastrar previsão, pagar, receber, cancelar, listar vencidos;
- movimentações avulsas e extrato por conta;
- a **DRE** nos modos previsto, realizado e comparativo;
- as **contas a pagar e a receber**, com totais, vencidos e janela de vencimentos;
- o **tratamento de erros** completo: 10 status HTTP distintos, formato único de resposta e
  tradução de mensagens sem vazamento de detalhe interno.

**65 testes de integração**, todos passando.

## Licença

Projeto acadêmico desenvolvido para estudo de Banco de Dados e desenvolvimento de APIs com
Spring Boot.
