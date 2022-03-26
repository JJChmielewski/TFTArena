package com.jjchmielewski.tftarena.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RiotController {

    @GetMapping("/riot")
    public String riotTXT(){
        return "riot.txt";
    }

}
