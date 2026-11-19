package com.fitoherb.fitoherb_backend_v2.infra.security;

import com.fitoherb.fitoherb_backend_v2.services.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica requisicoes com o header {@code X-API-Key}, concedendo ROLE_ADMIN.
 * Nao e um bean: e instanciado apenas na SecurityFilterChain para nao ser registrado
 * tambem como filtro de servlet fora da cadeia do Spring Security.
 */
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    private static final String KEY_MANAGEMENT_PATH = "/api-keys";

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);

        // A gestao de chaves exige login JWT de administrador: uma API key nao pode criar outras.
        if (rawKey == null || request.getRequestURI().startsWith(KEY_MANAGEMENT_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        var apiKey = apiKeyService.authenticate(rawKey);
        if (apiKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{ \"status\": \"401 UNAUTHORIZED\", \"message\": \"API key invalida ou revogada\" }");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "api-key:" + apiKey.get().getName(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
