Gerenciador de Finanças Pessoais

README / Documentação do Projeto

Sistema web para gerenciamento de finanças pessoais, desenvolvido com Java e Spring Boot, utilizando MySQL como banco de dados.

O objetivo do sistema é permitir que usuários acompanhem suas receitas, despesas, contas financeiras, recebimentos e pagamentos de forma organizada, possibilitando também a geração de informações financeiras, incluindo uma DRE simplificada.

A aplicação será desenvolvida inicialmente como uma API REST, permitindo futuramente integração com aplicações web, mobile ou outros clientes.

Descritivo do Projeto

O Gerenciador de Finanças Pessoais permitirá ao usuário registrar acontecimentos financeiros e acompanhar tanto o resultado por competência quanto a movimentação efetiva de caixa.

O sistema contará com funcionalidades para:

Cadastro e gerenciamento de usuários

Autenticação e autorização de usuários

Cadastro e gerenciamento de categorias

Cadastro e gerenciamento de contas financeiras

Registro de receitas

Registro de despesas

Registro de recebimentos

Registro de pagamentos

Edição e exclusão de movimentações

Associação das movimentações às categorias

Associação de recebimentos e pagamentos às contas

Consulta do histórico financeiro

Controle de valores a receber e a pagar

Cálculo de saldo por conta

Cálculo do saldo geral

Filtros por período, categoria, conta e tipo

Geração de resumo financeiro

Geração de relatório de DRE simplificada

Tecnologias

Backend

Java

Spring Boot

Spring Web

Spring Data JPA

Spring Security

Hibernate

Bean Validation

Banco de Dados

MySQL

Ferramentas

Maven

Git

GitHub

Postman ou Insomnia

IntelliJ IDEA / Eclipse / VS Code

Testes

JUnit

Mockito

Spring Boot Test

Arquitetura

O projeto seguirá uma arquitetura baseada em camadas, buscando separar as responsabilidades da aplicação.

src
│
├── main
│   ├── java
│   │   └── com.example.financas
│   │       ├── controller
│   │       ├── service
│   │       ├── repository
│   │       ├── entity
│   │       ├── dto
│   │       ├── mapper
│   │       ├── exception
│   │       ├── config
│   │       └── security
│   │
│   └── resources
│       └── application.properties
│
└── test

Controller

Responsável por receber as requisições HTTP e disponibilizar os endpoints da API.

Service

Responsável pelas regras de negócio da aplicação.

Repository

Responsável pela comunicação com o banco de dados utilizando Spring Data JPA.

Entity

Representa as entidades persistidas no banco de dados.

DTO

Responsável por transportar dados entre as diferentes camadas da aplicação, evitando a exposição direta das entidades.

Mapper

Responsável pela conversão entre Entities e DTOs.

Exception

Centraliza o tratamento das exceções e erros da aplicação.

Security

Responsável pela autenticação, autorização e controle de acesso.

Requisitos Funcionais

Usuários

Cadastrar usuário

Realizar login

Atualizar dados do usuário

Alterar senha

Desativar conta

Controlar acesso aos recursos da aplicação

Movimentações

A movimentação representa o fato financeiro reconhecido para fins de acompanhamento por competência.

Uma movimentação poderá ser:

RECEITA

DESPESA

Funcionalidades:

Cadastrar movimentação

Editar movimentação

Excluir movimentação

Consultar movimentações

Filtrar movimentações por período

Filtrar movimentações por categoria

Associar categoria à movimentação

Uma movimentação não representa necessariamente que o dinheiro já entrou ou saiu da conta.

Exemplo:

Venda no valor de R$ 1.200,00
Data da receita: 01/09

Recebimentos:
R$ 400,00 em 01/09
R$ 400,00 em 01/10
R$ 400,00 em 01/11

A receita da movimentação permanece em R$ 1.200,00, enquanto os recebimentos registram quando o dinheiro efetivamente entrou.

Categorias

Cada usuário poderá possuir suas próprias categorias.

