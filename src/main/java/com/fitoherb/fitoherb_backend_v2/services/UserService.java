package com.fitoherb.fitoherb_backend_v2.services;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.PasswordUpdateReq;
import com.fitoherb.fitoherb_backend_v2.dtos.requests.UserReq;
import com.fitoherb.fitoherb_backend_v2.dtos.responses.UserRes;
import com.fitoherb.fitoherb_backend_v2.entities.User;
import com.fitoherb.fitoherb_backend_v2.exceptions.DatabaseOperationException;
import com.fitoherb.fitoherb_backend_v2.exceptions.ResourceNotFoundException;
import com.fitoherb.fitoherb_backend_v2.mappers.UserMapper;
import com.fitoherb.fitoherb_backend_v2.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final  PasswordEncoder passwordEncoder;

    public UserRes getUserByEmail(String email) {
        User user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        UserRes userRes = userMapper.entityToRes(user);
        return userRes;
    }

    public Page<UserRes> getAllUsers(String search, int page, String sortField, String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, 10, Sort.by(sortDirection, sortField));

        String searchTerm = (search == null) ? "" : search;
        Page<User> userPage = userRepository.findAllFiltered(searchTerm, pageable);

        return userPage.map(userMapper::entityToRes);
    }

    @Transactional
    public void updateUserByEmail(String email, UserReq userReq) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update user in database.");
        }
    }

    @Transactional
    public void updatePasswordByEmail(String email, PasswordUpdateReq passwordUpdateReq) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        String encryptedPassword = passwordEncoder.encode(passwordUpdateReq.getPassword());
        user.setPassword(encryptedPassword);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to update password.");
        }
    }

    @Transactional
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        try {
            this.userRepository.delete(user);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to delete user. Ensure there are no records linked to this account.");
        }
    }
}
