package com.marius.ptr.vitale.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String getGreeting() {
        return "Welcome to the book catalog";
    }

    @GetMapping("/v2")
    public String getGreetingV2() {
        return "Welcome to the book catalog v2 docker";
    }
}
