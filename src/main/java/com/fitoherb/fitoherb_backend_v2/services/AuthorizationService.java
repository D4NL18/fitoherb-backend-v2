package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.MailReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceAlreadyExistsException;
import com.fitoherb.fitoherb_backend_v2.mappers.AuthMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final TokenService tokenService;
    private final MailService mailService;

    private final ObjectProvider<AuthenticationManager> authManagerProvider;
    private final ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com e-mail: " + email));
    }

    public String login(LoginReq loginReq) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginReq.getEmail(), loginReq.getPassword());

        var auth = this.authManagerProvider.getObject().authenticate(usernamePassword);

        return tokenService.generateToken((User) auth.getPrincipal());
    }

    @Transactional
    public User register(RegisterReq registerReq) {
        if (this.userRepository.findByEmail(registerReq.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("E-mail já está em uso");
        }

        String rawPassword = generateSecurePassword();

        String encryptedPassword = this.passwordEncoderProvider.getObject().encode(rawPassword);

        User newUser = authMapper.registerReqToEntity(registerReq);
        newUser.setPassword(encryptedPassword);

        try {
            User savedUser = userRepository.save(newUser);
            sendWelcomeEmail(savedUser.getEmail(), savedUser.getName(), rawPassword);
            log.info("User registered successfully: {}", savedUser.getEmail());

            return savedUser;
        } catch (Exception e) {
            log.error("Failed to register user {}: ", registerReq.getEmail(), e);
            throw new DatabaseOperationException("Falha ao registrar usuário. O sistema não conseguiu salvar a conta.", e);
        }
    }

    private String generateSecurePassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "@$!%*?&#";
        String allChars = upperCase + lowerCase + digits + specials;

        StringBuilder password = new StringBuilder();

        password.append(upperCase.charAt(SECURE_RANDOM.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(SECURE_RANDOM.nextInt(lowerCase.length())));
        password.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        password.append(specials.charAt(SECURE_RANDOM.nextInt(specials.length())));

        for (int i = 4; i < 10; i++) {
            password.append(allChars.charAt(SECURE_RANDOM.nextInt(allChars.length())));
        }

        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    private void sendWelcomeEmail(String email, String name, String password) {
        MailReq mailReq = new MailReq();
        mailReq.setEmail(email);
        mailReq.setSubject("Bem-vindo ao Fitoherb - Suas Credenciais de Acesso");

        String body = """
        Olá %s,
        
        Sua conta foi criada com sucesso em nossa plataforma!
        
        Sua senha de acesso temporária é: %s
        
        Recomendamos que você altere sua senha assim que realizar o seu primeiro acesso para garantir a segurança da sua conta.
        
        Atenciosamente,
        Equipe Fitoherb
        """.formatted(name, password);

        mailReq.setMessage(body);
        mailService.sendEmail(mailReq);
    }
}