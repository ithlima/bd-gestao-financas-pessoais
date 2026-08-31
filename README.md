# 💰 Gerenciador de Finanças Pessoais

## README / Documentação do Projeto

Sistema web para gerenciamento de finanças pessoais, desenvolvido com Java e Spring Boot, utilizando MySQL como banco de dados.

O objetivo do sistema é permitir que usuários acompanhem suas receitas, despesas, contas financeiras e movimentações de forma simples e organizada.

A aplicação será desenvolvida inicialmente como uma API REST, permitindo futuramente integração com aplicações web, mobile ou outros clientes.

---

# 📋 Descritivo do Projeto

O Gerenciador de Finanças Pessoais permitirá ao usuário registrar e acompanhar suas movimentações financeiras, separando receitas e despesas e oferecendo uma visão geral de sua situação financeira.

O sistema contará com funcionalidades para:

* Cadastro e gerenciamento de usuários
* Autenticação e autorização de usuários
* Registro de receitas
* Registro de despesas
* Edição e exclusão de movimentações
* Categorização das movimentações
* Controle de contas financeiras
* Consulta do histórico financeiro
* Cálculo de saldo por conta
* Cálculo do saldo geral
* Filtros por período
* Filtros por categoria
* Visualização de receitas e despesas
* Geração de resumo financeiro

---

# 🛠 Tecnologias

## Backend

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* Hibernate
* Bean Validation

## Banco de Dados

* MySQL

## Ferramentas

* Maven
* Git
* GitHub
* Postman ou Insomnia
* IntelliJ IDEA / Eclipse / VS Code

## Testes

* JUnit
* Mockito
* Spring Boot Test

---

# 🏗 Arquitetura

O projeto seguirá uma arquitetura baseada em camadas, buscando separar as responsabilidades da aplicação.

```text
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
```

## Controller

Responsável por receber as requisições HTTP e disponibilizar os endpoints da API.

## Service

Responsável pelas regras de negócio da aplicação.

## Repository

Responsável pela comunicação com o banco de dados utilizando Spring Data JPA.

## Entity

Representa as entidades persistidas no banco de dados.

## DTO

Responsável por transportar dados entre as diferentes camadas da aplicação, evitando a exposição direta das entidades.

## Mapper

Responsável pela conversão entre Entities e DTOs.

## Exception

Centraliza o tratamento das exceções e erros da aplicação.

## Security

Responsável pela autenticação, autorização e controle de acesso.

---

# 📌 Requisitos Funcionais

## 👤 Usuários

* Cadastrar usuário
* Realizar login
* Atualizar dados do usuário
* Alterar senha
* Desativar conta
* Controlar acesso aos recursos da aplicação

---

## 💰 Movimentações

O sistema utilizará uma única estrutura para representar movimentações financeiras.

Uma movimentação poderá ser:

* RECEITA
* DESPESA

Funcionalidades:

* Cadastrar movimentação
* Editar movimentação
* Excluir movimentação
* Consultar movimentações
* Filtrar movimentações por período
* Filtrar movimentações por categoria
* Associar movimentações a contas
* Categorizar movimentações

---

## 🏷 Categorias

Cada usuário poderá possuir suas próprias categorias.

Exemplos:

* Alimentação
* Transporte
* Moradia
* Salário
* Lazer
* Educação
* Saúde
* Investimentos

Funcionalidades:

* Criar categorias
* Editar categorias
* Excluir categorias
* Consultar categorias
* Associar categorias às movimentações

As categorias poderão possuir dois tipos:

* RECEITA
* DESPESA

---

## 🏦 Contas Financeiras

O usuário poderá cadastrar diferentes contas para organizar seu dinheiro.

Exemplos:

* Conta Corrente
* Poupança
* Carteira
* Conta Digital

Funcionalidades:

* Criar conta
* Editar conta
* Excluir conta
* Consultar contas
* Visualizar saldo por conta

Cada movimentação deverá estar associada a uma conta financeira.

---

## 📊 Controle Financeiro

O sistema deverá permitir:

* Calcular saldo geral
* Calcular saldo por conta
* Calcular total de receitas
* Calcular total de despesas
* Consultar movimentações por período
* Consultar movimentações por categoria
* Gerar resumo financeiro

