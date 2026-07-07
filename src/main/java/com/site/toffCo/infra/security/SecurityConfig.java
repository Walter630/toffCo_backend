package com.site.toffCo.infra.security;

import com.site.toffCo.module.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final UserRepository userRepository;

    public SecurityConfig(SecurityFilter securityFilter, UserRepository userRepository) {
        this.securityFilter = securityFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/produtos",
                                "/api/produtos/*"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/pagamentoitems/FormaPayment"
                        ).permitAll()

                        .requestMatchers(
                                "/bot-test.html",
                                "/webhook/whatsapp/receive",
                                "/webhook/whatsapp/simulate",
                                "/webhook/set/ToffCoBot",
                                "/instance/create"
                        ).permitAll()
                        .anyRequest().authenticated() // Bloqueia o resto
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            var userEntity = userRepository.findByEmail(email) // Se o seu método no repository tiver outro nome (ex: findByUsername), troque aqui!
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

            return User.builder()
                    .username(userEntity.getEmail())
                    .password(userEntity.getPassword())
                    .roles(userEntity.getRole().name())
                    .build();
        };
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 3. CONFIGURAÇÃO DO CORS: Define quem pode acessar a sua API
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Em produção, você trocaria o "*" pelas URLs oficiais do seu frontend
        configuration.setAllowedOrigins(List.of(
                "https://toffbr.com.br",
                "https://api.toffbr.com.br",
                "https://www.toffbr.com.br",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Aplica essa regra em todas as rotas
        return source;
    }
}
