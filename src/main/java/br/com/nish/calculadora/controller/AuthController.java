package br.com.nish.calculadora.controller;

import br.com.nish.calculadora.auth.Role;
import br.com.nish.calculadora.auth.RoleRepository;
import br.com.nish.calculadora.auth.Usuario;
import br.com.nish.calculadora.auth.UsuarioRepository;
import br.com.nish.calculadora.controller.dto.AuthResponse;
import br.com.nish.calculadora.controller.dto.LoginRequest;
import br.com.nish.calculadora.controller.dto.MeResponse;
import br.com.nish.calculadora.controller.dto.RegisterRequest;
import br.com.nish.calculadora.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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
     * @return 200 OK se criado, 400 se email já existir
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
     * @return token e expiração em segundos
     */
    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());

        /*
         * ALTERADO: Capturamos o objeto Authentication retornado pelo manager,
         * que contém os detalhes do usuário autenticado, incluindo suas roles.
         */
        Authentication authentication = authenticationManager.authenticate(authToken);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        /*
         * ALTERADO: As claims agora são extraídas dinamicamente do objeto UserDetails.
         * Removemos o valor fixo "ROLE_USER" e inserimos a lista real de permissões do usuário.
         */
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        String token = jwtService.generateToken(userDetails.getUsername(), claims);
        long expiresIn = (long) 60 * 60;
        AuthResponse response = new AuthResponse(token, expiresIn);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna informações do usuário autenticado.
     * Útil para testar o JWT no cliente.
     * @return id, email, nome e roles do usuário
     */
    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<MeResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = auth.getName();
        return usuarioRepository.findByEmail(email)
                .map(u -> {
                    MeResponse resp = new MeResponse(
                            u.getId(),
                            u.getEmail(),
                            u.getNome(),
                            u.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                    );
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(401).build());
    }
}