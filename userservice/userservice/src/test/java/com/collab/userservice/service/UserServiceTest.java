package com.collab.userservice.service;

import com.collab.userservice.dto.RegisterRequest;
import com.collab.userservice.model.User;
import com.collab.userservice.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository repo;
    private PasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setup() {
        repo = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UserService(repo, encoder);
    }

    @Test
    void register_success_savesUserWithHashedPassword() {
        RegisterRequest req = new RegisterRequest();
        req.username = "zohreh";
        req.email = "z@x.com";
        req.password = "pass123";

        when(repo.existsByUsername("zohreh")).thenReturn(false);
        when(repo.existsByEmail("z@x.com")).thenReturn(false);
        when(encoder.encode("pass123")).thenReturn("HASHED");
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register(req);

        assertEquals("zohreh", saved.getUsername());
        assertEquals("z@x.com", saved.getEmail());
        assertEquals("HASHED", saved.getPasswordHash());
        verify(repo).save(any(User.class));
    }

    @Test
    void register_usernameExists_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.username = "taken";
        req.email = "a@b.com";
        req.password = "x";

        when(repo.existsByUsername("taken")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.register(req));
        assertEquals("username already exists", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void register_emailExists_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.username = "u";
        req.email = "taken@x.com";
        req.password = "x";

        when(repo.existsByUsername("u")).thenReturn(false);
        when(repo.existsByEmail("taken@x.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.register(req));
        assertEquals("email already exists", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void login_wrongUsername_throwsUnauthorized() {
        when(repo.findByUsername("nope")).thenReturn(Optional.empty());
        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> service.login("nope", "pw"));
        assertEquals("invalid credentials", ex.getMessage());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User u = new User();
        u.setUsername("u");
        u.setEmail("u@x.com");
        u.setPasswordHash("HASHED");

        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("wrong", "HASHED")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> service.login("u", "wrong"));
        assertEquals("invalid credentials", ex.getMessage());
    }

    @Test
    void login_success_returnsUser() {
        User u = new User();
        u.setUsername("u");
        u.setEmail("u@x.com");
        u.setPasswordHash("HASHED");

        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("pw", "HASHED")).thenReturn(true);

        User out = service.login("u", "pw");
        assertEquals("u", out.getUsername());
    }

    @Test
    void changePassword_wrongCurrent_throwsUnauthorized() {
        User u = new User();
        u.setUsername("u");
        u.setEmail("u@x.com");
        u.setPasswordHash("OLD");

        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("bad", "OLD")).thenReturn(false);

        UnauthorizedException ex = assertThrows(
                UnauthorizedException.class,
                () -> service.changePassword("u", "bad", "new")
        );

        assertEquals("current password is incorrect", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void changePassword_success_updatesHash() {
        User u = new User();
        u.setUsername("u");
        u.setEmail("u@x.com");
        u.setPasswordHash("OLD");

        when(repo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches("old", "OLD")).thenReturn(true);
        when(encoder.encode("new")).thenReturn("NEW_HASH");

        service.changePassword("u", "old", "new");

        assertEquals("NEW_HASH", u.getPasswordHash());
        verify(repo).save(u);
    }

    @Test
    void exists_delegatesToRepo() {
        when(repo.existsByUsername("ali")).thenReturn(true);
        assertTrue(service.exists("ali"));
        verify(repo).existsByUsername("ali");
    }

    // Add these to your existing UserServiceTest class:

    @Test
    void getByUsername_found_returnsUser() {
        User u = new User();
        u.setUsername("ali");
        when(repo.findByUsername("ali")).thenReturn(Optional.of(u));

        User result = service.getByUsername("ali");
        assertEquals("ali", result.getUsername());
    }

    @Test
    void getByUsername_notFound_throwsException() {
        when(repo.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getByUsername("missing"));
    }

    @Test
    void updateEmail_success_savesNewUser() {
        User u = new User();
        u.setUsername("ali");
        u.setEmail("old@x.com");

        when(repo.findByUsername("ali")).thenReturn(Optional.of(u));
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = service.updateEmail("ali", "new@x.com");

        assertEquals("new@x.com", updated.getEmail());
        verify(repo).save(u);
    }
}
