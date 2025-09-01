package br.com.nish.calculadora.controller.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Informações básicas do usuário autenticado.
 */
@Data
@AllArgsConstructor
public class MeResponse {
    private Long id;
    private String email;
    private String nome;
    private Set<String> roles;
    // NOVO: Adicionamos o username à resposta do /me.
    private String username;
}
