package com.example.strava.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("firstname") + " " + principal.getAttribute("lastname"));
            model.addAttribute("username", principal.getAttribute("username"));
        }
        return "dashboard";
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        String errorMessage = "An error occurred during authentication.";
        
        if (exception != null && exception instanceof Exception) {
            Exception ex = (Exception) exception;
            String exMessage = ex.getMessage();
            
            // Extract meaningful error message from exception
            if (exMessage != null) {
                // Try to extract JSON error message from Strava API response
                if (exMessage.contains("\"message\"")) {
                    try {
                        // Extract message from JSON: "message":"Rate Limit Exceeded"
                        int messageStart = exMessage.indexOf("\"message\":\"") + 11;
                        int messageEnd = exMessage.indexOf("\"", messageStart);
                        if (messageEnd > messageStart) {
                            String apiMessage = exMessage.substring(messageStart, messageEnd);
                            if (apiMessage.contains("Rate Limit Exceeded")) {
                                errorMessage = "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                            } else {
                                errorMessage = "Strava API Error: " + apiMessage;
                            }
                        }
                    } catch (Exception e) {
                        // Fall through to default handling
                    }
                }
                
                // Check for rate limit errors
                if (exMessage.contains("429") || exMessage.contains("Rate Limit Exceeded")) {
                    errorMessage = "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                } else if (exMessage.contains("401") || exMessage.contains("Unauthorized")) {
                    errorMessage = "Authentication Failed: Invalid credentials or expired token. Please try logging in again.";
                } else if (exMessage.contains("403") || exMessage.contains("Forbidden")) {
                    errorMessage = "Access Forbidden: You don't have permission to access this resource.";
                } else if (exMessage.contains("invalid_user_info_response")) {
                    // Extract the actual error from the message
                    if (exMessage.contains("429")) {
                        errorMessage = "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                    } else if (!errorMessage.equals("An error occurred during authentication.")) {
                        // Keep the extracted message if we already set it
                    } else {
                        errorMessage = "Authentication Error: " + exMessage;
                    }
                } else if (errorMessage.equals("An error occurred during authentication.")) {
                    errorMessage = "Error: " + exMessage;
                }
            }
        } else if (message != null) {
            errorMessage = message.toString();
        }
        
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("status", status != null ? status.toString() : "Unknown");
        
        return "error";
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public String handleOAuth2Exception(OAuth2AuthenticationException ex, Model model) {
        String errorMessage = "An error occurred during authentication.";
        String exMessage = ex.getMessage();
        
        if (exMessage != null) {
            // Try to extract JSON error message from Strava API response
            if (exMessage.contains("\"message\"")) {
                try {
                    int messageStart = exMessage.indexOf("\"message\":\"") + 11;
                    int messageEnd = exMessage.indexOf("\"", messageStart);
                    if (messageEnd > messageStart) {
                        String apiMessage = exMessage.substring(messageStart, messageEnd);
                        if (apiMessage.contains("Rate Limit Exceeded")) {
                            errorMessage = "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
                        } else {
                            errorMessage = "Strava API Error: " + apiMessage;
                        }
                    }
                } catch (Exception e) {
                    // Fall through to default handling
                }
            }
            
            // Check for specific error types
            if (exMessage.contains("429") || exMessage.contains("Rate Limit Exceeded")) {
                errorMessage = "Strava API Rate Limit Exceeded: You've made too many requests. Please wait a few minutes and try again.";
            } else if (exMessage.contains("401") || exMessage.contains("Unauthorized")) {
                errorMessage = "Authentication Failed: Invalid credentials or expired token. Please try logging in again.";
            } else if (exMessage.contains("403") || exMessage.contains("Forbidden")) {
                errorMessage = "Access Forbidden: You don't have permission to access this resource.";
            } else if (errorMessage.equals("An error occurred during authentication.")) {
                errorMessage = "Authentication Error: " + exMessage;
            }
        }
        
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}
