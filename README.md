Gerenciador de Finanças Pessoais

API REST para gerenciamento de finanças pessoais, desenvolvida com Java, Spring Boot e MySQL.

Sobre o projeto

O sistema permite registrar receitas e despesas, organizar categorias e contas financeiras e acompanhar o fluxo de caixa por meio de recebimentos e pagamentos.

A modelagem também permite gerar uma DRE simplificada a partir das movimentações registradas por competência.

Tecnologias

Java 21

Spring Boot 3.x

Spring Web

Spring Data JPA

Spring Security

Bean Validation

MySQL 8+

Maven

JUnit / Mockito

Arquitetura

O projeto utiliza arquitetura em camadas:

src/main/java/com/example/financas/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
├── config/
└── security/

Modelagem do banco

O banco possui seis tabelas principais:

USUARIO
├── CATEGORIA
├── CONTA
└── MOVIMENTACAO
      ├── RECEBIMENTO
      └── PAGAMENTO

USUARIO

Armazena os usuários do sistema.

id_usuario — PK

nome

email — UNIQUE

senha

data_cadastro

ativo

CATEGORIA

Classifica receitas e despesas.

id_categoria — PK

nome

tipo — RECEITA ou DESPESA

usuario_id — FK

Restrição: UNIQUE(nome, usuario_id)

CONTA

Representa as contas financeiras do usuário.

id_conta — PK

nome

tipo

saldo_inicial

usuario_id — FK

Restrição: UNIQUE(nome, usuario_id)

MOVIMENTACAO

Representa o lançamento financeiro reconhecido por competência.

id_movimentacao — PK

descricao

valor — maior que zero

tipo — RECEITA ou DESPESA

data

usuario_id — FK

categoria_id — FK

Uma movimentação pode possuir vários recebimentos ou pagamentos.

RECEBIMENTO

Registra a entrada efetiva de dinheiro relacionada a uma receita.

id_recebimento — PK

valor — maior que zero

data_recebimento

movimentacao_id — FK

conta_id — FK

PAGAMENTO

Registra a saída efetiva de dinheiro relacionada a uma despesa.

id_pagamento — PK

valor — maior que zero

data_pagamento

movimentacao_id — FK

conta_id — FK

Relacionamentos

USUARIO 1:N CATEGORIA
USUARIO 1:N CONTA
USUARIO 1:N MOVIMENTACAO
CATEGORIA 1:N MOVIMENTACAO
MOVIMENTACAO 1:N RECEBIMENTO
MOVIMENTACAO 1:N PAGAMENTO
CONTA 1:N RECEBIMENTO
CONTA 1:N PAGAMENTO

Regras principais

O email do usuário deve ser único.

Categorias e contas possuem nomes únicos por usuário.

O valor de movimentações, recebimentos e pagamentos deve ser maior que zero.

O tipo de uma categoria deve ser compatível com o tipo da movimentação.

Recebimentos devem estar vinculados a movimentações do tipo RECEITA.

Pagamentos devem estar vinculados a movimentações do tipo DESPESA.

A conta utilizada em um recebimento ou pagamento deve pertencer ao mesmo usuário da movimentação.

O total de recebimentos de uma receita não deve ultrapassar o valor da movimentação.

O total de pagamentos de uma despesa não deve ultrapassar o valor da movimentação.

DRE e fluxo de caixa

A DRE é gerada a partir das movimentações por competência.

RESULTADO = RECEITAS - DESPESAS

Recebimentos e pagamentos representam o fluxo efetivo de caixa.

SALDO ATUAL = SALDO INICIAL + RECEBIMENTOS - PAGAMENTOS

A DRE não é armazenada em uma tabela própria; ela será gerada por consultas e regras da aplicação.

Estrutura de pacotes

com.example.financas
├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── exception
├── config
└── security

Configuração

Crie o banco no MySQL:

CREATE DATABASE financas;

Configure o acesso no application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/financas
spring.datasource.username=root
spring.datasource.password=SUA_SENHA

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

Não versione senhas ou outras credenciais reais no GitHub.

Execução

Windows:

mvnw.cmd spring-boot:run

Linux/macOS:

./mvnw spring-boot:run

Status

Em desenvolvimento.

Licença

Projeto acadêmico desenvolvido para estudo de Banco de Dados e desenvolvimento de APIs com Spring Boot.
