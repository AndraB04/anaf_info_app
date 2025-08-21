package com.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Slf4j
public class OAuthController {

    @GetMapping("/oauth2/start")
    public RedirectView startOAuth(@RequestParam("sessionId") String sessionId, HttpServletResponse response) {
        Cookie cookie = new Cookie("oauth_session", sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(600);
        response.addCookie(cookie);
        log.info("Starting OAuth flow with session {}", sessionId);
        return new RedirectView("/oauth2/authorization/google?state=" + sessionId);
    }
}