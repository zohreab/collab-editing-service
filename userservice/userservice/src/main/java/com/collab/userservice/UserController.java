package com.collab.userservice;

import com.collab.userservice.dto.*;
import com.collab.userservice.model.User;
import com.collab.userservice.service.UserService;
import com.collab.userservice.security.JwtUtils; // New Import
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;
    private final JwtUtils jwtUtils; // 1. Added JwtUtils field

    // 2. Updated Constructor to inject JwtUtils
    public UserController(UserService users, JwtUtils jwtUtils) {
        this.users = users;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        User u = users.register(req);
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // 3. Verify the user exists and password is correct
        User u = users.login(req.username, req.password);

        // 4. Generate the JWT token for this user
        String token = jwtUtils.generateToken(u.getUsername());

        // 5. Return the token in the response
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getEmail());
    }

    @GetMapping("/me")
    public UserResponse me(@RequestHeader("X-User") String username) {
        var u = users.getByUsername(username);
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail());
    }

    @PutMapping("/me")
    public UserResponse updateMe(
            @Valid @RequestBody UpdateProfileRequest req,
            @RequestHeader("X-User") String username
    ) {
        var u = users.updateEmail(username, req.email);
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail());
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            @RequestHeader("X-User") String username // Read the header from Gateway
    ) {
        // Now we use the username directly from the Gateway header
        users.changePassword(username, req.currentPassword, req.newPassword);
    }

    @GetMapping("/exists/{username}")
    public boolean userExists(@PathVariable String username) {
        // This calls the fixed method in your UserService
        return users.exists(username);
    }
}