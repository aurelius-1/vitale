package com.marius.ptr.vitale.controller;

import com.marius.ptr.vitale.config.PolarProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final PolarProperties polarProperties;

    public HomeController(PolarProperties polarProperties) {
        this.polarProperties = polarProperties;
    }

    @GetMapping("/")
    public String getGreeting() {
        return polarProperties.getGreeting();
    }

    @GetMapping("/v2")
    public String getGreetingV2() {
        return "Welcome to the book catalog v2 docker";
    }
}