Exemplos:

Alimentação

Transporte

Moradia

Salário

Lazer

Educação

Saúde

Investimentos

Cada categoria terá um tipo:

RECEITA

DESPESA

Regras:

Uma categoria pertence a apenas um usuário.

Um usuário pode possuir várias categorias.

O mesmo usuário não poderá possuir duas categorias com o mesmo nome.

O tipo da categoria deve ser compatível com o tipo da movimentação.

 Contas Financeiras

O usuário poderá cadastrar diferentes contas para organizar o dinheiro.

Exemplos:

Conta Corrente

Poupança

Carteira

Conta Digital

Cada conta possuirá um saldo inicial.

O saldo atual será calculado considerando os recebimentos e pagamentos efetivamente registrados:

SALDO ATUAL = SALDO INICIAL + RECEBIMENTOS - PAGAMENTOS

Regras:

Uma conta pertence a apenas um usuário.

Um usuário pode possuir várias contas.

O mesmo usuário não poderá possuir duas contas com o mesmo nome.

 Recebimentos

A tabela de recebimentos representa a entrada efetiva de dinheiro relacionada a uma movimentação do tipo RECEITA.

Exemplo:

Movimentação:
Receita de R$ 1.200,00

Recebimentos:
1. R$ 500,00 em 10/09 na Conta Corrente
2. R$ 700,00 em 10/10 na Conta Corrente

Cada recebimento deverá estar associado a:

Uma movimentação

Uma conta financeira

Relacionamentos:

MOVIMENTACAO 1:N RECEBIMENTO
CONTA        1:N RECEBIMENTO

Regras:

O valor do recebimento deve ser maior que zero.

O recebimento deve estar vinculado a uma movimentação do tipo RECEITA.

O total dos recebimentos não deve ultrapassar o valor da movimentação.

A conta utilizada no recebimento deve pertencer ao mesmo usuário da movimentação.

 Pagamentos

A tabela de pagamentos representa a saída efetiva de dinheiro relacionada a uma movimentação do tipo DESPESA.

Exemplo:

Movimentação:
Despesa de R$ 900,00

Pagamentos:
1. R$ 300,00 em 05/09 na Conta Corrente
2. R$ 600,00 em 05/10 na Conta Corrente

Cada pagamento deverá estar associado a:

Uma movimentação

Uma conta financeira

Relacionamentos:

MOVIMENTACAO 1:N PAGAMENTO
CONTA        1:N PAGAMENTO

Regras:

O valor do pagamento deve ser maior que zero.

O pagamento deve estar vinculado a uma movimentação do tipo DESPESA.

O total dos pagamentos não deve ultrapassar o valor da movimentação.

A conta utilizada no pagamento deve pertencer ao mesmo usuário da movimentação.

📊 DRE — Demonstração do Resultado do Exercício

A DRE será tratada como um relatório calculado a partir das movimentações financeiras reconhecidas no período.

A DRE não será armazenada como uma tabela própria no banco de dados.

A estrutura básica será:

DRE
│
├── RECEITAS
│      ├── Receita 1
│      ├── Receita 2
│      └── ...
│
├── DESPESAS
│      ├── Despesa 1
│      ├── Despesa 2
│      └── ...
│
└── RESULTADO

Cálculo simplificado:

RESULTADO = TOTAL DE RECEITAS - TOTAL DE DESPESAS

A DRE utiliza as movimentações e suas datas de competência, enquanto recebimentos e pagamentos representam o fluxo efetivo de caixa.

🗄 Modelo do Banco de Dados

O banco de dados será composto inicialmente por seis entidades principais:

USUARIO

CATEGORIA

CONTA

MOVIMENTACAO

RECEBIMENTO

PAGAMENTO

👤 USUARIO

Campo

Tipo

Restrições

Descrição

id_usuario

BIGINT

PK, AUTO_INCREMENT

Identificador do usuário

nome

VARCHAR(100)

NOT NULL

Nome do usuário

