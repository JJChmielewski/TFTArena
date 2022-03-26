package com.jjchmielewski.tftarena.controllers;


import com.jjchmielewski.tftarena.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1")
public class TeamV1Controller {

    private final MainService mainService;

    @Autowired
    public TeamV1Controller(MainService mainService) {
        this.mainService = mainService;
    }

    @GetMapping("/check")
    public double checkAlgorithm(){
        return this.mainService.checkAlgorithm();
    }

    @GetMapping("/predict")
    public Pair<String, Double>[] getMatchPrediction(@RequestBody String[] teams){

        return mainService.predictMatch(teams);

    }

    @GetMapping("/team/get-best")
    public Pair<String, Double>[] getBestTeams(@RequestBody String[] teams, @RequestParam(defaultValue = "0.025") double minPercentagePlayed, @RequestParam(defaultValue = "5") int limit){
        return mainService.findBestTeams(teams,minPercentagePlayed, limit);
    }

    @GetMapping("/team/get-units")
    public List<Pair<String,Double>> getTeamUnits(@RequestParam String teamName, @RequestParam int level, @RequestParam(defaultValue = "0.025") double minPercentagePlayed){

        return mainService.buildTeam(teamName,level,minPercentagePlayed);

    }
}
