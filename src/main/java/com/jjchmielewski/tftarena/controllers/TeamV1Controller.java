package com.jjchmielewski.tftarena.controllers;


import com.jjchmielewski.tftarena.matrixBuilder.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;


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

    @GetMapping("/teams/get-best")
    public Pair<String, Double>[] getBestTeams(@RequestBody String[] teams, @RequestParam(defaultValue = "0.025") double minPercentagePlayed, @RequestParam(defaultValue = "5") int limit){
        return mainService.findBestTeams(teams,minPercentagePlayed, limit);
    }
}
