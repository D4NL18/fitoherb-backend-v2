package com.fitoherb.fitoherb_backend_v2.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class SyncSchedulerAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_SCHEDULER_TOKEN = "X-Sync-Scheduler-Token";
    private static final String DAILY_SYNC_PATH = "/erp-products/sync/daily";

    private final String expectedToken;

    @Autowired
    public SyncSchedulerAuthFilter(@Value("${app.erp.sync.scheduler-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedToken = request.getHeader(HEADER_SCHEDULER_TOKEN);
        String requestUri = request.getRequestURI();

        if (providedToken != null && (requestUri == null || requestUri.endsWith(DAILY_SYNC_PATH))) {
            if (expectedToken == null || expectedToken.isBlank()) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"SERVICE_UNAVAILABLE\",\"message\":\"Token de agendamento não configurado no servidor.\",\"code\":\"SYNC_SCHEDULER_UNCONFIGURED\"}"
                );
                return;
            }

            byte[] expectedBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
            byte[] providedBytes = providedToken.getBytes(StandardCharsets.UTF_8);

            if (!MessageDigest.isEqual(expectedBytes, providedBytes)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":\"UNAUTHORIZED\",\"message\":\"Credencial de agendamento inválida ou ausente.\",\"code\":\"SYNC_SCHEDULER_UNAUTHORIZED\"}"
                );
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    "CLOUD_SCHEDULER",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SYNC_SCHEDULER"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
