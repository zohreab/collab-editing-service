package com.collab.userservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserPingController {

    @GetMapping("/ping")
    public String ping() {
        return "user-service OK";
    }
}
