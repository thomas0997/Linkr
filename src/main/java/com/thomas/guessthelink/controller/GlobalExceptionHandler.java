package com.thomas.guessthelink.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleAll(Exception e, Model model) {

        // Print full stack trace to terminal
        System.out.println("=== UNHANDLED ERROR ===");
        System.out.println("Type:    " + e.getClass().getSimpleName());
        System.out.println("Message: " + e.getMessage());
        e.printStackTrace();

        // Pass details to the error page
        model.addAttribute("errorType", e.getClass().getSimpleName());
        model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "No message provided.");

        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }
}