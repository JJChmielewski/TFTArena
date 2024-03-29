package com.jjchmielewski.tftarena.controllers;


import com.jjchmielewski.tftarena.responses.ResponseItem;
import com.jjchmielewski.tftarena.responses.ResponseTeam;
import com.jjchmielewski.tftarena.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public void checkAlgorithm(){

        //this.mainService.checkAlgorithm();

        this.mainService.checkAlgorithmWithMetaTftData();
    }

    @GetMapping("/team/get-teams")
    public ResponseTeam[] getBestTeams(@RequestBody String[] teams, @RequestParam(defaultValue = "0.025") double minPercentagePlayed, @RequestParam(defaultValue = "5") int limit){

        return mainService.findBestTeams(teams,minPercentagePlayed, limit);
    }

    @GetMapping("/team/get-units")
    public ResponseTeam getTeamUnits(@RequestParam String teamName, @RequestParam int level, @RequestParam(defaultValue = "0.025") double minPercentagePlayed){

        return mainService.buildTeam(teamName,level,minPercentagePlayed);
    }

    @GetMapping("/team/get-items")
    public List<ResponseItem> getUnitItems(@RequestParam String unitName, @RequestParam(defaultValue = "5") int limit){

        return mainService.findBestItemsForUnit(unitName,limit);
    }
}
