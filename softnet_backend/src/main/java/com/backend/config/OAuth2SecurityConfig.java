package com.backend.config;

import com.backend.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class OAuth2SecurityConfig {

    private final EmailVerificationService verificationService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/**", "/oauth2/**", "/login/**", "/static/**", "/*.html").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                String email = oAuth2User.getAttribute("email");

                String sessionIdStr = null;
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if ("oauth_session".equals(c.getName())) {
                            sessionIdStr = c.getValue();
                            c.setMaxAge(0);
                            c.setPath("/");
                            response.addCookie(c);
                            break;
                        }
                    }
                }

                if (sessionIdStr == null) {
                    String state = request.getParameter("state");
                    if (state != null) {
                        try {
                            UUID.fromString(state);
                            sessionIdStr = state;
                        } catch (Exception ex) {
                            log.warn("Received non-UUID OAuth2 state, and no oauth_session cookie present");
                        }
                    }
                }

                boolean verificationSuccessful = false;
                UUID sessionUuid = null;
                if (sessionIdStr != null) {
                    try {
                        sessionUuid = UUID.fromString(sessionIdStr);
                        verificationSuccessful = verificationService.verifyGoogleAuth(sessionUuid, email);
                        log.info("Email verification result for session {}: {}", sessionUuid, verificationSuccessful);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid session UUID received: {}", sessionIdStr);
                    } catch (Exception e) {
                        log.error("Error during email verification: ", e);
                    }
                } else {
                    log.warn("No verification session ID could be determined during OAuth2 success");
                }

                if (verificationSuccessful && sessionUuid != null) {
                    response.sendRedirect("/email-verification-success.html?email=" + email + "&session=" + sessionUuid);
                } else {
                    response.sendRedirect("/email-verification-error.html?error=verification_failed");
                }
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException exception)
                    throws IOException {
                log.error("OAuth2 authentication failed: ", exception);
                response.sendRedirect("/email-verification-error.html?error=auth_failed");
            }
        };
    }
}