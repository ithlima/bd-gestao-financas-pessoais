package com.example.financas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da API.
 *
 * <p><b>Estado atual: tudo liberado, de forma explícita.</b> Este é um projeto
 * acadêmico e a autenticação (login, hash de senha, JWT) ainda não faz parte do
 * escopo. O importante aqui é que a liberação seja <i>declarada</i> e não
 * acidental: qualquer endpoint novo que vier a ser criado já cai nesta regra, e
 * quem ler o arquivo sabe exatamente o que esperar.
 *
 * <p>Cada requisição recebe o {@code usuarioId} explicitamente nos parâmetros ou
 * no corpo. Quando a autenticação for implementada, a evolução natural é:
 *
 * <ol>
 *   <li>remover o {@code usuarioId} dos requests e obtê-lo do token;</li>
 *   <li>trocar {@code anyRequest().permitAll()} por
 *       {@code anyRequest().authenticated()};</li>
 *   <li>adicionar um {@code PasswordEncoder} (BCrypt) e passar a guardar o hash
 *       da senha em vez do texto puro.</li>
 * </ol>
 *
 * <p>É importante que essas três mudanças andem juntas: liberar a API hoje é uma
 * decisão consciente, não um esquecimento.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // API REST sem sessão nem formulário: CSRF não se aplica a este cenário.
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(autorizacao -> autorizacao.anyRequest().permitAll());

    return http.build();
  }
}
