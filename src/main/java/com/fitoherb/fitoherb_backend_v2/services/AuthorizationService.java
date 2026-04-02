package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.LoginReq;
import com.fitoherb.fitoherb_backend_v2.DTOs.Requests.RegisterReq;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.mappers.AuthMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import com.fitoherb.fitoherb_backend_v2.exceptions.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    @Autowired
    private AuthMapper authMapper;

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
        var userExistis = this.userRepository.findByEmail(registerReq.getEmail());

        if (userExistis != null) {
            throw new UserAlreadyExistsException();
        }

        String encryptedPassword = this.passwordEncoder.encode(registerReq.getPassword());

        User newUser = authMapper.registerReqToEntity(registerReq);

        newUser.setPassword(encryptedPassword);

        userRepository.save(newUser);
        return ResponseEntity.ok().build();
    }
}

