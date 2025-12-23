package com.collab.userservice;

import com.collab.userservice.dto.*;
import com.collab.userservice.model.User;
import com.collab.userservice.service.UserService;
import com.collab.userservice.security.JwtUtils; // New Import
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;
    private final JwtUtils jwtUtils; // 1. Added JwtUtils field

    private final RestTemplate restTemplate;

    @Value("${services.docservice.baseUrl:http://localhost:8082}")
    private String docserviceBaseUrl;

    @Value("${internal.secret}")
    private String internalSecret;

    // 2. Updated Constructor to inject JwtUtils
    public UserController(UserService users, JwtUtils jwtUtils, RestTemplate restTemplate) {
        this.users = users;
        this.jwtUtils = jwtUtils;
        this.restTemplate = restTemplate;
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

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@RequestHeader("X-User") String username) {

        // 1) First delete docs owned by this user (call DocService internal endpoint)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    docserviceBaseUrl + "/docs/internal/owner/" + username,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );
        } catch (Exception e) {
            // Decide policy: strict or best-effort.
            // I recommend STRICT for consistency:
            throw new IllegalArgumentException("Failed to delete user's documents. User was not deleted.");
        }

        // 2) Then delete user
        users.deleteByUsername(username);
    }

}