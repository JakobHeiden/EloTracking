package com.elorankingbot.backend.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ForwardToFrontend {

    @RequestMapping(value = "/{path:[\\d]+}")
    public String forwardToRouteUrl() {
        System.out.println("forward");
        return "forward:/";
    }
}