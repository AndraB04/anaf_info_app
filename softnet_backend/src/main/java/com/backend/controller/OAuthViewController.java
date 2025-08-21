package com.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class OAuthViewController {

    @GetMapping(value = "/email-verification-success.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> emailVerificationSuccess(@RequestParam(required = false) String email,
                                                          @RequestParam(required = false) String session) {
        try {
            ClassPathResource resource = new ClassPathResource("static/email-verification-success.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("<h1>Error loading page</h1>");
        }
    }

    @GetMapping(value = "/email-verification-error.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> emailVerificationError(@RequestParam(required = false) String error) {
        try {
            ClassPathResource resource = new ClassPathResource("static/email-verification-error.html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("<h1>Error loading page: " + e.getMessage() + "</h1>");
        }
    }
}