---

# ⚙ Requisitos Não Funcionais

* A aplicação deve utilizar uma API REST.
* Os dados devem ser persistidos em banco de dados MySQL.
* A aplicação deve utilizar autenticação e autorização.
* Senhas não devem ser armazenadas em texto puro.
* Os endpoints devem possuir validação dos dados recebidos.
* A aplicação deve possuir tratamento centralizado de exceções.
* O código deve seguir princípios de organização e separação de responsabilidades.
* A API deve retornar códigos HTTP apropriados.
* O projeto deve possuir documentação dos principais endpoints.
* O sistema deve ser preparado para futuras expansões.

---

# 🗄 Modelagem do Banco de Dados

O sistema será composto inicialmente por quatro entidades principais:

* Usuário
* Categoria
* Conta
* Movimentação

---

# 👤 User

Representa os usuários do sistema.

| Campo        | Tipo     | Descrição                |
| ------------ | -------- | ------------------------ |
| id           | BIGINT   | Identificador do usuário |
| nome         | VARCHAR  | Nome do usuário          |
| email        | VARCHAR  | Email único do usuário   |
| senha        | VARCHAR  | Senha criptografada      |
| dataCadastro | DATETIME | Data de cadastro         |
| ativo        | BOOLEAN  | Status da conta          |

### Regras

* O email deve ser único.
* A senha nunca deve ser armazenada em texto puro.
* Um usuário pode possuir várias categorias.
* Um usuário pode possuir várias contas.
* Um usuário pode possuir várias movimentações.

---

# 🏷 Category

Representa as categorias utilizadas para organizar receitas e despesas.

| Campo   | Tipo    | Descrição                         |
| ------- | ------- | --------------------------------- |
| id      | BIGINT  | Identificador da categoria        |
| nome    | VARCHAR | Nome da categoria                 |
| tipo    | ENUM    | RECEITA ou DESPESA                |
| usuario | FK      | Usuário proprietário da categoria |

### Exemplos

```text
Alimentação
Transporte
Moradia
Salário
Lazer
Educação
Saúde
Investimentos
```

### Regras

* Uma categoria pertence a apenas um usuário.
* Um usuário pode possuir várias categorias.
* Uma categoria pode estar associada a várias movimentações.
* O tipo da categoria deve ser compatível com o tipo da movimentação.

Exemplo:

```text
Categoria: Salário
Tipo: RECEITA
```

Não deverá ser possível utilizar essa categoria em uma movimentação do tipo DESPESA.

---

# 🏦 Account

Representa uma conta financeira do usuário.

| Campo        | Tipo    | Descrição              |
| ------------ | ------- | ---------------------- |
| id           | BIGINT  | Identificador da conta |
| nome         | VARCHAR | Nome da conta          |
| tipo         | VARCHAR | Tipo da conta          |
| saldoInicial | DECIMAL | Saldo inicial da conta |
| usuario      | FK      | Usuário proprietário   |

### Exemplos

```text
Conta Corrente
Poupança
Carteira
Conta Digital
```

### Regras

* Uma conta pertence a apenas um usuário.
* Um usuário pode possuir várias contas.
* Uma conta pode possuir várias movimentações.

O saldo atual da conta não será necessariamente armazenado diretamente.

Ele poderá ser calculado utilizando:

```text
Saldo Atual = Saldo Inicial + Receitas - Despesas
```

Dessa forma evitamos inconsistências entre o saldo armazenado e as movimentações registradas.

---

# 💸 Transaction / Movimentação

Representa qualquer movimentação financeira realizada pelo usuário.

Uma movimentação poderá ser:

* RECEITA
* DESPESA

| Campo     | Tipo    | Descrição                     |
| --------- | ------- | ----------------------------- |
| id        | BIGINT  | Identificador da movimentação |
| descricao | VARCHAR | Descrição da movimentação     |
| valor     | DECIMAL | Valor da movimentação         |
| tipo      | ENUM    | RECEITA ou DESPESA            |
| data      | DATE    | Data da movimentação          |
| usuario   | FK      | Usuário responsável           |
| categoria | FK      | Categoria da movimentação     |
| conta     | FK      | Conta financeira associada    |

### Exemplos

#### Receita

