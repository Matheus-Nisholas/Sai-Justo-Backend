package br.com.nish.calculadora.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload para login.
 */
@Data
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String senha;
}