email

VARCHAR(150)

NOT NULL, UNIQUE

Email do usuário

senha

VARCHAR(255)

NOT NULL

Hash da senha

data_cadastro

DATETIME

NOT NULL

Data de cadastro

ativo

BOOLEAN

NOT NULL, DEFAULT TRUE

Status da conta

Relacionamentos:

USUARIO 1:N CATEGORIA
USUARIO 1:N CONTA
USUARIO 1:N MOVIMENTACAO

🏷 CATEGORIA

Campo

Tipo

Restrições

Descrição

id_categoria

BIGINT

PK, AUTO_INCREMENT

Identificador da categoria

nome

VARCHAR(100)

NOT NULL

Nome da categoria

tipo

ENUM

NOT NULL

RECEITA ou DESPESA

usuario_id

BIGINT

FK, NOT NULL

Usuário proprietário

Restrição:

UNIQUE(nome, usuario_id)

🏦 CONTA

Campo

Tipo

Restrições

Descrição

id_conta

BIGINT

PK, AUTO_INCREMENT

Identificador da conta

nome

VARCHAR(100)

NOT NULL

Nome da conta

tipo

VARCHAR(50)

NOT NULL

Tipo da conta

saldo_inicial

DECIMAL(15,2)

NOT NULL, DEFAULT 0.00

Saldo inicial

usuario_id

BIGINT

FK, NOT NULL

Usuário proprietário

Restrição:

UNIQUE(nome, usuario_id)

💸 MOVIMENTACAO

Campo

Tipo

Restrições

Descrição

id_movimentacao

BIGINT

PK, AUTO_INCREMENT

Identificador

descricao

VARCHAR(255)

NOT NULL

Descrição

valor

DECIMAL(15,2)

NOT NULL, CHECK > 0

Valor reconhecido

tipo

ENUM

NOT NULL

RECEITA ou DESPESA

data

DATE

NOT NULL

Data da movimentação

usuario_id

BIGINT

FK, NOT NULL

Usuário responsável

categoria_id

BIGINT

FK, NOT NULL

Categoria

Relacionamentos:

USUARIO   1:N MOVIMENTACAO
CATEGORIA 1:N MOVIMENTACAO

📥 RECEBIMENTO

Campo

Tipo

Restrições

Descrição

id_recebimento

BIGINT

PK, AUTO_INCREMENT

Identificador

valor

DECIMAL(15,2)

NOT NULL, CHECK > 0

Valor efetivamente recebido

data_recebimento

DATE

NOT NULL

Data da entrada

movimentacao_id

BIGINT

FK, NOT NULL

Receita de origem

conta_id

BIGINT

FK, NOT NULL

Conta que recebeu o dinheiro

Relacionamentos:

MOVIMENTACAO 1:N RECEBIMENTO
CONTA        1:N RECEBIMENTO

📤 PAGAMENTO

Campo

Tipo

Restrições

Descrição

id_pagamento

BIGINT

PK, AUTO_INCREMENT

Identificador

valor

DECIMAL(15,2)

NOT NULL, CHECK > 0

Valor efetivamente pago

data_pagamento

DATE

NOT NULL

Data da saída

movimentacao_id

BIGINT

FK, NOT NULL

Despesa de origem

conta_id

BIGINT

FK, NOT NULL

Conta de onde saiu o dinheiro

Relacionamentos:

MOVIMENTACAO 1:N PAGAMENTO
CONTA        1:N PAGAMENTO

🔗 Relacionamentos Definitivos

                         USUARIO
                    ┌───────────────┐
                    │ id_usuario PK │
                    │ nome          │
                    │ email         │
                    │ senha         │
                    │ data_cadastro │
                    │ ativo         │
                    └───────┬───────┘
                            │
             ┌──────────────┼──────────────┐
             │              │              │
            1:N            1:N            1:N
             │              │              │
             ▼              ▼              ▼
       CATEGORIA          CONTA       MOVIMENTACAO
             │              │              │
            1:N            1:N          ┌───┴───┐
             │              │           │       │
             │              │          1:N     1:N
             │              │           │       │
             │              └──────┐    ▼       ▼
             │                     │ RECEBIMENTO PAGAMENTO
             └─────────────────────┴───────────────

