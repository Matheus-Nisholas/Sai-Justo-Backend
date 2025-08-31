package br.com.nish.calculadora.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// NOVO: Import da anotação @Size
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String nome;

    @NotBlank
    // ALTERADO: Adicionamos a validação de tamanho mínimo para a senha.
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    private String senha;
}