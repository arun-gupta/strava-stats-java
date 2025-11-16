package com.example.strava.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;

    public SecurityConfig(OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient) {
        this.accessTokenResponseClient = accessTokenResponseClient;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/error", "/webjars/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(accessTokenResponseClient)
                )
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                                                        AuthenticationException exception) throws IOException, ServletException {
                        // Store the error message in session to pass to error page
                        String errorMessage = extractErrorMessage(exception);
                        request.getSession().setAttribute("errorMessage", errorMessage);
                        response.sendRedirect("/error");
                    }
                    
                    private String extractErrorMessage(AuthenticationException exception) {
                        String exMessage = exception.getMessage();
                        if (exMessage == null) {
                            return "An error occurred during authentication.";
                        }
                        
                        // Try to extract JSON error message from Strava API response
                        if (exMessage.contains("\"message\"")) {
                            try {
                                int messageStart = exMessage.indexOf("\"message\":\"") + 11;
                                int messageEnd = exMessage.indexOf("\"", messageStart);
                                if (messageEnd > messageStart) {
                                    String apiMessage = exMessage.substring(messageStart, messageEnd);
                                    if (apiMessage.contains("Rate Limit Exceeded")) {
                                        return "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                                    } else {
                                        return "Strava API Error: " + apiMessage;
                                    }
                                }
                            } catch (Exception e) {
                                // Fall through to default handling
                            }
                        }
                        
                        // Check for specific error types
                        if (exMessage.contains("429") || exMessage.contains("Rate Limit Exceeded")) {
                            return "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                        } else if (exMessage.contains("401") || exMessage.contains("Unauthorized")) {
                            return "Authentication Failed: Invalid credentials or expired token. Please try logging in again.";
                        } else if (exMessage.contains("403") || exMessage.contains("Forbidden")) {
                            return "Access Forbidden: You don't have permission to access this resource.";
                        } else if (exMessage.contains("invalid_user_info_response")) {
                            if (exMessage.contains("429")) {
                                return "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                            }
                        }
                        
                        return "Authentication Error: " + exMessage;
                    }
                })
            );
        return http.build();
    }
}
