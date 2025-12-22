package com.collab.userservice.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    @Test
    void generateToken_thenValidate_returnsSameUsername() {
        JwtUtils jwt = new JwtUtils();
        String token = jwt.generateToken("zohreh");
        assertEquals("zohreh", jwt.validateAndGetUsername(token));
    }
}
