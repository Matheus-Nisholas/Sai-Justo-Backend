package br.com.nish.calculadora.controller;

import br.com.nish.calculadora.auth.Role;
import br.com.nish.calculadora.auth.RoleRepository;
import br.com.nish.calculadora.auth.Usuario;
import br.com.nish.calculadora.auth.UsuarioRepository;
import br.com.nish.calculadora.controller.dto.AuthResponse;
import br.com.nish.calculadora.controller.dto.LoginRequest;
import br.com.nish.calculadora.controller.dto.RegisterRequest;
import br.com.nish.calculadora.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticação.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro e login com JWT")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Registra um novo usuário com ROLE_USER.
     * @param request dados de registro
     * @return 200 OK se criado
     */
    @PostMapping("/register")
    @Transactional
    @Operation(summary = "Registrar usuário")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setNome(request.getNome());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));

        Role roleUser = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roleRepository.save(r);
        });
        usuario.setRoles(Set.of(roleUser));
        usuarioRepository.save(usuario);
        return ResponseEntity.ok().build();
    }

    /**
     * Autentica e retorna o token JWT.
     * @param request credenciais
     * @return token e expiração
     */
    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());
        authenticationManager.authenticate(authToken);

        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("roles", "ROLE_USER");
        String token = jwtService.generateToken(request.getEmail(), claims);

        long expiresIn = (long) 60 * 60; // espelha jwt.expiration-minutes=60
        AuthResponse response = new AuthResponse(token, expiresIn);
        return ResponseEntity.ok(response);
    }
}
