package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TeamRepository {

    @Autowired
    private TeamNEO4JRepository teamNEO4JRepository;


    public void saveGraph(List<Team> nodes){

        for(Team t : nodes){
            teamNEO4JRepository.saveNodes(t.getName());
        }

        for(Team t: nodes){
            for(TeamRelationship r : t.getEnemyTeams()){
                teamNEO4JRepository.saveRelationships(t.getName(),r.getEnemyTeam().getName(),r.getStrength());
            }
        }

    }
}
