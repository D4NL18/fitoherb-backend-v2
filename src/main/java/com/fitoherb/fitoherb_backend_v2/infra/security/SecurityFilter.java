package com.fitoherb.fitoherb_backend_v2.infra.security;

import com.fitoherb.fitoherb_backend_v2.exceptions.InvalidTokenException;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import com.fitoherb.fitoherb_backend_v2.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            var token = this.recoverToken(request);
            if (token != null && !request.getRequestURI().contains("/auth/refresh")) {
                System.out.println("Recovered token from request: " + request.getRequestURI());
                var email = tokenService.validateToken(token);
                System.out.println("Token validated, email: " + email);
                userRepository.findByEmail(email).ifPresent(user -> {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } else if (token == null && !request.getRequestURI().contains("/auth/")) {
                System.out.println("No token recovered for request: " + request.getRequestURI());
            }
            filterChain.doFilter(request, response);
        } catch (InvalidTokenException e) {
            System.out.println("Invalid token exception: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{ \"status\": \"401 UNAUTHORIZED\", \"message\": \"" + e.getMessage() + "\" }");
        }
    }

    private String recoverToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("fitoherb_jwt".equals(cookie.getName())) {
                    String val = cookie.getValue();
                    if (val != null && val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                        return val.substring(1, val.length() - 1);
                    }
                    return val;
                }
            }
        }
        
        var authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.replace("Bearer ", "");
        }

        return null;
    }
}