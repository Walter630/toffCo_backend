package com.site.toffCo.infra.security;

import com.site.toffCo.module.user.repository.UserRepository;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = tokenService.validateToken(token);

            /*
             * Token presente, mas inválido ou expirado.
             *
             * Não deixamos a requisição continuar como anônima,
             * pois isso acaba sendo convertido em 403.
             */
            if (email == null || email.isBlank()) {
                responderNaoAutorizado(
                        response,
                        "Token inválido ou expirado."
                );
                return;
            }

            var userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                responderNaoAutorizado(
                        response,
                        "Usuário do token não foi encontrado."
                );
                return;
            }

            var user = userOptional.get();

            var authorities = List.of(
                    new SimpleGrantedAuthority(
                            "ROLE_" + user.getRole().name()
                    )
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception exception) {
            SecurityContextHolder.clearContext();

            responderNaoAutorizado(
                    response,
                    "Token inválido ou expirado."
            );
        }
    }

    private String recuperarToken(HttpServletRequest request) {
        String authorization =
                request.getHeader("Authorization");

        if (authorization == null ||
                !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();

        return token.isBlank() ? null : token;
    }

    private void responderNaoAutorizado(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        String escapedMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        response.getWriter().write("""
                {
                  "status": 401,
                  "error": "UNAUTHORIZED",
                  "message": "%s"
                }
                """.formatted(escapedMessage));
    }

    /**
     * Login, cadastro e refresh não devem depender
     * de um access token válido.
     */
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getServletPath();

        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/refresh_token")
                // Webhooks da Evolution API e Odoo
                || path.startsWith("/api/webhook/")
                || path.startsWith("/api/webhooks/")
                || path.startsWith("/instance/")
                || path.startsWith("/webhook/whatsapp/")
                || path.equals("/bot-test.html");
    }
}