package com.collab.versionservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/versions")
public class VersionPingController {

    @GetMapping("/ping")
    public String ping() {
        return "version-service OK";
    }
}
