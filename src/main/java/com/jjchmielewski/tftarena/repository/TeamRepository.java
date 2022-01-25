package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TeamRepository {

    @Autowired
    private TeamNEO4JRepository teamNEO4JRepository;

    @Value("${tftarena.prefix.traitPrefix}")
    private String traitPrefix;

    @Value("${tftarena.prefix.unitPrefix}")
    private String unitPrefix;


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

    public void deleteAll(){
        teamNEO4JRepository.deleteAll();
    }

    public List<Team> getMatchInfo(String[] teams){

        for(int i=0;i<teams.length;i++){
            String[] temp = teams[i].split("_");
            if(temp.length == 4)
                teams[i] = this.traitPrefix+temp[0]+"_"+temp[1]+this.unitPrefix+temp[2]+"_"+temp[3];
        }

        return teamNEO4JRepository.findStrength(teams);
    }
}
