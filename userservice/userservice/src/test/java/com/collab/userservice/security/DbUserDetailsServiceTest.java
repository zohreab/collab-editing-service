package com.collab.userservice.security;

import com.collab.userservice.model.User;
import com.collab.userservice.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private DbUserDetailsService service;

    @Test
    void loadUserByUsername_Success() {
        User u = new User();
        u.setUsername("ali");
        u.setPasswordHash("hashed_pw");

        when(repo.findByUsername("ali")).thenReturn(Optional.of(u));

        UserDetails details = service.loadUserByUsername("ali");

        assertEquals("ali", details.getUsername());
        assertEquals("hashed_pw", details.getPassword());
    }

    @Test
    void loadUserByUsername_NotFound_ThrowsException() {
        when(repo.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                service.loadUserByUsername("nobody")
        );
    }
}