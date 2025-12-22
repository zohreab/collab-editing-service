package com.collab.docservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
    @GetMapping("/docs/ping")
    public String ping() {
        return "docservice ok";
    }
}
