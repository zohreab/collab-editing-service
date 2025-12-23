package com.collab.userservice.service;

import com.collab.userservice.dto.RegisterRequest;
import com.collab.userservice.model.User;
import com.collab.userservice.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User register(RegisterRequest req) {
        if (repo.existsByUsername(req.username)) {
            throw new IllegalArgumentException("username already exists");
        }
        if (repo.existsByEmail(req.email)) {
            throw new IllegalArgumentException("email already exists");
        }

        User u = new User();
        u.setUsername(req.username);
        u.setEmail(req.email);
        u.setPasswordHash(encoder.encode(req.password));
        return repo.save(u);
    }

    public User login(String username, String password) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        return u;
    }

    public com.collab.userservice.model.User getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    public com.collab.userservice.model.User updateEmail(String username, String newEmail) {
        var u = getByUsername(username);

        repo.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(u.getId())) {
                throw new IllegalArgumentException("email already exists");
            }
        });

        u.setEmail(newEmail);
        return repo.save(u);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        var u = getByUsername(username);

        if (!encoder.matches(currentPassword, u.getPasswordHash())) {
            throw new UnauthorizedException("current password is incorrect");
        }

        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
    }

    public boolean exists(String username) {
        // Use 'repo' (the instance), not 'UserRepository' (the class)
        return repo.existsByUsername(username);
    }

    public void deleteByUsername(String username) {
        User u = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        repo.delete(u);
    }



}
