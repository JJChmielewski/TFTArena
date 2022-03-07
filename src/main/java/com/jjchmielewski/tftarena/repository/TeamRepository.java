package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.Item;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.UnitNode;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamUnitRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.UnitItemRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamRepository {

    @Autowired
    private TeamNEO4JRepository teamNEO4JRepository;

    @Autowired
    private UnitNEO4JRepository unitNEO4JRepository;

    @Autowired
    private ItemNeo4JRepository itemNeo4JRepository;

    @Value("${tftarena.prefix.traitPrefix}")
    private String traitPrefix;

    @Value("${tftarena.prefix.unitPrefix}")
    private String unitPrefix;


    @Transactional
    public void saveGraph(List<Team> teams, UnitNode[] units, Item[] items){

        for(Team t: teams){
            teamNEO4JRepository.saveNode(t.getName());
        }
        for(Item i : items){
            itemNeo4JRepository.saveItem(i.getItemId());
        }

        for(UnitNode unit: units){
            if(unit!=null) {
                unitNEO4JRepository.saveUnit(unit.getName());

                for(UnitItemRelationship r : unit.getItems()){
                    unitNEO4JRepository.saveUnitItemRelationship(unit.getName(),r.getItem().getItemId(),r.getTimesPlayed());
                }
            }
        }

        for(Team t: teams){
            for(TeamRelationship r : t.getEnemyTeams()){
                teamNEO4JRepository.saveTeamRelationships(t.getName(),r.getEnemyTeam().getName(),r.getStrength());
            }
            for(TeamUnitRelationship r : t.getUnits()){
                teamNEO4JRepository.saveTeamUnitRelationship(t.getName(),r.getUnit().getName(),r.getWeight(),r.getPercentagePlayed());
            }
        }

    }

    public void deleteAll(){
        teamNEO4JRepository.deleteAll();
        unitNEO4JRepository.deleteAll();
        itemNeo4JRepository.deleteAll();
    }

    public List<Team> getMatchInfo(String[] teams){

        /*
        for(int i=0;i<teams.length;i++){
            String[] temp = teams[i].split("_");
            if(temp.length == 4)
                teams[i] = this.traitPrefix+temp[0]+"_"+temp[1]+this.unitPrefix+temp[2]+"_"+temp[3];
        }*/

        return teamNEO4JRepository.findStrength(teams);
    }
}
