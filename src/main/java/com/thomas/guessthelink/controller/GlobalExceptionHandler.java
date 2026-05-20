package com.thomas.guessthelink.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Catches any unhandled exception and renders the error template
    @ExceptionHandler(Exception.class)
    public String handleAll(Exception e, Model model) {
        model.addAttribute("errorType", e.getClass().getSimpleName());
        model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Something went wrong.");
        return "error";
    }

    // 404s also render the error template instead of a blank response,
    // which prevents IDOR-style probing from getting silent empty pages
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNotFound(NoResourceFoundException e, Model model) {
        model.addAttribute("errorType", "Page Not Found");
        model.addAttribute("errorMessage", "The page you're looking for doesn't exist.");
        return "error";
    }
}