Resumo das cardinalidades:

Relacionamento

Cardinalidade

USUARIO → CATEGORIA

1

USUARIO → CONTA

1

USUARIO → MOVIMENTACAO

1

CATEGORIA → MOVIMENTACAO

1

MOVIMENTACAO → RECEBIMENTO

1

MOVIMENTACAO → PAGAMENTO

1

CONTA → RECEBIMENTO

1

CONTA → PAGAMENTO

1

 Regras de Integridade e Negócio

O email do usuário deve ser único.

A senha deve ser armazenada somente em formato de hash seguro.

Um usuário pode possuir várias categorias, contas e movimentações.

O mesmo usuário não pode possuir categorias com nomes repetidos.

O mesmo usuário não pode possuir contas com nomes repetidos.

O valor de uma movimentação deve ser maior que zero.

O valor de um recebimento deve ser maior que zero.

O valor de um pagamento deve ser maior que zero.

Categorias do tipo RECEITA só podem ser usadas por movimentações RECEITA.

Categorias do tipo DESPESA só podem ser usadas por movimentações DESPESA.

Recebimentos só podem estar associados a movimentações RECEITA.

Pagamentos só podem estar associados a movimentações DESPESA.

A soma dos recebimentos não deve ultrapassar o valor da receita correspondente.

A soma dos pagamentos não deve ultrapassar o valor da despesa correspondente.

A conta utilizada em um recebimento deve pertencer ao mesmo usuário da movimentação.

A conta utilizada em um pagamento deve pertencer ao mesmo usuário da movimentação.

O saldo atual da conta é calculado pelos movimentos de caixa.

A DRE é gerada a partir das movimentações e não armazenada como tabela.

A exclusão física de usuários não será utilizada como regra principal; o campo ativo permite desativação lógica.

Categorias e contas que possuam histórico financeiro deverão ser protegidas contra exclusão indevida.

 Saldo e Fluxo de Caixa

O saldo da conta representa o dinheiro efetivamente disponível.

SALDO ATUAL = SALDO INICIAL + RECEBIMENTOS - PAGAMENTOS

Exemplo:

Saldo inicial:       R$ 1.000,00
Recebimentos:      + R$ 3.000,00
Pagamentos:        - R$ 1.200,00
                    ----------------
Saldo atual:         R$ 2.800,00

 DRE x Fluxo de Caixa

O sistema terá duas perspectivas financeiras diferentes.

DRE / Competência

Utiliza:

MOVIMENTACAO

Representa o resultado do período.

RECEITAS - DESPESAS = RESULTADO

Caixa

Utiliza:

RECEBIMENTOS
PAGAMENTOS

Representa a movimentação efetiva do dinheiro.

RECEBIMENTOS - PAGAMENTOS = VARIAÇÃO DO CAIXA

Essa separação permite que uma receita ou despesa seja registrada antes de o dinheiro efetivamente entrar ou sair da conta.

 Principais Endpoints

Autenticação

POST /auth/register
POST /auth/login

Usuários

GET    /users/me
PUT    /users/me
DELETE /users/me

Movimentações

GET    /transactions
GET    /transactions/{id}
POST   /transactions
PUT    /transactions/{id}
DELETE /transactions/{id}

Categorias

GET    /categories
POST   /categories
PUT    /categories/{id}
DELETE /categories/{id}

Contas

GET    /accounts
GET    /accounts/{id}
POST   /accounts
PUT    /accounts/{id}
DELETE /accounts/{id}

Recebimentos

GET    /receipts
GET    /receipts/{id}
POST   /receipts
PUT    /receipts/{id}
DELETE /receipts/{id}

Pagamentos

