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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/register")
    @Transactional
    @Operation(summary = "Registrar usuário")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        // Valida se o email ou o username já existem
        if (usuarioRepository.existsByEmail(request.getEmail()) || usuarioRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setNome(request.getNome());
        usuario.setUsername(request.getUsername()); // Salva o novo campo
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

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // 1. Busca o usuário pelo campo 'login' (que pode ser email ou username)
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameOrEmail(request.getLogin(), request.getLogin());
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
        }
        Usuario usuario = usuarioOpt.get();

        // 2. Usa o EMAIL do usuário encontrado para a autenticação do Spring Security
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), request.getSenha());

        Authentication authentication = authenticationManager.authenticate(authToken);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Gera o token com as informações corretas
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("username", usuario.getUsername());

        String token = jwtService.generateToken(userDetails.getUsername(), claims); // O 'subject' do token continua sendo o email
        long expiresIn = 3600; // 1 hora
        AuthResponse response = new AuthResponse(token, expiresIn);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<MeResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = auth.getName();
        return usuarioRepository.findByEmail(email)
                .map(u -> {
                    // Inclui o username na resposta
                    MeResponse resp = new MeResponse(
                            u.getId(),
                            u.getEmail(),
                            u.getNome(),
                            u.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                            u.getUsername()
                    );
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}