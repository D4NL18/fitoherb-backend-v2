package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private AuthenticationManager authManager;

    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email);
    }

    public ResponseEntity login(LoginReq loginReq) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginReq.getEmail(), loginReq.getPassword());
        var auth = this.authManager.authenticate(usernamePassword);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity register(RegisterReq registerReq) {
        if(this.userRepository.findByEmail(registerReq.getEmail()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = this.passwordEncoder.encode(registerReq.getPassword());

        User newUser = new User(
                registerReq.getEmail(),
                encryptedPassword,
                registerReq.getName(),
                registerReq.getBirthDate(),
                registerReq.getRole()
        );

        userRepository.save(newUser);
        return ResponseEntity.ok().build();
    }
}