GET    /payments
GET    /payments/{id}
POST   /payments
PUT    /payments/{id}
DELETE /payments/{id}

Dashboard / Relatórios

GET /dashboard
GET /dashboard/summary
GET /reports/dre
GET /reports/cash-flow

 Segurança

A aplicação utilizará Spring Security para controle de autenticação e autorização.

A autenticação poderá ser implementada utilizando JWT (JSON Web Token).

As senhas deverão ser armazenadas utilizando algoritmo seguro de hash, como BCrypt.

Fluxo básico:

Usuário
   ↓
Login
   ↓
API
   ↓
Validação das credenciais
   ↓
JWT
   ↓
Requisições autenticadas
   ↓
Spring Security
   ↓
Recursos protegidos

 Testes

O projeto deverá possuir testes para:

Regras de negócio

Services

Controllers

Validação de dados

Autenticação

Autorização

Recebimentos e pagamentos

Cálculo de saldo

Geração da DRE

Tratamento de exceções

Tecnologias:

JUnit

Mockito

Spring Boot Test

 Documentação da API

A API poderá utilizar Swagger/OpenAPI para facilitar a visualização e o teste dos endpoints.

http://localhost:8080/swagger-ui.html

🚀 Como Executar o Projeto

1. Clonar o repositório

git clone https://github.com/seu-usuario/gerenciador-financas.git

2. Entrar no diretório

cd gerenciador-financas

3. Criar o banco

CREATE DATABASE financas;

4. Configurar o banco

Configure as credenciais do MySQL no arquivo application.properties ou através de variáveis de ambiente.

5. Executar a aplicação

Linux:

./mvnw spring-boot:run

Windows:

mvnw.cmd spring-boot:run

A aplicação estará disponível em:

http://localhost:8080

🗺 Roadmap

Fase 1 — Estrutura inicial

Criar projeto Spring Boot

Configurar Maven

Configurar MySQL

Criar entidades e relacionamentos

Fase 2 — Usuários e segurança

Cadastro de usuários

Login

Spring Security

JWT

Controle de acesso

Fase 3 — Categorias e contas

CRUD de categorias

CRUD de contas

Saldo inicial

Fase 4 — Movimentações

CRUD de movimentações

Receitas

Despesas

Associação com categorias

Fase 5 — Recebimentos e pagamentos

Registro de recebimentos

Registro de pagamentos

Controle de valores parciais

Atualização do fluxo de caixa

Fase 6 — Relatórios

Dashboard

DRE simplificada

Fluxo de caixa

Relatórios por período

Relatórios por categoria

Relatórios por conta

Fase 7 — Qualidade

Testes unitários

Testes de integração

Swagger

Tratamento de exceções

Melhorias de segurança

Dockerização

Fase 8 — Futuras funcionalidades

Transferências entre contas

Metas financeiras

Investimentos

Despesas recorrentes

Receitas recorrentes

Notificações

Exportação de relatórios

Gráficos financeiros

Aplicação frontend

Aplicativo mobile

 Estrutura do Projeto

gerenciador-financas/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/financas/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       ├── dto/
│   │   │       ├── mapper/
│   │   │       ├── exception/
│   │   │       ├── config/
│   │   │       └── security/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── .gitignore
├── pom.xml
└── README.md

 Contribuição

Contribuições são bem-vindas.

git checkout -b feature/nova-funcionalidade
git add .
git commit -m "feat: adiciona nova funcionalidade"
git push origin feature/nova-funcionalidade

Em seguida, abra um Pull Request.

 Padrão de Commits

O projeto seguirá preferencialmente o padrão Conventional Commits.

feat: adiciona cadastro de usuários
feat: implementa autenticação JWT
feat: adiciona movimentações financeiras
feat: adiciona recebimentos
feat: adiciona pagamentos
feat: implementa relatório de DRE
fix: corrige cálculo do saldo
refactor: reorganiza camada de serviços
test: adiciona testes para transaction service
docs: atualiza documentação da API
chore: atualiza dependências
