package br.com.nish.calculadora.config;

import br.com.nish.calculadora.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança HTTP baseada em JWT.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Injeta o filtro de autenticação JWT.
     * @param jwtAuthFilter filtro que extrai e valida o token
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * Cadeia de filtros de segurança.
     * Desabilita CSRF para APIs stateless, libera rotas públicas e exige autenticação nas demais.
     * @param http objeto de configuração HTTP
     * @return SecurityFilterChain
     * @throws Exception erro de configuração
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/auth/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/actuator/health",
                        "/actuator/info"
                ).permitAll()
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Encoder de senhas usando BCrypt.
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager padrão do Spring.
     * @param cfg configuração de autenticação
     * @return AuthenticationManager
     * @throws Exception erro ao obter o manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