```text
Descrição: Salário
Valor: R$ 2.500
Tipo: RECEITA
Categoria: Salário
Conta: Conta Corrente
```

#### Despesa

```text
Descrição: Supermercado
Valor: R$ 300
Tipo: DESPESA
Categoria: Alimentação
Conta: Conta Corrente
```

### Regras

* Uma movimentação pertence a um usuário.
* Uma movimentação pertence a uma categoria.
* Uma movimentação pertence a uma conta.
* Uma categoria pode possuir várias movimentações.
* Uma conta pode possuir várias movimentações.
* O valor da movimentação deve ser maior que zero.
* O tipo da movimentação deve ser RECEITA ou DESPESA.

---

# 🔗 Relacionamentos

## Usuário e Categoria

```text
USUARIO 1 -------- N CATEGORIA
```

Um usuário pode possuir várias categorias.

Uma categoria pertence a apenas um usuário.

---

## Usuário e Conta

```text
USUARIO 1 -------- N CONTA
```

Um usuário pode possuir várias contas.

Uma conta pertence a apenas um usuário.

---

## Usuário e Movimentação

```text
USUARIO 1 -------- N MOVIMENTACAO
```

Um usuário pode registrar várias movimentações.

Uma movimentação pertence a apenas um usuário.

---

## Categoria e Movimentação

```text
CATEGORIA 1 -------- N MOVIMENTACAO
```

Uma categoria pode estar associada a várias movimentações.

Uma movimentação possui uma categoria.

---

## Conta e Movimentação

```text
CONTA 1 -------- N MOVIMENTACAO
```

Uma conta pode possuir várias movimentações.

Uma movimentação pertence a uma conta.

---

# 📊 Modelo Conceitual

```text
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
             ┌───────────┼───────────┐
             │           │           │
            1:N         1:N         1:N
             │           │           │
             ▼           ▼           ▼

        CATEGORIA      CONTA    MOVIMENTACAO
        ┌─────────┐   ┌──────┐  ┌────────────────┐
        │ id PK   │   │ id PK│  │ id PK          │
        │ nome    │   │ nome │  │ descricao      │
        │ tipo    │   │ tipo │  │ valor          │
        │ user FK │   │saldo │  │ tipo           │
        └────┬────┘   │ini.  │  │ data           │
             │        │userFK│  │ usuario FK     │
             │        └───┬──┘  │ categoria FK   │
             │            │     │ conta FK       │
             └─────1:N────┼─────┘
```

---

# 🔮 Expansões Futuras

A modelagem será preparada para futuras funcionalidades.

## Transferências entre contas

Uma transferência não representa exatamente uma receita ou despesa.

Exemplo:

```text
Conta Corrente
      ↓
   R$ 500
      ↓
Poupança
```

No futuro poderá ser criada uma entidade específica:

```text
TRANSFERENCIA
----------------------
id
valor
data
conta_origem
conta_destino
usuario
```

---

## Outras funcionalidades futuras

* Metas financeiras
* Investimentos
* Despesas recorrentes
* Receitas recorrentes
* Notificações
* Exportação de relatórios
* Gráficos financeiros
* Aplicação frontend
* Aplicativo mobile

---

# 🌐 Principais Endpoints

## Autenticação

```http
POST /auth/register
POST /auth/login
```

---

## Usuários

```http
GET    /users/me
PUT    /users/me
DELETE /users/me
```

---

## Movimentações

```http
GET    /transactions
GET    /transactions/{id}
POST   /transactions
PUT    /transactions/{id}
DELETE /transactions/{id}
```

Filtros futuros:

```http
GET /transactions?dataInicio=2026-01-01
GET /transactions?dataFim=2026-12-31
GET /transactions?categoriaId=1
GET /transactions?contaId=1
GET /transactions?tipo=DESPESA
```

---

## Categorias

```http
GET    /categories
POST   /categories
PUT    /categories/{id}
DELETE /categories/{id}
```

---

## Contas

```http
GET    /accounts
GET    /accounts/{id}
POST   /accounts
PUT    /accounts/{id}
DELETE /accounts/{id}
```

---

## Dashboard

```http
GET /dashboard
GET /dashboard/summary
```

O dashboard poderá apresentar:

```json
{
    "totalReceitas": 5000.00,
    "totalDespesas": 3200.00,
    "saldo": 1800.00
}
```

---

# 🔐 Segurança

A aplicação utilizará Spring Security para controle de autenticação e autorização.

A autenticação poderá ser implementada utilizando JWT (JSON Web Token).

Fluxo básico:

```text
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
```

As senhas deverão ser armazenadas utilizando um algoritmo seguro de hash, como BCrypt.

---

# 🗄 Banco de Dados

O projeto utilizará MySQL como banco de dados relacional.

Nome inicial do banco:

```sql
CREATE DATABASE financas;
```

Configuração inicial:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/financas
spring.datasource.username=root
spring.datasource.password=sua_senha

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Atenção: credenciais reais do banco de dados não devem ser enviadas para o GitHub.

Recomenda-se utilizar:

* Variáveis de ambiente
* Arquivos de configuração locais
* `.env`

---

# 🧪 Testes

O projeto deverá possuir testes para:

* Regras de negócio
* Services
* Controllers
* Validação de dados
* Autenticação
* Autorização
* Tratamento de exceções

Tecnologias:

* JUnit
* Mockito
* Spring Boot Test

---

# 📚 Documentação da API

A API poderá utilizar Swagger/OpenAPI para facilitar a visualização e teste dos endpoints.

Exemplo:

```text
http://localhost:8080/swagger-ui.html
```

A documentação deverá apresentar:

* Endpoints
* Métodos HTTP
* Parâmetros
* Corpo das requisições
* Respostas
* Códigos HTTP
* Autenticação

---

# 🚀 Como Executar o Projeto

## 1. Clonar o repositório

```bash
git clone https://github.com/seu-usuario/gerenciador-financas.git
```

## 2. Entrar no diretório

```bash
cd gerenciador-financas
```

## 3. Criar o banco

```sql
CREATE DATABASE financas;
```

## 4. Configurar o banco

Configure as credenciais do MySQL no arquivo:

```text
application.properties
```

Ou através de variáveis de ambiente.

## 5. Executar a aplicação

Linux:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

A aplicação estará disponível em:

```text
http://localhost:8080
```

---

# 🗺 Roadmap

## Fase 1 — Estrutura Inicial

* Criar projeto Spring Boot
* Configurar Maven
* Configurar MySQL
* Criar estrutura de pacotes
* Criar entidades principais
* Criar relacionamentos do banco

## Fase 2 — Usuários e Segurança

* Cadastro de usuários
* Login
* Spring Security
* JWT
* Controle de acesso

## Fase 3 — Categorias e Contas

* CRUD de categorias
* CRUD de contas financeiras
* Cálculo de saldo

## Fase 4 — Movimentações

* CRUD de movimentações
* Registro de receitas
* Registro de despesas
* Associação com categorias
* Associação com contas
* Filtros

## Fase 5 — Relatórios

* Dashboard
* Resumo financeiro
* Relatórios por período
* Relatórios por categoria
* Relatórios por conta
* Indicadores financeiros

## Fase 6 — Qualidade

* Testes unitários
* Testes de integração
* Swagger
* Tratamento de exceções
* Melhorias de segurança
* Dockerização

## Fase 7 — Futuras Funcionalidades

* Transferências entre contas
* Aplicação frontend
* Aplicativo mobile
* Notificações
* Metas financeiras
* Investimentos
* Despesas recorrentes
* Receitas recorrentes
* Exportação de relatórios
* Gráficos financeiros

---

# 📁 Estrutura do Projeto

```text
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
```

---

# 🤝 Contribuição

Contribuições são bem-vindas.

```bash
git checkout -b feature/nova-funcionalidade
git add .
git commit -m "feat: adiciona nova funcionalidade"
git push origin feature/nova-funcionalidade
```

Em seguida, abra um Pull Request.

---

# 📝 Padrão de Commits

O projeto seguirá preferencialmente o padrão Conventional Commits.

```text
feat: adiciona cadastro de usuários
feat: implementa autenticação JWT
feat: adiciona movimentações financeiras
fix: corrige cálculo do saldo
refactor: reorganiza camada de serviços
test: adiciona testes para transaction service
docs: atualiza documentação da API
chore: atualiza dependências
```
